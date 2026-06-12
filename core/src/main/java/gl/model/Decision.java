package gl.model;

import gl.Ids;

import java.time.Instant;

/**
 * A recorded accept/reject decision about candidate material.
 *
 * <p>Decisions are first-class governance objects, not just status flips.
 * Each decision stores the agent, reason, and timestamp for full auditability.
 *
 * <p>The {@code agentId} is obtained from the {@link Candidate} at decision time
 * (which received it from the provider binding during {@code call()}).
 */
public record Decision(
        String decisionId,
        String candidateId,
        String agentId,
        DecisionType type,
        String reason,
        Instant timestamp
) {
    public Decision {
        decisionId = decisionId == null || decisionId.isBlank() ? Ids.id("dec") : decisionId;
        candidateId = candidateId == null ? "" : candidateId;
        agentId = agentId == null ? "" : agentId;
        type = type == null ? DecisionType.REJECTED : type;
        reason = reason == null ? "" : reason;
        timestamp = timestamp == null ? Ids.now() : timestamp;
    }
}
