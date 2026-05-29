package gl.body;

/** Kind of generative body: LLM (language model), TOOL (structured tool call),
 *  or HYBRID (both). */
public enum BodyKind {
    LLM, RAG, PLANNER, TOOL_PROPOSER, MEMORY, REFLECTOR, ASSESSOR, HUMAN_REVIEW, CUSTOM
}
