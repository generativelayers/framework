package gl.kernel;

public final class Outcomes {
    private Outcomes() {}

    public enum PolicyOutcome { ALLOW, DENY, ESCALATE }
    public enum AdmissibilityOutcome { ADMISSIBLE, INADMISSIBLE, ESCALATE }
    public enum ResultOutcome { SUCCESS, GOVERNANCE_DENIED, GOVERNANCE_ESCALATED, PROVIDER_FAILED, INVALID_OUTPUT, STORED_ONLY }
    public enum AssessmentVerdict { ACCEPT, REJECT, UNCERTAIN, NEEDS_EVIDENCE, NEEDS_HUMAN, RETRY }
}
