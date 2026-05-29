package gl.model;

import java.util.Map;

/** Contextual metadata passed to governance policies during evaluation.
 *  Carries agent-level and goal-level context that policies may use
 *  to make allow/deny/escalate decisions. */
public record GovernanceContext(String policyId, Map<String, String> attributes) {
    public GovernanceContext {
        policyId = policyId == null || policyId.isBlank() ? "default" : policyId;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
    public static GovernanceContext empty() { return new GovernanceContext("default", Map.of()); }
}
