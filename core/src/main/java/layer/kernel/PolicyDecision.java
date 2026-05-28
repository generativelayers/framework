package layer.kernel;

import java.util.Map;

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
