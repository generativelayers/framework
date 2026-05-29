package gl.model;

import gl.Outcomes;

import java.util.Map;

/** Admissibility gate result: whether a candidate may be accepted by the agent.
 *  @param outcome ADMISSIBLE or INADMISSIBLE
 *  @param reason human-readable explanation
 *  @param details additional context (e.g. failing criteria) */
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
