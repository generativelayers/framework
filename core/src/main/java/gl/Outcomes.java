package gl;

/**
 * Enum container for all outcome types used across the framework.
 *
 * <p>Groups four related enums under one namespace:
 * {@link PolicyOutcome}, {@link AdmissibilityOutcome},
 * {@link ResultOutcome}, and {@link AssessmentVerdict}.
 */
public final class Outcomes {
    private Outcomes() {}

    /** Outcome of a pre-generation governance policy evaluation. */
    public enum PolicyOutcome { ALLOW, DENY, ESCALATE }

    /** Outcome of a candidate admissibility check. */
    public enum AdmissibilityOutcome { ADMISSIBLE, INADMISSIBLE, ESCALATE }

    /** Outcome of a generation invocation through the kernel. */
    public enum ResultOutcome { SUCCESS, GOVERNANCE_DENIED, GOVERNANCE_ESCALATED, PROVIDER_FAILED, INVALID_OUTPUT, STORED_ONLY }

    /** Verdict rendered by a peer assessor on a candidate. */
    public enum AssessmentVerdict { ACCEPT, REJECT, UNCERTAIN, NEEDS_EVIDENCE, NEEDS_HUMAN, RETRY }
}
