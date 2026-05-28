package layer.body;

import layer.kernel.ResourceResult;
import java.time.Instant;

public record InvocationResult(String bodyId, InvocationStatus status, ResourceResult resourceResult,
                               String candidateId, String outputBlobId, String traceId,
                               String message, Instant createdAt) {
    public InvocationResult {
        bodyId = bodyId == null ? "" : bodyId;
        status = status == null ? InvocationStatus.CREATED : status;
        candidateId = candidateId == null ? "" : candidateId;
        outputBlobId = outputBlobId == null ? "" : outputBlobId;
        traceId = traceId == null ? "" : traceId;
        message = message == null ? "" : message;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
