package gl.body;

import gl.model.*;

/** The 13 affordance types a generative body can perform.
 *  Each affordance maps to a {@link CandidateType} via
 *  {@link DefaultGenerativeBody}. Affordances are the agent's
 *  declared capabilities for interacting with generative resources. */
public enum BodyAffordance {
    ANSWER,
    CLASSIFY,
    SUMMARISE,
    GROUND_FACT,
    DECOMPOSE_GOAL,
    PROPOSE_TOOL_CALL,
    PROPOSE_ACTION,
    RETRIEVE_MEMORY,
    REFLECT,
    CRITIQUE,
    ASSESS,
    EXPLAIN,
    ESCALATE
}
