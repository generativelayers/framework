package gl.body;

/** Kind of generative body. Nine body kinds classify the role of the
 *  external resource: {@code LLM}, {@code RAG}, {@code PLANNER},
 *  {@code TOOL_PROPOSER}, {@code MEMORY}, {@code REFLECTOR},
 *  {@code ASSESSOR}, {@code HUMAN_REVIEW}, and {@code CUSTOM}. */
public enum BodyKind {
    LLM, RAG, PLANNER, TOOL_PROPOSER, MEMORY, REFLECTOR, ASSESSOR, HUMAN_REVIEW, CUSTOM
}
