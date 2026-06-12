package gl.model;

/**
 * Type of a final agent decision about candidate material.
 *
 * <p>These are <b>decision outcomes</b>, distinct from judge verdicts
 * ({@code APPROVE, WARN, REJECT_VERDICT, UNCERTAIN}).
 * A judge verdict is evaluative evidence; a decision is a final agent action.
 */
public enum DecisionType {
    ACCEPTED,
    REJECTED
}
