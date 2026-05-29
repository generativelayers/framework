package gl.model;

import gl.Outcomes;

import java.util.Map;

/** Result of a pre-generation governance policy evaluation.
 *  Determines whether a generation request is allowed, denied, or escalated.
 *  @param allowed whether generation may proceed
 *  @param reason human-readable explanation
 *  @param outcome the policy outcome (ALLOW/DENY/ESCALATE) */
public record PolicyDecision(Outcomes.PolicyOutcome outcome, String reason, Map<String, String> metadata) {
    public PolicyDecision {
        outcome = outcome == null ? Outcomes.PolicyOutcome.DENY : outcome;
        reason = reason == null ? "" : reason;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean allowed() {
        return outcome == Outcomes.PolicyOutcome.ALLOW;
    }
}
