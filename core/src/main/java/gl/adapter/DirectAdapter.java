package gl.adapter;

import gl.*;
import gl.body.*;
import gl.model.*;
import gl.provider.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * GL v2 -- Direct implementation of the 13 governed lifecycle commands.
 *
 * <p>Architecture:
 * <ul>
 *   <li>Provider bindings are per-agent ({@code bind()} creates isolated bindings)</li>
 *   <li>Lifecycle stores are shared (results, candidates, assessments, decisions, traces
 *       are globally resolvable by ID)</li>
 * </ul>
 */
public final class DirectAdapter implements ResourceActions {

    // -- Shared lifecycle stores (global) --
    private final KernelPorts.BlobStore blobs;
    private final KernelPorts.CandidateStore candidates;
    private final KernelPorts.AssessmentStore assessments;
    private final KernelPorts.ResultStore results;
    private final KernelPorts.TraceSink traces;
    private final KernelPorts.MetricsSink metrics;
    private final KernelPorts.DecisionStore decisions;

    // -- Per-agent provider bindings --
    private final ConcurrentHashMap<String, ProviderBinding> bindings = new ConcurrentHashMap<>();

    // -- Result-to-binding provenance (exact binding used per call) --
    private final ConcurrentHashMap<String, String> resultToBinding = new ConcurrentHashMap<>();

    // -- Trace-to-result reverse index (O(1) lookup for explainTrace) --
    private final ConcurrentHashMap<String, String> traceToResult = new ConcurrentHashMap<>();

    // -- Candidate-to-binding reverse index (O(1) lookup for findKernelForCandidate) --
    private final ConcurrentHashMap<String, String> candidateToBinding = new ConcurrentHashMap<>();

    // -- Pluggable governance components --
    private final KernelPorts.ResponseValidator validator;
    private final KernelPorts.GovernancePolicy governance;
    private final KernelPorts.AdmissibilityChecker admissibilityChecker;

    public DirectAdapter() {
        this.blobs = new InMemoryKernelStores.Blobs();
        this.candidates = new InMemoryKernelStores.Candidates();
        this.assessments = new InMemoryKernelStores.Assessments();
        this.results = new InMemoryKernelStores.Results();
        this.traces = new InMemoryKernelStores.Traces();
        this.metrics = new InMemoryKernelStores.Metrics();
        this.decisions = new InMemoryKernelStores.Decisions();
        this.validator = new KernelDefaults.KeyValueResponseValidator();
        this.governance = new KernelDefaults.SimpleGovernancePolicy();
        this.admissibilityChecker = new KernelDefaults.SimpleAdmissibilityChecker();
    }

    // -- 1. see() --

    @Override
    public String see() {
        Set<String> available = ProviderRegistry.available();
        if (available.isEmpty()) return "EMPTY";
        StringBuilder sb = new StringBuilder();
        for (String name : available) {
            if (sb.length() > 0) sb.append(';');
            sb.append("name=").append(name);
            // Check usability by trying to detect API key
            String envVar = name.toUpperCase() + "_API_KEY";
            String key = System.getenv(envVar);
            if ("fake".equals(name) || (key != null && !key.isBlank())) {
                sb.append(",status=usable");
            } else {
                sb.append(",status=missing_key,reason=").append(envVar).append(" not set");
            }
        }
        return sb.toString();
    }

    // -- 2. bind() --

    @Override
    public String bind(String agentId, String provider, String model, String config) {
        if (agentId == null || agentId.isBlank()) return "ERROR:missing_agent_id";
        if (provider == null || provider.isBlank()) return "ERROR:missing_provider";

        // Parse config string: "temperature=0.5,maxTokens=1000" or ""
        ProviderConfig.Builder cb = new ProviderConfig.Builder();
        cb.set("provider", provider);
        if (model != null && !model.isBlank()) cb.set("model", model);
        if (config != null && !config.isBlank()) {
            for (String pair : config.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) cb.set(kv[0].trim(), kv[1].trim());
            }
        }
        ProviderConfig providerConfig = cb.build();

        // Resolve provider
        KernelPorts.GenerativeProvider resolved;
        try {
            resolved = ProviderRegistry.resolve(providerConfig);
        } catch (Exception e) {
            return "ERROR:unknown_provider:" + provider;
        }

        // Create kernel with shared stores
        GovernanceKernel kernel = new GovernanceKernel(
                resolved, validator, governance, admissibilityChecker,
                blobs, candidates, assessments, results, traces, metrics,
                decisions, null, List.of());

        // Register all standard bodies for this kernel
        GenerativeBodyRegistry bodies = GenerativeBodyRuntime.createStandardRegistry(kernel);

        ProviderBinding binding = new ProviderBinding(
                Ids.id("bind"), agentId, provider,
                model == null ? "" : model, kernel, bodies,
                providerConfig, Ids.now());

        // Cleanup: remove any previous binding for this agent (prevents memory leak on re-bind)
        // Also clean orphaned resultToBinding entries so provenance lookup doesn't break
        Set<String> oldBindingIds = new HashSet<>();
        bindings.values().removeIf(old -> {
            if (old.agentId().equals(agentId)) {
                oldBindingIds.add(old.bindingId());
                return true;
            }
            return false;
        });
        if (!oldBindingIds.isEmpty()) {
            resultToBinding.values().removeIf(oldBindingIds::contains);
            candidateToBinding.values().removeIf(oldBindingIds::contains);
        }

        bindings.put(binding.bindingId(), binding);
        return binding.bindingId();
    }

    // -- 3. call() --

    @Override
    public String call(String bindingId, String goalId, String bodyId,
                       String affordance, String prompt, String requiredFields, String context) {
        if (bindingId == null || bindingId.isBlank()) return "ERROR:missing_binding_id";
        ProviderBinding binding = bindings.get(bindingId);
        if (binding == null) return "ERROR:unknown_binding";

        // Get agentId from binding
        String agentId = binding.agentId();

        // Parse affordance -- case-insensitive to prevent silent failures
        // when agents pass "answer" instead of "ANSWER"
        if (affordance == null || affordance.isBlank()) return "ERROR:missing_affordance";
        BodyAffordance parsedAff;
        try {
            parsedAff = BodyAffordance.valueOf(affordance.toUpperCase());
        } catch (Exception e) {
            String allowed = Arrays.stream(BodyAffordance.values())
                    .map(Enum::name).collect(Collectors.joining(","));
            return "ERROR:invalid_affordance:" + affordance + ":allowed=" + allowed;
        }
        final BodyAffordance aff = parsedAff;

        // Parse required fields
        List<String> fields = (requiredFields == null || requiredFields.isBlank())
                ? List.of()
                : Arrays.stream(requiredFields.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

        // Build belief context from knowledge context string
        List<String> beliefContext = (context == null || context.isBlank())
                ? List.of()
                : List.of(context);

        // Pass safe binding config parameters into invocation
        Map<String, String> invocationParams = binding.config() != null
                ? new HashMap<>(binding.config().asMap()) : new HashMap<>();

        // Build invocation with belief context so it reaches the LLM prompt
        BodyInvocation invocation = new BodyInvocation(
                agentId, goalId == null ? "" : goalId,
                bodyId == null ? "llm.answer" : bodyId,
                aff, prompt == null ? "" : prompt,
                fields, invocationParams, beliefContext, "");

        // Resolve body -- strict registry, no dynamic fallback
        String resolvedBodyId = bodyId == null || bodyId.isBlank() ? "llm.answer" : bodyId;
        Optional<GenerativeBody> bodyOpt = binding.bodies().get(resolvedBodyId);
        if (bodyOpt.isEmpty()) {
            String registered = binding.bodies().descriptors().stream()
                    .map(BodyDescriptor::bodyId).sorted()
                    .collect(Collectors.joining(","));
            return "ERROR:unknown_body:" + resolvedBodyId + ":registered=" + registered;
        }
        GenerativeBody body = bodyOpt.get();

        // Verify the body supports the requested affordance
        if (!body.descriptor().affordances().contains(aff)) {
            String allowed = body.descriptor().affordances().stream()
                    .map(Enum::name).collect(Collectors.joining(","));
            return "ERROR:unsupported_affordance:" + resolvedBodyId
                    + ":" + aff.name() + ":allowed=" + allowed;
        }

        InvocationResult invResult = body.invoke(invocation);
        String resultId = invResult.resourceResult().resultId();
        resultToBinding.put(resultId, bindingId);
        String cId = invResult.resourceResult().candidateId();
        if (cId != null && !cId.isBlank()) candidateToBinding.put(cId, bindingId);
        String tId = invResult.resourceResult().traceId();
        if (tId != null && !tId.isBlank()) traceToResult.put(tId, resultId);
        return resultId;
    }

    // -- 4. result() --

    @Override
    public String result(String resultId) {
        return results.get(resultId)
                .map(r -> r.outcome().name())
                .orElse("ERROR:not_found");
    }

    // -- 5. candidate() --

    @Override
    public String candidate(String resultId) {
        Optional<ResourceResult> res = results.get(resultId);
        if (res.isEmpty()) return "ERROR:not_found";
        String cid = res.get().candidateId();
        if (cid == null || cid.isBlank()) return "ERROR:no_candidate";
        return cid;
    }

    // -- 6. check() --

    @Override
    public String check(String refId) {
        if (refId == null || refId.isBlank()) return "ERROR:missing_reference";

        // Result validation check
        if (refId.startsWith("res_")) {
            return results.get(refId).map(r -> {
                if (r.validation() == null) return "RESULT:UNKNOWN";
                if (r.validation().valid()) return "RESULT:VALID";
                List<String> errors = r.validation().errors();
                String detail = (errors == null || errors.isEmpty()) ? r.validation().message()
                        : "missing=" + String.join(",", errors);
                return "RESULT:INVALID:" + detail;
            }).orElse("ERROR:not_found");
        }

        // Candidate status check
        if (refId.startsWith("cand_")) {
            return candidates.get(refId)
                    .map(c -> "CANDIDATE:STATUS=" + c.status().name())
                    .orElse("ERROR:not_found");
        }

        return "ERROR:check_only_supports:res_*,cand_*";
    }

    // -- 7. get() --

    @Override
    public String get(String candidateId, String fieldName) {
        if (candidateId == null || candidateId.isBlank()) return "ERROR:missing_candidate_id";
        if (fieldName == null || fieldName.isBlank()) return "ERROR:missing_field_name";

        Optional<Candidate> cOpt = candidates.get(candidateId);
        if (cOpt.isEmpty()) return "ERROR:not_found";

        Map<String, String> fields = cOpt.get().fields();
        if (fields == null || fields.isEmpty()) return "ERROR:missing_field";

        // Level 1: exact match
        if (fields.containsKey(fieldName)) return fields.get(fieldName);

        // Level 2: case-insensitive
        for (Map.Entry<String, String> e : fields.entrySet()) {
            if (e.getKey().equalsIgnoreCase(fieldName)) return e.getValue();
        }

        // Level 3: alias fallback (e.g., "answer" > first field)
        if ("answer".equalsIgnoreCase(fieldName) && fields.size() == 1) {
            return fields.values().iterator().next();
        }

        return "ERROR:missing_field";
    }

    // -- 8. judge() --

    @Override
    public String judge(String candidateId, String assessorId, String verdict,
                        double confidence, String rationale) {
        if (candidateId == null || candidateId.isBlank()) return "ERROR:missing_candidate_id";
        if (assessorId == null || assessorId.isBlank()) return "ERROR:missing_assessor_id";
        if (rationale == null || rationale.isBlank()) return "ERROR:missing_rationale";

        // Resolve candidate first
        Optional<Candidate> cOpt = candidates.get(candidateId);
        if (cOpt.isEmpty()) return "ERROR:not_found";

        // Prevent lifecycle regression: no assessment after final decision
        if (isDecided(cOpt.get())) {
            return alreadyDecidedError(candidateId, cOpt.get().status());
        }

        // Constraint: INVALID candidates cannot be rehabilitated by judgement
        if (cOpt.get().status() == CandidateStatus.INVALID) {
            return "ERROR:not_assessable:INVALID";
        }

        // Validate verdict -- case-insensitive to prevent silent failures
        if (verdict == null || verdict.isBlank()) return "ERROR:missing_verdict";
        Outcomes.AssessmentVerdict v;
        try {
            v = Outcomes.AssessmentVerdict.valueOf(verdict.toUpperCase());
        } catch (Exception e) {
            return "ERROR:invalid_verdict:" + verdict + ":allowed=APPROVE,WARN,REJECT_VERDICT,UNCERTAIN";
        }

        // Validate confidence (NaN/Infinity bypass < > comparisons, must check explicitly)
        if (Double.isNaN(confidence) || Double.isInfinite(confidence)
                || confidence < 0.0 || confidence > 1.0) {
            return "ERROR:invalid_confidence:" + confidence + ":range=0.0-1.0";
        }

        // Store assessment using the kernel (which now uses resolveCandidate -- fixed bug)
        // We call the kernel's assess method which handles candidate resolution and status update
        GovernanceKernel kernel = findKernelForCandidate(candidateId);
        if (kernel == null) return "ERROR:no_kernel";

        Assessment assessment = kernel.assess(
                assessorId, candidateId, "candidate", v, confidence,
                List.of(), List.of(), rationale);

        // Defensive: kernel returns null if candidate was decided between our check and its call
        if (assessment == null) {
            Optional<Candidate> latest = candidates.get(candidateId);
            if (latest.isPresent() && isDecided(latest.get())) {
                return alreadyDecidedError(candidateId, latest.get().status());
            }
            return "ERROR:assessment_failed";
        }
        return assessment.assessmentId();
    }

    // -- 9. decide() --

    @Override
    public String decide(String candidateId) {
        if (candidateId == null || candidateId.isBlank()) return "ERROR:missing_candidate_id";

        Optional<Candidate> cOpt = candidates.get(candidateId);
        if (cOpt.isEmpty()) return "ERROR:not_found";

        // Final-aware: if already decided, return consistent admissibility
        // so ASTRA context guards (== "ADMISSIBLE") work on re-evaluation
        if (isDecided(cOpt.get())) {
            if (cOpt.get().status() == CandidateStatus.ACCEPTED_BY_AGENT) {
                return "ADMISSIBLE";
            } else {
                return "INADMISSIBLE:already_rejected";
            }
        }

        AdmissibilityDecision decision = admissibilityChecker.check(
                cOpt.get(), assessments.forTarget(candidateId));

        if (decision.outcome() == Outcomes.AdmissibilityOutcome.ADMISSIBLE) {
            return "ADMISSIBLE";
        } else {
            return "INADMISSIBLE:" + decision.reason();
        }
    }

    // -- 10. accept() --

    @Override
    public String accept(String candidateId, String reason) {
        if (candidateId == null || candidateId.isBlank()) return "ERROR:missing_candidate_id";
        if (reason == null || reason.isBlank()) return "ERROR:missing_reason";

        // Resolve candidate first
        Optional<Candidate> cOpt = candidates.get(candidateId);
        if (cOpt.isEmpty()) return "ERROR:not_found";

        // Prevent duplicate decisions
        if (isDecided(cOpt.get())) {
            return alreadyDecidedError(candidateId, cOpt.get().status());
        }

        // Enforce admissibility
        AdmissibilityDecision admDecision = admissibilityChecker.check(
                cOpt.get(), assessments.forTarget(candidateId));
        if (admDecision.outcome() != Outcomes.AdmissibilityOutcome.ADMISSIBLE) {
            return "ERROR:not_admissible:" + admDecision.reason();
        }

        // Record decision -- agentId comes from the candidate (which got it from the binding)
        GovernanceKernel kernel = findKernelForCandidate(candidateId);
        if (kernel == null) return "ERROR:no_kernel";

        Optional<Decision> dec = kernel.recordDecision(candidateId, DecisionType.ACCEPTED, reason);
        return dec.map(Decision::decisionId).orElse("ERROR:not_found");
    }

    // -- 11. reject() --

    @Override
    public String reject(String candidateId, String reason) {
        if (candidateId == null || candidateId.isBlank()) return "ERROR:missing_candidate_id";
        if (reason == null || reason.isBlank()) return "ERROR:missing_reason";

        // Resolve candidate -- must exist, but no admissibility check needed
        Optional<Candidate> cOpt = candidates.get(candidateId);
        if (cOpt.isEmpty()) return "ERROR:not_found";

        // Prevent duplicate decisions
        if (isDecided(cOpt.get())) {
            return alreadyDecidedError(candidateId, cOpt.get().status());
        }

        GovernanceKernel kernel = findKernelForCandidate(candidateId);
        if (kernel == null) return "ERROR:no_kernel";

        Optional<Decision> dec = kernel.recordDecision(candidateId, DecisionType.REJECTED, reason);
        return dec.map(Decision::decisionId).orElse("ERROR:not_found");
    }

    // -- 12. knowledge() --

    @Override
    public String knowledge(String agentId) {
        if (agentId == null || agentId.isBlank()) return "EMPTY";
        StringBuilder sb = new StringBuilder();
        boolean firstEntry = true;
        for (Candidate c : candidates.all()) {
            if (c.status() == CandidateStatus.ACCEPTED_BY_AGENT
                    && c.agentId().equals(agentId)
                    && !c.fields().isEmpty()) {
                if (!firstEntry) sb.append(';');
                firstEntry = false;
                boolean firstField = true;
                for (Map.Entry<String, String> e : c.fields().entrySet()) {
                    if (!firstField) sb.append(',');
                    firstField = false;
                    // Sanitize field values: remove semicolons, commas, and newlines
                    String safeV = e.getValue().replace(";", " ").replace(",", " ")
                            .replace("\n", " ").replace("\r", "");
                    sb.append(e.getKey()).append('=').append(safeV);
                }
            }
        }
        return sb.length() == 0 ? "EMPTY" : sb.toString();
    }

    // -- 13. explain() --

    @Override
    public String explain(String refId) {
        if (refId == null || refId.isBlank()) return "ERROR:missing_reference";

        // Route by prefix to actual stored records
        if (refId.startsWith("res_")) {
            return explainResult(refId);
        } else if (refId.startsWith("cand_")) {
            return explainCandidate(refId);
        } else if (refId.startsWith("assess_")) {
            return explainAssessment(refId);
        } else if (refId.startsWith("dec_")) {
            return explainDecision(refId);
        } else if (refId.startsWith("trace_")) {
            return explainTrace(refId);
        } else if (refId.startsWith("bind_")) {
            return explainBinding(refId);
        }

        return "ERROR:unknown_reference";
    }

    // -- Private helpers --

    private String explainBinding(String bindingId) {
        ProviderBinding b = bindings.get(bindingId);
        if (b == null) return "ERROR:not_found";

        StringBuilder sb = new StringBuilder();
        sb.append("type=binding");
        sb.append(";id=").append(b.bindingId());
        sb.append(";agent=").append(b.agentId());
        sb.append(";provider=").append(b.providerName());
        sb.append(";model=").append(b.modelName());

        // Config keys (without values for security -- no API keys leaked)
        if (b.config() != null && !b.config().asMap().isEmpty()) {
            String keys = b.config().asMap().keySet().stream()
                    .sorted()
                    .collect(Collectors.joining(","));
            sb.append(";config_keys=").append(keys);
        }

        // Registered body IDs
        if (b.bodies() != null) {
            String bodyIds = b.bodies().descriptors().stream()
                    .map(BodyDescriptor::bodyId)
                    .sorted()
                    .collect(Collectors.joining(","));
            if (!bodyIds.isEmpty()) {
                sb.append(";bodies=").append(bodyIds);
            }
        }

        sb.append(";created=").append(b.createdAt());
        return sb.toString();
    }

    private String explainResult(String resultId) {
        return results.get(resultId).map(r -> {
            StringBuilder sb = new StringBuilder();
            sb.append("type=result");

            // Find trace by exact traceId stored in result
            Optional<TraceRecord> trace = r.traceId().isBlank()
                    ? Optional.empty()
                    : traces.get(r.traceId());

            trace.ifPresent(t -> {
                sb.append(";agent=").append(t.agentId());
                sb.append(";goal=").append(t.goalId());
                sb.append(";resource=").append(t.resourceId());
            });

            sb.append(";result=").append(r.outcome().name());

            if (r.validation() != null) {
                sb.append(";validation=").append(r.validation().valid() ? "VALID" : "INVALID");
            }
            if (!r.candidateId().isBlank()) {
                sb.append(";candidate=").append(r.candidateId());
            }
            if (!r.traceId().isBlank()) {
                sb.append(";trace=").append(r.traceId());
            }
            return sb.toString();
        }).orElse("ERROR:not_found");
    }

    private String explainCandidate(String candidateId) {
        return candidates.get(candidateId).map(c -> {
            StringBuilder sb = new StringBuilder();
            sb.append("type=candidate");
            sb.append(";id=").append(c.candidateId());
            sb.append(";agent=").append(c.agentId());
            sb.append(";goal=").append(c.goalId());
            sb.append(";status=").append(c.status().name());
            if (!c.fields().isEmpty()) {
                String fieldStr = c.fields().entrySet().stream()
                        .map(e -> e.getKey() + ":" + sanitize(e.getValue()))
                        .collect(Collectors.joining(","));
                sb.append(";fields=").append(fieldStr);
            }

            // -- Assessment evidence --
            List<Assessment> assessmentList = assessments.forTarget(candidateId);
            sb.append(";assessment_count=").append(assessmentList.size());
            if (!assessmentList.isEmpty()) {
                // Latest by timestamp
                Assessment latest = assessmentList.stream()
                        .max(java.util.Comparator.comparing(Assessment::createdAt))
                        .orElse(assessmentList.get(assessmentList.size() - 1));
                sb.append(";latest_assessment=").append(latest.assessmentId());
                sb.append(";assessment_verdict=").append(latest.verdict().name());
                sb.append(";assessment_confidence=").append(latest.confidence());
                sb.append(";assessment_rationale=").append(sanitize(latest.explanation()));
            }

            // -- Live admissibility (or final state for decided candidates) --
            if (c.status() == CandidateStatus.ACCEPTED_BY_AGENT
                    || c.status() == CandidateStatus.REJECTED_BY_AGENT) {
                sb.append(";admissibility=FINAL:").append(c.status().name());
            } else {
                AdmissibilityDecision adm = admissibilityChecker.check(c, assessmentList);
                sb.append(";admissibility=").append(adm.outcome().name());
                sb.append(";admissibility_reason=").append(sanitize(adm.reason()));
            }

            // -- Decision evidence --
            List<Decision> decisionList = decisions.forCandidate(candidateId);
            sb.append(";decision_count=").append(decisionList.size());
            if (!decisionList.isEmpty()) {
                // Latest by timestamp
                Decision latestDec = decisionList.stream()
                        .max(java.util.Comparator.comparing(Decision::timestamp))
                        .orElse(decisionList.get(decisionList.size() - 1));
                sb.append(";latest_decision=").append(latestDec.decisionId());
                sb.append(";decision=").append(latestDec.type().name());
                sb.append(";decision_reason=").append(sanitize(latestDec.reason()));
            }

            return sb.toString();
        }).orElse("ERROR:not_found");
    }

    /** Sanitize a value for the semicolon-separated explain format:
     *  replace semicolons and newlines with spaces. */
    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace(';', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private String explainAssessment(String assessmentId) {
        return assessments.get(assessmentId).map(a -> {
            StringBuilder sb = new StringBuilder();
            sb.append("type=assessment");
            sb.append(";id=").append(a.assessmentId());
            sb.append(";assessor=").append(a.assessorId());
            sb.append(";verdict=").append(a.verdict().name());
            sb.append(";confidence=").append(a.confidence());
            sb.append(";rationale=").append(sanitize(a.explanation()));
            sb.append(";candidate=").append(a.targetRef());
            return sb.toString();
        }).orElse("ERROR:not_found");
    }

    private String explainDecision(String decisionId) {
        return decisions.get(decisionId).map(d -> {
            StringBuilder sb = new StringBuilder();
            sb.append("type=decision");
            sb.append(";id=").append(d.decisionId());
            sb.append(";candidate=").append(d.candidateId());
            sb.append(";agent=").append(d.agentId());
            sb.append(";decision=").append(d.type().name());
            sb.append(";reason=").append(sanitize(d.reason()));
            sb.append(";timestamp=").append(d.timestamp());
            return sb.toString();
        }).orElse("ERROR:not_found");
    }

    private String explainTrace(String traceId) {
        return traces.get(traceId).map(t -> {
            StringBuilder sb = new StringBuilder();
            sb.append("type=trace");
            sb.append(";id=").append(t.traceId());
            sb.append(";agent=").append(t.agentId());
            sb.append(";goal=").append(t.goalId());
            sb.append(";resource=").append(t.resourceId());
            sb.append(";result=").append(t.outcome().name());

            // -- Provider/model/binding (exact provenance via trace > result > binding) --
            Optional.ofNullable(traceToResult.get(traceId))
                    .map(resultToBinding::get)
                    .map(bindings::get)
                    .ifPresent(b -> {
                        sb.append(";binding=").append(b.bindingId());
                        sb.append(";provider=").append(b.providerName());
                        sb.append(";model=").append(b.modelName());
                    });

            // -- Prompt/output blob hashes --
            if (!t.promptBlobId().isBlank()) {
                blobs.get(t.promptBlobId()).ifPresent(b ->
                        sb.append(";prompt_hash=").append(b.sha256()));
            }
            if (!t.outputBlobId().isBlank()) {
                blobs.get(t.outputBlobId()).ifPresent(b ->
                        sb.append(";output_hash=").append(b.sha256()));
            }

            // -- Validation --
            if (t.validationResult() != null) {
                sb.append(";validation=").append(t.validationResult().valid() ? "VALID" : "INVALID");
            }

            // -- Policy --
            if (t.policyDecision() != null) {
                sb.append(";policy=").append(t.policyDecision().outcome().name());
            }

            // -- Candidate link --
            if (!t.candidateId().isBlank()) {
                sb.append(";candidate=").append(t.candidateId());
            }

            sb.append(";timestamp=").append(t.createdAt());
            return sb.toString();
        }).orElse("ERROR:not_found");
    }

    /** Find the kernel that produced this candidate via result provenance.
     *  Falls back to any available kernel (stores are shared, so lifecycle
     *  ops like accept/reject work regardless of which kernel is used). */
    private GovernanceKernel findKernelForCandidate(String candidateId) {
        if (bindings.isEmpty()) return null;
        // 1. O(1) reverse index: candidate > binding > kernel
        String bid = candidateToBinding.get(candidateId);
        if (bid != null) {
            ProviderBinding b = bindings.get(bid);
            if (b != null) return b.kernel();
        }
        // 2. Fallback: any kernel (shared stores make this safe for lifecycle ops)
        return bindings.values().iterator().next().kernel();
    }

    /** Check if a candidate has reached a final decision state. */
    private static boolean isDecided(Candidate c) {
        return c.status() == CandidateStatus.ACCEPTED_BY_AGENT
            || c.status() == CandidateStatus.REJECTED_BY_AGENT;
    }

    /** Build error string for already-decided candidates, including the existing decision ID. */
    private String alreadyDecidedError(String candidateId, CandidateStatus status) {
        List<Decision> existing = decisions.forCandidate(candidateId);
        String decId = existing.isEmpty() ? "unknown" : existing.get(existing.size() - 1).decisionId();
        return "ERROR:already_decided:" + status.name() + ":" + decId;
    }
}
