package gl.model;

import gl.Outcomes;
import gl.Ids;

import java.time.Instant;

/** The kernel's response to a {@link ResourceRequest}.
 *  Contains the outcome, prompt/output blob IDs, candidate ID, trace ID,
 *  validation result, policy decision, and timestamp. */
public record ResourceResult(
        String resultId,
        String requestId,
        Outcomes.ResultOutcome outcome,
        String promptBlobId,
        String outputBlobId,
        String candidateId,
        String traceId,
        ValidationResult validation,
        PolicyDecision policyDecision,
        String message,
        Instant createdAt
) {
    public ResourceResult {
        resultId = resultId == null || resultId.isBlank() ? Ids.id("res") : resultId;
        requestId = requestId == null ? "" : requestId;
        outcome = outcome == null ? Outcomes.ResultOutcome.STORED_ONLY : outcome;
        promptBlobId = promptBlobId == null ? "" : promptBlobId;
        outputBlobId = outputBlobId == null ? "" : outputBlobId;
        candidateId = candidateId == null ? "" : candidateId;
        traceId = traceId == null ? "" : traceId;
        message = message == null ? "" : message;
        createdAt = createdAt == null ? Ids.now() : createdAt;
    }

    public boolean success() {
        return outcome == Outcomes.ResultOutcome.SUCCESS;
    }
}
