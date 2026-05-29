package gl.model;

/** Semantic type of a {@link Candidate}, determined by the invoking affordance. */
public enum CandidateType {
    CANDIDATE_ANSWER,
    TASK_DECOMPOSITION,
    ACTION_PROPOSAL,
    TOOL_CALL_PROPOSAL,
    GROUNDED_FACT,
    MEMORY_USE,
    REFLECTION_NOTE,
    EXPLANATION
}
