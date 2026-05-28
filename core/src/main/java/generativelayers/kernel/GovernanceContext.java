package generativelayers.kernel;

import java.util.Map;

public record GovernanceContext(String policyId, Map<String, String> attributes) {
    public GovernanceContext {
        policyId = policyId == null || policyId.isBlank() ? "default" : policyId;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
    public static GovernanceContext empty() { return new GovernanceContext("default", Map.of()); }
}
