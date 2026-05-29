package gl.body;

import gl.model.CandidateType;
import gl.GovernanceKernel;
import gl.GovernanceKernelFactory;
import gl.provider.ProviderConfig;
import gl.provider.ProviderRegistry;
import java.util.List;
import java.util.Map;

/**
 * Runtime wiring: creates {@link DefaultGenerativeBody} instances from a
 * {@link GovernanceKernel} and registers them in the {@link GenerativeBodyRegistry}.
 *
 * <p>Uses lazy initialization — the kernel and registry are created on first access,
 * not at class-load time. This avoids premature provider auto-detection.
 */
public final class GenerativeBodyRuntime {

    private static volatile GovernanceKernel lazyKernel;
    private static volatile GenerativeBodyRegistry lazyRegistry;

    private GenerativeBodyRuntime() {}

    /** Get the shared kernel, creating it lazily on first access. */
    public static GovernanceKernel kernel() {
        if (lazyKernel == null) {
            synchronized (GenerativeBodyRuntime.class) {
                if (lazyKernel == null) {
                    lazyKernel = GovernanceKernelFactory.withProvider(
                            ProviderRegistry.resolve(ProviderConfig.empty()));
                }
            }
        }
        return lazyKernel;
    }

    /** Get the shared registry, creating it lazily on first access. */
    public static GenerativeBodyRegistry registry() {
        if (lazyRegistry == null) {
            synchronized (GenerativeBodyRuntime.class) {
                if (lazyRegistry == null) {
                    lazyRegistry = createStandardRegistry(kernel());
                }
            }
        }
        return lazyRegistry;
    }

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
