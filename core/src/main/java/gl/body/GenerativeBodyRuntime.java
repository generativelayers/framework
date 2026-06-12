package gl.body;

import gl.model.CandidateType;
import gl.GovernanceKernel;
import java.util.List;
import java.util.Map;

/**
 * Factory for standard {@link GenerativeBodyRegistry} instances.
 *
 * <p>Creates {@link DefaultGenerativeBody} instances from a
 * {@link GovernanceKernel} and registers them in a {@link GenerativeBodyRegistry}.
 *
 * <p>In GL v2, provider binding goes through {@code bind() > call()}.
 * This class only provides the factory method used by tests and adapters.
 */
public final class GenerativeBodyRuntime {

    private GenerativeBodyRuntime() {}

    /** Create a standard registry with six pre-configured bodies for the given kernel. */
    public static GenerativeBodyRegistry createStandardRegistry(GovernanceKernel kernel) {
        GenerativeBodyRegistry registry = new GenerativeBodyRegistry();
        registry.register(new DefaultGenerativeBody(kernel, new BodyDescriptor("llm.answer", BodyKind.LLM, "General answer body", List.of(BodyAffordance.ANSWER, BodyAffordance.CLASSIFY, BodyAffordance.SUMMARISE), CandidateType.CANDIDATE_ANSWER, Map.of())));
        registry.register(new DefaultGenerativeBody(kernel, new BodyDescriptor("rag.ground", BodyKind.RAG, "Grounded fact body", List.of(BodyAffordance.GROUND_FACT, BodyAffordance.SUMMARISE, BodyAffordance.EXPLAIN), CandidateType.GROUNDED_FACT, Map.of())));
        registry.register(new DefaultGenerativeBody(kernel, new BodyDescriptor("planner.decompose", BodyKind.PLANNER, "Goal decomposition body", List.of(BodyAffordance.DECOMPOSE_GOAL), CandidateType.TASK_DECOMPOSITION, Map.of())));
        registry.register(new DefaultGenerativeBody(kernel, new BodyDescriptor("tool.propose", BodyKind.TOOL_PROPOSER, "Tool proposal body", List.of(BodyAffordance.PROPOSE_TOOL_CALL, BodyAffordance.PROPOSE_ACTION), CandidateType.TOOL_CALL_PROPOSAL, Map.of())));
        registry.register(new DefaultGenerativeBody(kernel, new BodyDescriptor("memory.retrieve", BodyKind.MEMORY, "Memory retrieval body", List.of(BodyAffordance.RETRIEVE_MEMORY), CandidateType.MEMORY_USE, Map.of())));
        registry.register(new DefaultGenerativeBody(kernel, new BodyDescriptor("reflect.critique", BodyKind.REFLECTOR, "Reflection and critique body", List.of(BodyAffordance.REFLECT, BodyAffordance.CRITIQUE, BodyAffordance.EXPLAIN), CandidateType.REFLECTION_NOTE, Map.of())));
        return registry;
    }
}
