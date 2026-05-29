package gl.tests;

import gl.*;
import gl.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for retry/recovery when generation produces invalid output.
 */
class RetryRecoveryTest {

    /** Failing provider that succeeds on the Nth attempt. */
    static class FailNTimesProvider implements KernelPorts.GenerativeProvider {
        private final int failCount;
        private int calls = 0;

        FailNTimesProvider(int failCount) { this.failCount = failCount; }

        @Override
        public ProviderOutput generate(ResourceRequest request, Blob promptBlob) throws Exception {
            calls++;
            if (calls <= failCount) {
                throw new RuntimeException("transient error #" + calls);
            }
            return new ProviderOutput("test", "test", "label=test\nconfidence=0.95", java.util.Map.of());
        }

        int callCount() { return calls; }
    }

    @Test
    void noRetryPolicyFailsImmediately() {
        var provider = new FailNTimesProvider(1);
        GovernanceKernel kernel = GovernanceKernelFactory.builder(provider)
                .withRetryPolicy(RetryPolicy.none())
                .build();

        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "prompt", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), java.util.Map.of(), ""));

        assertEquals(Outcomes.ResultOutcome.PROVIDER_FAILED, result.outcome());
        assertEquals(1, provider.callCount());
    }

    @Test
    void retrySucceedsAfterTransientFailure() {
        var provider = new FailNTimesProvider(2);
        GovernanceKernel kernel = GovernanceKernelFactory.builder(provider)
                .withRetryPolicy(RetryPolicy.withRetries(3))
                .build();

        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "prompt", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), java.util.Map.of(), ""));

        assertEquals(Outcomes.ResultOutcome.SUCCESS, result.outcome());
        assertEquals(3, provider.callCount()); // 2 failures + 1 success
    }

    @Test
    void retryExhaustsAllAttemptsAndFails() {
        var provider = new FailNTimesProvider(5);
        GovernanceKernel kernel = GovernanceKernelFactory.builder(provider)
                .withRetryPolicy(RetryPolicy.withRetries(3))
                .build();

        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "prompt", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), java.util.Map.of(), ""));

        assertEquals(Outcomes.ResultOutcome.PROVIDER_FAILED, result.outcome());
        assertEquals(3, provider.callCount());
    }

    @Test
    void retryOnInvalidOutputRevalidates() {
        // Provider that returns invalid output first, then valid
        AtomicInteger calls = new AtomicInteger(0);
        KernelPorts.GenerativeProvider provider = (req, blob) -> {
            int n = calls.incrementAndGet();
            if (n == 1) return new ProviderOutput("t", "t", "garbage output", java.util.Map.of());
            return new ProviderOutput("t", "t", "label=test\nconfidence=0.9", java.util.Map.of());
        };

        GovernanceKernel kernel = GovernanceKernelFactory.builder(provider)
                .withRetryPolicy(RetryPolicy.withRetries(3))
                .build();

        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "prompt", ResponseSchema.required("s", List.of("label", "confidence")),
                GovernanceContext.empty(), java.util.Map.of(), ""));

        assertEquals(Outcomes.ResultOutcome.SUCCESS, result.outcome());
        assertEquals(2, calls.get());
    }

    @Test
    void retryPolicyMinimumIsOne() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(0, false));
    }
}
