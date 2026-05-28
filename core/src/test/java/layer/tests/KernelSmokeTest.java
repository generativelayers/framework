package layer.tests;

import layer.body.*;
import layer.kernel.*;
import java.util.List;
import java.util.Map;

public final class KernelSmokeTest {
    private KernelSmokeTest() {}

    public static void main(String[] args) {
        Kernel kernel = KernelFactory.deterministicInMemory();
        GenerativeBodyRegistry registry = GenerativeBodyRuntime.createStandardRegistry(kernel);
        BodyInvocation invocation = new BodyInvocation("tester", "g1", "llm.answer", BodyAffordance.ANSWER, "Return label and confidence", List.of("label", "confidence"), Map.of());
        InvocationResult result = registry.require("llm.answer").invoke(invocation);
        if (result.resourceResult() == null || !result.resourceResult().success()) throw new IllegalStateException("Generative Layer smoke test failed");
        if (result.candidateId().isBlank()) throw new IllegalStateException("candidate missing");
        System.out.println("Generative Layer smoke test passed: " + result.candidateId());
    }
}
