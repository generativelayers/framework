package gl;

import gl.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** The central governance engine of the Generative Layers framework.
 *
 *  <p>Orchestrates the full pipeline: policy check &gt; provider call &gt;
 *  output validation &gt; candidate creation &gt; assessment &gt; admissibility
 *  &gt; acceptance/rejection. Every step produces auditable traces.
 *
 *  <p>Supports retry/recovery on invalid output, conversation context
 *  for multi-turn dialogues, and event hooks via {@link KernelListener}.
 *
 *  <p>The kernel is configured via {@link GovernanceKernelFactory} with
 *  pluggable ports ({@link KernelPorts}): governance policy, output
 *  validator, admissibility checker, provider, and blob store. */
public final class GovernanceKernel {
    private final KernelPorts.GenerativeProvider provider;
    private final KernelPorts.ResponseValidator validator;
    private final KernelPorts.GovernancePolicy governance;
    private final KernelPorts.AdmissibilityChecker admissibility;
    private final KernelPorts.BlobStore blobs;
    private final KernelPorts.CandidateStore candidates;
    private final KernelPorts.AssessmentStore assessments;
    private final KernelPorts.ResultStore results;
    private final KernelPorts.TraceSink traces;
    private final KernelPorts.MetricsSink metrics;
    private final RetryPolicy retryPolicy;
    private final List<KernelListener> listeners;
    private final ConcurrentHashMap<String, ConversationContext> conversations = new ConcurrentHashMap<>();

    public GovernanceKernel(KernelPorts.GenerativeProvider provider, KernelPorts.ResponseValidator validator,
                     KernelPorts.GovernancePolicy governance, KernelPorts.AdmissibilityChecker admissibility,
                     KernelPorts.BlobStore blobs, KernelPorts.CandidateStore candidates,
                     KernelPorts.AssessmentStore assessments, KernelPorts.ResultStore results,
                     KernelPorts.TraceSink traces, KernelPorts.MetricsSink metrics) {
        this(provider, validator, governance, admissibility, blobs, candidates,
                assessments, results, traces, metrics, RetryPolicy.none(), List.of());
    }

    public GovernanceKernel(KernelPorts.GenerativeProvider provider, KernelPorts.ResponseValidator validator,
                     KernelPorts.GovernancePolicy governance, KernelPorts.AdmissibilityChecker admissibility,
                     KernelPorts.BlobStore blobs, KernelPorts.CandidateStore candidates,
                     KernelPorts.AssessmentStore assessments, KernelPorts.ResultStore results,
                     KernelPorts.TraceSink traces, KernelPorts.MetricsSink metrics,
                     RetryPolicy retryPolicy, List<KernelListener> listeners) {
        this.provider = Objects.requireNonNull(provider);
        this.validator = Objects.requireNonNull(validator);
        this.governance = Objects.requireNonNull(governance);
        this.admissibility = Objects.requireNonNull(admissibility);
        this.blobs = Objects.requireNonNull(blobs);
        this.candidates = Objects.requireNonNull(candidates);
        this.assessments = Objects.requireNonNull(assessments);
        this.results = Objects.requireNonNull(results);
        this.traces = Objects.requireNonNull(traces);
        this.metrics = Objects.requireNonNull(metrics);
        this.retryPolicy = retryPolicy == null ? RetryPolicy.none() : retryPolicy;
        this.listeners = listeners == null ? List.of() : List.copyOf(listeners);
    }

    public ResourceResult invoke(ResourceRequest request) {
        metrics.increment("gl.invoke.total");

        // 1. Policy gate
        PolicyDecision policy = governance.evaluate(request);
        if (!policy.allowed()) {
            fire(l -> l.onPolicyDenied(request, policy));
            return deniedResult(request, policy);
        }

        // 2. Resolve conversation context
        String effectivePrompt = request.prompt();
        ConversationContext conversation = null;
        if (!request.conversationId().isEmpty()) {
            conversation = conversations.computeIfAbsent(request.conversationId(), ConversationContext::new);
            effectivePrompt = conversation.buildPrompt(effectivePrompt);
        }

        Blob promptBlob = blobs.put(Blob.of(BlobType.PROMPT, effectivePrompt,
                Map.of("requestId", request.requestId())));

        // 3. Generate with retry loop
        ProviderOutput output = null;
        ValidationResult validation = null;
        Exception lastError = null;
        String lastPrompt = effectivePrompt;

        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            final int currentAttempt = attempt;

            // Build request with (potentially amended) prompt
            ResourceRequest attemptRequest = attempt == 1 ? request
                    : new ResourceRequest(request.requestId(), request.agentId(), request.goalId(),
                    request.resourceId(), request.taskType(), request.expectedCandidateType(),
                    lastPrompt, request.schema(), request.governanceContext(),
                    request.parameters(), request.conversationId());

            try {
                output = provider.generate(attemptRequest, promptBlob);
                lastError = null;
            } catch (Exception ex) {
                lastError = ex;
                System.err.println("[GL] Provider error: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
                fire(l -> l.onProviderFailed(request, ex));
                if (attempt < retryPolicy.maxAttempts()) {
                    fire(l -> l.onRetry(request, currentAttempt, "provider error: " + ex.getMessage()));
                    if (retryPolicy.includeErrorFeedback()) {
                        lastPrompt = effectivePrompt + "\n\n[System: Previous attempt failed: " + ex.getMessage() + ". Please try again.]";
                    }
                    continue;
                }
                // Final attempt failed
                validation = ValidationResult.invalid("provider exception",
                        List.of(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
                TraceRecord trace = recordTrace(request, promptBlob.blobId(), "", "", Outcomes.ResultOutcome.PROVIDER_FAILED, policy, validation);
                return results.put(new ResourceResult(Ids.id("res"), request.requestId(), Outcomes.ResultOutcome.PROVIDER_FAILED,
                        promptBlob.blobId(), "", "", trace.traceId(), validation, policy, validation.message(), Ids.now()));
            }

            // Validate
            validation = validator.validate(output.rawText(), request.schema());
            if (validation.valid()) break; // Success — exit retry loop

            final ValidationResult failedValidation = validation;
            fire(l -> l.onValidationFailed(request, failedValidation));
            if (attempt < retryPolicy.maxAttempts()) {
                fire(l -> l.onRetry(request, currentAttempt, "validation failed: " + failedValidation.message()));
                if (retryPolicy.includeErrorFeedback()) {
                    lastPrompt = effectivePrompt + "\n\n[System: Your previous response was invalid. Errors: "
                            + String.join("; ", validation.errors())
                            + ". Required format: key=value lines with fields: "
                            + String.join(", ", request.schema().requiredFields()) + "]";
                }
            }
        }

        // 4. Create candidate
        String resultId = Ids.id("res");
        Blob outputBlob = blobs.put(Blob.of(BlobType.GENERATIVE_OUTPUT, output.rawText(),
                Map.of("resultId", resultId, "provider", output.providerName())));
        CandidateStatus status = validation.valid() ? CandidateStatus.VALIDATED : CandidateStatus.INVALID;
        Candidate candidate = candidates.put(new Candidate(Ids.id("cand"), request.expectedCandidateType(), status,
                resultId, outputBlob.blobId(), request.agentId(), request.goalId(),
                validation.fields(), List.of(outputBlob.blobId()), Ids.now()));
        fire(l -> l.onCandidateCreated(candidate));

        Outcomes.ResultOutcome outcome = validation.valid() ? Outcomes.ResultOutcome.SUCCESS : Outcomes.ResultOutcome.INVALID_OUTPUT;
        TraceRecord trace = recordTrace(request, promptBlob.blobId(), outputBlob.blobId(),
                candidate.candidateId(), outcome, policy, validation);
        metrics.increment(validation.valid() ? "gl.invoke.success" : "gl.invoke.invalid");

        // 5. Record conversation turn on success
        if (validation.valid() && conversation != null) {
            conversation.addTurn("user", request.prompt());
            conversation.addTurn("assistant", output.rawText());
        }

        return results.put(new ResourceResult(resultId, request.requestId(), outcome, promptBlob.blobId(),
                outputBlob.blobId(), candidate.candidateId(), trace.traceId(), validation, policy,
                validation.message(), Ids.now()));
    }

    public Assessment assess(String assessorId, String targetRef, String targetType, Outcomes.AssessmentVerdict verdict, double confidence, List<String> criteria, List<String> evidenceRefs, String explanation) {
        Assessment assessment = assessments.put(new Assessment(Ids.id("assess"), assessorId, targetRef, targetType, verdict, confidence, criteria, evidenceRefs, explanation, Ids.now()));
        candidates.get(targetRef).ifPresent(c -> candidates.update(c.withStatus(CandidateStatus.ASSESSED)));
        metrics.increment("gl.assess.total");
        return assessment;
    }

    public AdmissibilityDecision checkAdmissibility(String candidateId) {
        return candidates.get(candidateId)
                .map(c -> admissibility.check(c, assessments.forTarget(candidateId)))
                .orElseGet(() -> new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.INADMISSIBLE, "candidate not found", Map.of("candidateId", candidateId == null ? "" : candidateId)));
    }

    public Optional<Candidate> acceptCandidate(String candidateId) {
        return candidates.get(candidateId).map(c -> {
            Candidate accepted = candidates.update(c.withStatus(CandidateStatus.ACCEPTED_BY_AGENT));
            fire(l -> l.onCandidateAccepted(accepted));
            return accepted;
        });
    }

    public Optional<Candidate> rejectCandidate(String candidateId) {
        return candidates.get(candidateId).map(c -> {
            Candidate rejected = candidates.update(c.withStatus(CandidateStatus.REJECTED_BY_AGENT));
            fire(l -> l.onCandidateRejected(rejected));
            return rejected;
        });
    }

    public Optional<ResourceResult> result(String resultId) { return results.get(resultId); }
    public Optional<Candidate> candidate(String candidateId) { return candidates.get(candidateId); }
    public Optional<Blob> blob(String blobId) { return blobs.get(blobId); }
    public Optional<TraceRecord> trace(String traceId) { return traces.get(traceId); }
    public boolean valid(String resultId) { return result(resultId).map(ResourceResult::success).orElse(false); }
    public String field(String resultId, String fieldName) { return result(resultId).map(ResourceResult::validation).map(ValidationResult::fields).map(f -> f.getOrDefault(fieldName, "")).orElse(""); }
    public List<TraceRecord> traces() { return traces.all(); }
    public List<String> metrics() { return metrics.events(); }

    /** Get or create a conversation context by ID. */
    public ConversationContext conversation(String conversationId) {
        return conversations.computeIfAbsent(conversationId, ConversationContext::new);
    }

    /** Return accepted knowledge for an agent as semicolon-separated field entries.
     *  Each candidate's fields are formatted as "key=value,key=value" and candidates
     *  are separated by ";". Returns "" if no accepted candidates exist. */
    public String acceptedKnowledge(String agentId) {
        StringBuilder sb = new StringBuilder();
        for (Candidate c : candidates.all()) {
            if (c.status() == CandidateStatus.ACCEPTED_BY_AGENT
                    && c.agentId().equals(agentId)
                    && !c.fields().isEmpty()) {
                if (sb.length() > 0) sb.append(';');
                c.fields().forEach((k, v) -> {
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ';') sb.append(',');
                    sb.append(k).append('=').append(v);
                });
            }
        }
        return sb.toString();
    }

    private ResourceResult deniedResult(ResourceRequest request, PolicyDecision policy) {
        Outcomes.ResultOutcome outcome = policy.outcome() == Outcomes.PolicyOutcome.ESCALATE ? Outcomes.ResultOutcome.GOVERNANCE_ESCALATED : Outcomes.ResultOutcome.GOVERNANCE_DENIED;
        ValidationResult validation = ValidationResult.invalid(policy.reason(), List.of(policy.reason()));
        TraceRecord trace = recordTrace(request, "", "", "", outcome, policy, validation);
        metrics.increment("gl.invoke.denied");
        return results.put(new ResourceResult(Ids.id("res"), request.requestId(), outcome, "", "", "", trace.traceId(), validation, policy, policy.reason(), Ids.now()));
    }

    private TraceRecord recordTrace(ResourceRequest request, String promptBlobId, String outputBlobId, String candidateId, Outcomes.ResultOutcome outcome, PolicyDecision policy, ValidationResult validation) {
        return traces.record(new TraceRecord(Ids.id("trace"), request.requestId(), request.agentId(), request.goalId(), request.resourceId(), request.taskType(), promptBlobId, outputBlobId, candidateId, "", outcome, policy, null, validation, Ids.now()));
    }

    private void fire(java.util.function.Consumer<KernelListener> event) {
        for (KernelListener l : listeners) {
            try { event.accept(l); } catch (Exception ignored) { /* listener errors must not break the pipeline */ }
        }
    }
}
