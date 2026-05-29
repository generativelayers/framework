package gl.model;

/** Lifecycle status of a {@link Candidate}: PROPOSED, VALIDATED, INVALID,
 *  ASSESSED, ACCEPTED_BY_AGENT, REJECTED_BY_AGENT. */
public enum CandidateStatus {
    PROPOSED,
    VALIDATED,
    INVALID,
    ASSESSED,
    ACCEPTED_BY_AGENT,
    REJECTED_BY_AGENT,
    ESCALATED
}
