package gl.GovernanceKernel;

import java.util.Map;

public record AdmissibilityDecision(Outcomes.AdmissibilityOutcome outcome, String reason, Map<String, String> metadata) {
    public AdmissibilityDecision {
        outcome = outcome == null ? Outcomes.AdmissibilityOutcome.INADMISSIBLE : outcome;
        reason = reason == null ? "" : reason;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean admissible() {
        return outcome == Outcomes.AdmissibilityOutcome.ADMISSIBLE;
    }
}
