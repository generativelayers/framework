package gl.body;

import gl.kernel.CandidateType;
import gl.kernel.GovernanceKernel;
import gl.kernel.GovernanceKernelFactory;
import gl.provider.ProviderConfig;
import gl.provider.ProviderRegistry;
import java.util.List;
import java.util.Map;

public final class GenerativeBodyRuntime {
    private static final GovernanceKernel KERNEL = GovernanceKernelFactory.withProvider(ProviderRegistry.resolve(ProviderConfig.empty()));
    private static final GenerativeBodyRegistry REGISTRY = createStandardRegistry(KERNEL);

    private GenerativeBodyRuntime() {}

    public static GovernanceKernel kernel() { return KERNEL; }
    public static GenerativeBodyRegistry registry() { return REGISTRY; }

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
