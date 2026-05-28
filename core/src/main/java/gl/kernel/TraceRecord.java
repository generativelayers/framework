package gl.kernel;

import java.time.Instant;

public record TraceRecord(
        String traceId,
        String requestId,
        String agentId,
        String goalId,
        String resourceId,
        String taskType,
        String promptBlobId,
        String outputBlobId,
        String candidateId,
        String assessmentId,
        Outcomes.ResultOutcome outcome,
        PolicyDecision policyDecision,
        AdmissibilityDecision admissibilityDecision,
        ValidationResult validationResult,
        Instant createdAt
) {
    public TraceRecord {
        traceId = traceId == null || traceId.isBlank() ? Ids.id("trace") : traceId;
        requestId = requestId == null ? "" : requestId;
        agentId = agentId == null ? "" : agentId;
        goalId = goalId == null ? "" : goalId;
        resourceId = resourceId == null ? "" : resourceId;
        taskType = taskType == null ? "" : taskType;
        promptBlobId = promptBlobId == null ? "" : promptBlobId;
        outputBlobId = outputBlobId == null ? "" : outputBlobId;
        candidateId = candidateId == null ? "" : candidateId;
        assessmentId = assessmentId == null ? "" : assessmentId;
        outcome = outcome == null ? Outcomes.ResultOutcome.STORED_ONLY : outcome;
        createdAt = createdAt == null ? Ids.now() : createdAt;
    }
}
