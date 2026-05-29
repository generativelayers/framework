package gl.tests;

import gl.*;
import gl.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for kernel lifecycle event hooks.
 */
class KernelListenerTest {

    /** Listener that records all events. */
    static class RecordingListener implements KernelListener {
        final List<String> events = new ArrayList<>();

        @Override public void onPolicyDenied(ResourceRequest request, PolicyDecision decision) {
            events.add("POLICY_DENIED:" + decision.reason());
        }
        @Override public void onProviderFailed(ResourceRequest request, Exception error) {
            events.add("PROVIDER_FAILED:" + error.getMessage());
        }
        @Override public void onValidationFailed(ResourceRequest request, ValidationResult result) {
            events.add("VALIDATION_FAILED:" + result.message());
        }
        @Override public void onCandidateCreated(Candidate candidate) {
            events.add("CANDIDATE_CREATED:" + candidate.candidateId());
        }
        @Override public void onCandidateAccepted(Candidate candidate) {
            events.add("CANDIDATE_ACCEPTED:" + candidate.candidateId());
        }
        @Override public void onCandidateRejected(Candidate candidate) {
            events.add("CANDIDATE_REJECTED:" + candidate.candidateId());
        }
        @Override public void onRetry(ResourceRequest request, int attempt, String reason) {
            events.add("RETRY:" + attempt + ":" + reason);
        }
    }

    @Test
    void listenerFiresOnSuccessfulInvocation() {
        var listener = new RecordingListener();
        GovernanceKernel kernel = GovernanceKernelFactory.builder(new KernelDefaults.DeterministicFakeProvider())
                .withListener(listener)
                .build();

        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), Map.of(), ""));

        assertEquals(Outcomes.ResultOutcome.SUCCESS, result.outcome());
        assertTrue(listener.events.stream().anyMatch(e -> e.startsWith("CANDIDATE_CREATED:")));
        assertFalse(listener.events.stream().anyMatch(e -> e.startsWith("POLICY_DENIED:")));
    }

    @Test
    void listenerFiresOnPolicyDenial() {
        var listener = new RecordingListener();
        GovernanceKernel kernel = GovernanceKernelFactory.builder(new KernelDefaults.DeterministicFakeProvider())
                .withListener(listener)
                .build();

        kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "blocked", ResponseSchema.freeText(),
                GovernanceContext.empty(), Map.of("deny", "true"), ""));

        assertTrue(listener.events.stream().anyMatch(e -> e.startsWith("POLICY_DENIED:")));
    }

    @Test
    void listenerFiresOnAcceptAndReject() {
        var listener = new RecordingListener();
        GovernanceKernel kernel = GovernanceKernelFactory.builder(new KernelDefaults.DeterministicFakeProvider())
                .withListener(listener)
                .build();

        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), Map.of(), ""));

        kernel.acceptCandidate(result.candidateId());
        assertTrue(listener.events.stream().anyMatch(e -> e.startsWith("CANDIDATE_ACCEPTED:")));

        // Second invocation to test reject
        ResourceResult r2 = kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), Map.of(), ""));
        kernel.rejectCandidate(r2.candidateId());
        assertTrue(listener.events.stream().anyMatch(e -> e.startsWith("CANDIDATE_REJECTED:")));
    }

    @Test
    void listenerFiresRetryEvents() {
        var listener = new RecordingListener();
        var calls = new java.util.concurrent.atomic.AtomicInteger(0);
        KernelPorts.GenerativeProvider failOnce = (req, blob) -> {
            if (calls.incrementAndGet() == 1) throw new RuntimeException("transient");
            return new ProviderOutput("t", "t", "label=ok", Map.of());
        };

        GovernanceKernel kernel = GovernanceKernelFactory.builder(failOnce)
                .withRetryPolicy(RetryPolicy.withRetries(3))
                .withListener(listener)
                .build();

        kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "prompt", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), Map.of(), ""));

        assertTrue(listener.events.stream().anyMatch(e -> e.startsWith("RETRY:1:")));
        assertTrue(listener.events.stream().anyMatch(e -> e.startsWith("PROVIDER_FAILED:")));
    }

    @Test
    void listenerExceptionDoesNotBreakPipeline() {
        KernelListener badListener = new KernelListener() {
            @Override public void onCandidateCreated(Candidate c) {
                throw new RuntimeException("listener crash!");
            }
        };

        GovernanceKernel kernel = GovernanceKernelFactory.builder(new KernelDefaults.DeterministicFakeProvider())
                .withListener(badListener)
                .build();

        // Should NOT throw — listener errors are swallowed
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), Map.of(), ""));

        assertEquals(Outcomes.ResultOutcome.SUCCESS, result.outcome());
    }
}
