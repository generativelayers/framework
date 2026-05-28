package layer.kernel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Kernel {
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

    public Kernel(KernelPorts.GenerativeProvider provider, KernelPorts.ResponseValidator validator,
                     KernelPorts.GovernancePolicy governance, KernelPorts.AdmissibilityChecker admissibility,
                     KernelPorts.BlobStore blobs, KernelPorts.CandidateStore candidates,
                     KernelPorts.AssessmentStore assessments, KernelPorts.ResultStore results,
                     KernelPorts.TraceSink traces, KernelPorts.MetricsSink metrics) {
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
    }

    public ResourceResult invoke(ResourceRequest request) {
        metrics.increment("layer.invoke.total");
        PolicyDecision policy = governance.evaluate(request);
        if (!policy.allowed()) return deniedResult(request, policy);
        Blob promptBlob = blobs.put(Blob.of(BlobType.PROMPT, request.prompt(), Map.of("requestId", request.requestId())));
        ProviderOutput output;
        try {
            output = provider.generate(request, promptBlob);
        } catch (Exception ex) {
            ValidationResult validation = ValidationResult.invalid("provider exception", List.of(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            TraceRecord trace = recordTrace(request, promptBlob.blobId(), "", "", Outcomes.ResultOutcome.PROVIDER_FAILED, policy, validation);
            return results.put(new ResourceResult(Ids.id("res"), request.requestId(), Outcomes.ResultOutcome.PROVIDER_FAILED, promptBlob.blobId(), "", "", trace.traceId(), validation, policy, validation.message(), Ids.now()));
        }
        String resultId = Ids.id("res");
        Blob outputBlob = blobs.put(Blob.of(BlobType.GENERATIVE_OUTPUT, output.rawText(), Map.of("resultId", resultId, "provider", output.providerName())));
        ValidationResult validation = validator.validate(output.rawText(), request.schema());
        CandidateStatus status = validation.valid() ? CandidateStatus.VALIDATED : CandidateStatus.INVALID;
        Candidate candidate = candidates.put(new Candidate(Ids.id("cand"), request.expectedCandidateType(), status, resultId, outputBlob.blobId(), request.agentId(), request.goalId(), validation.fields(), List.of(outputBlob.blobId()), Ids.now()));
        Outcomes.ResultOutcome outcome = validation.valid() ? Outcomes.ResultOutcome.SUCCESS : Outcomes.ResultOutcome.INVALID_OUTPUT;
        TraceRecord trace = recordTrace(request, promptBlob.blobId(), outputBlob.blobId(), candidate.candidateId(), outcome, policy, validation);
        metrics.increment(validation.valid() ? "layer.invoke.success" : "layer.invoke.invalid");
        return results.put(new ResourceResult(resultId, request.requestId(), outcome, promptBlob.blobId(), outputBlob.blobId(), candidate.candidateId(), trace.traceId(), validation, policy, validation.message(), Ids.now()));
    }

    public Assessment assess(String assessorId, String targetRef, String targetType, Outcomes.AssessmentVerdict verdict, double confidence, List<String> criteria, List<String> evidenceRefs, String explanation) {
        Assessment assessment = assessments.put(new Assessment(Ids.id("assess"), assessorId, targetRef, targetType, verdict, confidence, criteria, evidenceRefs, explanation, Ids.now()));
        candidates.get(targetRef).ifPresent(c -> candidates.update(c.withStatus(CandidateStatus.ASSESSED)));
        metrics.increment("layer.assess.total");
        return assessment;
    }

    public AdmissibilityDecision checkAdmissibility(String candidateId) {
        return candidates.get(candidateId)
                .map(c -> admissibility.check(c, assessments.forTarget(candidateId)))
                .orElseGet(() -> new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.INADMISSIBLE, "candidate not found", Map.of("candidateId", candidateId == null ? "" : candidateId)));
    }

    public Optional<Candidate> acceptCandidate(String candidateId) { return candidates.get(candidateId).map(c -> candidates.update(c.withStatus(CandidateStatus.ACCEPTED_BY_AGENT))); }
    public Optional<Candidate> rejectCandidate(String candidateId) { return candidates.get(candidateId).map(c -> candidates.update(c.withStatus(CandidateStatus.REJECTED_BY_AGENT))); }
    public Optional<ResourceResult> result(String resultId) { return results.get(resultId); }
    public Optional<Candidate> candidate(String candidateId) { return candidates.get(candidateId); }
    public Optional<Blob> blob(String blobId) { return blobs.get(blobId); }
    public Optional<TraceRecord> trace(String traceId) { return traces.get(traceId); }
    public boolean valid(String resultId) { return result(resultId).map(ResourceResult::success).orElse(false); }
    public String field(String resultId, String fieldName) { return result(resultId).map(ResourceResult::validation).map(ValidationResult::fields).map(f -> f.getOrDefault(fieldName, "")).orElse(""); }
    public List<TraceRecord> traces() { return traces.all(); }
    public List<String> metrics() { return metrics.events(); }

    private ResourceResult deniedResult(ResourceRequest request, PolicyDecision policy) {
        Outcomes.ResultOutcome outcome = policy.outcome() == Outcomes.PolicyOutcome.ESCALATE ? Outcomes.ResultOutcome.GOVERNANCE_ESCALATED : Outcomes.ResultOutcome.GOVERNANCE_DENIED;
        ValidationResult validation = ValidationResult.invalid(policy.reason(), List.of(policy.reason()));
        TraceRecord trace = recordTrace(request, "", "", "", outcome, policy, validation);
        metrics.increment("layer.invoke.denied");
        return results.put(new ResourceResult(Ids.id("res"), request.requestId(), outcome, "", "", "", trace.traceId(), validation, policy, policy.reason(), Ids.now()));
    }

    private TraceRecord recordTrace(ResourceRequest request, String promptBlobId, String outputBlobId, String candidateId, Outcomes.ResultOutcome outcome, PolicyDecision policy, ValidationResult validation) {
        return traces.record(new TraceRecord(Ids.id("trace"), request.requestId(), request.agentId(), request.goalId(), request.resourceId(), request.taskType(), promptBlobId, outputBlobId, candidateId, "", outcome, policy, null, validation, Ids.now()));
    }
}
