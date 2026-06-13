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
 *  <p>Performs the governed invocation pipeline: policy check, provider call,
 *  output validation, candidate creation, and trace recording.
 *  Assessment, admissibility checking, and accept/reject decisions are
 *  exposed through later lifecycle commands, not during invocation.
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
    private final KernelPorts.DecisionStore decisions;
    private final RetryPolicy retryPolicy;
    private final List<KernelListener> listeners;
    private final ConcurrentHashMap<String, ConversationContext> conversations = new ConcurrentHashMap<>();

    public GovernanceKernel(KernelPorts.GenerativeProvider provider, KernelPorts.ResponseValidator validator,
                     KernelPorts.GovernancePolicy governance, KernelPorts.AdmissibilityChecker admissibility,
                     KernelPorts.BlobStore blobs, KernelPorts.CandidateStore candidates,
                     KernelPorts.AssessmentStore assessments, KernelPorts.ResultStore results,
                     KernelPorts.TraceSink traces, KernelPorts.MetricsSink metrics,
                     KernelPorts.DecisionStore decisions,
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
        this.decisions = Objects.requireNonNull(decisions);
        this.retryPolicy = retryPolicy == null ? RetryPolicy.none() : retryPolicy;
        this.listeners = listeners == null ? List.of() : List.copyOf(listeners);
    }

    public ResourceResult invoke(ResourceRequest request) {
        metrics.increment("gl.call.total");

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
            if (validation.valid()) break; // Success -- exit retry loop

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
                validation.fields(), List.of(outputBlob.blobId()), Ids.now(), null));
        fire(l -> l.onCandidateCreated(candidate));

        Outcomes.ResultOutcome outcome = validation.valid() ? Outcomes.ResultOutcome.SUCCESS : Outcomes.ResultOutcome.INVALID_OUTPUT;
        TraceRecord trace = recordTrace(request, promptBlob.blobId(), outputBlob.blobId(),
                candidate.candidateId(), outcome, policy, validation);
        metrics.increment(validation.valid() ? "gl.call.success" : "gl.call.invalid");

        // 5. Record conversation turn on success
        if (validation.valid() && conversation != null) {
            conversation.addTurn("user", request.prompt());
            conversation.addTurn("assistant", output.rawText());
        }

        return results.put(new ResourceResult(resultId, request.requestId(), outcome, promptBlob.blobId(),
                outputBlob.blobId(), candidate.candidateId(), trace.traceId(), validation, policy,
                validation.message(), Ids.now()));
    }

    public synchronized Assessment assess(String assessorId, String targetRef, String targetType, Outcomes.AssessmentVerdict verdict, double confidence, List<String> criteria, List<String> evidenceRefs, String explanation) {
        // Resolve candidate FIRST -- no assessment record for missing candidates
        Optional<Candidate> cOpt = resolveCandidate(targetRef);
        if (cOpt.isEmpty()) return null;

        // Finality guard: do not allow assessment after final decision
        Candidate c = cOpt.get();
        if (c.status() == CandidateStatus.ACCEPTED_BY_AGENT
                || c.status() == CandidateStatus.REJECTED_BY_AGENT) {
            return null;
        }

        // INVALID candidates cannot be rehabilitated by assessment
        if (c.status() == CandidateStatus.INVALID) {
            return null;
        }

        // Canonicalise: always store under the candidate's canonical ID
        Assessment assessment = assessments.put(new Assessment(Ids.id("assess"), assessorId, c.candidateId(), "candidate", verdict, confidence, criteria, evidenceRefs, explanation, Ids.now()));
        candidates.update(c.withStatus(CandidateStatus.ASSESSED));
        metrics.increment("gl.judge.total");
        return assessment;
    }

    /** Resolve a candidate by candidate ID or result ID.
     *  Agents commonly pass result IDs to commands,
     *  so this method transparently resolves both.
     *  Uses the reverse index for O(1) result-to-candidate lookup. */
    public Optional<Candidate> resolveCandidate(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        // 1. Direct lookup by candidate ID
        Optional<Candidate> direct = candidates.get(id);
        if (direct.isPresent()) return direct;
        // 2. Reverse index: result ID > candidate (O(1))
        return candidates.byResultId(id);
    }

    /** Record a decision. Candidate MUST exist -- caller verifies first.
     *  Returns the existing decision if the candidate is already decided.
     *  Returns Optional.empty() if candidate is not found.
     *  Synchronized to prevent TOCTOU races in multi-agent MAS. */
    public synchronized Optional<Decision> recordDecision(String candidateId, DecisionType type, String reason) {
        return resolveCandidate(candidateId).map(c -> {
            // Finality guard: return existing decision if already decided
            if (c.status() == CandidateStatus.ACCEPTED_BY_AGENT
                    || c.status() == CandidateStatus.REJECTED_BY_AGENT) {
                List<Decision> existing = decisions.forCandidate(c.candidateId());
                return existing.isEmpty() ? null : existing.get(existing.size() - 1);
            }

            // Kernel-level admissibility enforcement for ACCEPTED decisions
            if (type == DecisionType.ACCEPTED) {
                AdmissibilityDecision adm = admissibility.check(c, assessments.forTarget(c.candidateId()));
                if (adm.outcome() != Outcomes.AdmissibilityOutcome.ADMISSIBLE) {
                    return null;
                }
            }

            Decision d = decisions.put(new Decision(
                    Ids.id("dec"), c.candidateId(), c.agentId(), type, reason, Ids.now()));
            CandidateStatus newStatus = type == DecisionType.ACCEPTED
                    ? CandidateStatus.ACCEPTED_BY_AGENT
                    : CandidateStatus.REJECTED_BY_AGENT;

            candidates.update(c.withStatus(newStatus));
            if (type == DecisionType.ACCEPTED) {
                fire(l -> l.onCandidateAccepted(c.withStatus(newStatus)));
            } else {
                fire(l -> l.onCandidateRejected(c.withStatus(newStatus)));
            }
            return d;
        });
    }

    public KernelPorts.DecisionStore decisionStore() { return decisions; }
    public KernelPorts.AssessmentStore assessmentStore() { return assessments; }

    public AdmissibilityDecision checkAdmissibility(String candidateOrResultId) {
        return resolveCandidate(candidateOrResultId)
                .map(c -> admissibility.check(c, assessments.forTarget(c.candidateId())))
                .orElseGet(() -> new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.INADMISSIBLE, "candidate not found", Map.of("id", candidateOrResultId == null ? "" : candidateOrResultId)));
    }


    public Optional<ResourceResult> result(String resultId) { return results.get(resultId); }
    public Optional<Candidate> candidate(String candidateOrResultId) { return resolveCandidate(candidateOrResultId); }
    public Optional<Blob> blob(String blobId) { return blobs.get(blobId); }
    public Optional<TraceRecord> trace(String traceId) { return traces.get(traceId); }
    public List<TraceRecord> traces() { return traces.all(); }
    public List<String> metrics() { return metrics.events(); }

    /** Get or create a conversation context by ID. */
    public ConversationContext conversation(String conversationId) {
        return conversations.computeIfAbsent(conversationId, ConversationContext::new);
    }

    private ResourceResult deniedResult(ResourceRequest request, PolicyDecision policy) {
        Outcomes.ResultOutcome outcome = policy.outcome() == Outcomes.PolicyOutcome.ESCALATE ? Outcomes.ResultOutcome.GOVERNANCE_ESCALATED : Outcomes.ResultOutcome.GOVERNANCE_DENIED;
        ValidationResult validation = ValidationResult.invalid(policy.reason(), List.of(policy.reason()));
        TraceRecord trace = recordTrace(request, "", "", "", outcome, policy, validation);
        metrics.increment("gl.call.denied");
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
