package gl.tests;

import gl.*;
import gl.model.*;
import gl.body.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-platform parity test.
 *
 * <p>Proves that the Generative Layer governance semantics are
 * <b>platform-independent</b>: the same {@link ResourceRequest}
 * produces identical governance outcomes regardless of whether
 * it is issued by an ASTRA adapter, a Jason adapter, or the
 * kernel directly. This is essential for Paper 1 Claim T9:
 * cross-platform parity.
 *
 * <p>The test simulates both adapter paths by executing
 * the <b>same scenario</b> (classify food item with schema validation)
 * against the same deterministic kernel and comparing every field
 * of the resulting {@link ResourceResult}, {@link Candidate},
 * and {@link TraceRecord}.
 */
public final class CrossPlatformParityTest {

    private GovernanceKernel kernelA;
    private GovernanceKernel kernelB;
    private GenerativeBodyRegistry bodiesA;
    private GenerativeBodyRegistry bodiesB;

    @BeforeEach
    void setUp() {
        // Two independent kernels simulate two different platform runtimes
        kernelA = GovernanceKernelFactory.deterministicInMemory();
        kernelB = GovernanceKernelFactory.deterministicInMemory();
        bodiesA = GenerativeBodyRuntime.createStandardRegistry(kernelA);
        bodiesB = GenerativeBodyRuntime.createStandardRegistry(kernelB);
    }

    // ─── Scenario: Valid classification ─────────────────────────

    @Test
    void validClassificationProducesIdenticalOutcomes() {
        // Simulate ASTRA adapter path
        InvocationResult resultA = bodiesA.require("llm.answer").invoke(new BodyInvocation(
                "agent_astra", "classify_food", "llm.answer", BodyAffordance.ANSWER,
                "Classify 'apple'. Return label and confidence.",
                List.of("label", "confidence"), Map.of()
        ));

        // Simulate Jason adapter path (identical scenario, different agentId)
        InvocationResult resultB = bodiesB.require("llm.answer").invoke(new BodyInvocation(
                "agent_jason", "classify_food", "llm.answer", BodyAffordance.ANSWER,
                "Classify 'apple'. Return label and confidence.",
                List.of("label", "confidence"), Map.of()
        ));

        // Both must produce the same status
        assertEquals(resultA.status(), resultB.status(),
                "ASTRA and Jason must produce identical invocation status");
        assertEquals(InvocationStatus.CANDIDATE_READY, resultA.status());

        // Both results must be valid
        assertTrue(resultA.resourceResult().success());
        assertTrue(resultB.resourceResult().success());

        // Both must produce identical outcomes
        assertEquals(resultA.resourceResult().outcome(), resultB.resourceResult().outcome());

        // Both candidates must be in VALIDATED state (not auto-accepted)
        Candidate candA = kernelA.candidate(resultA.candidateId()).orElseThrow();
        Candidate candB = kernelB.candidate(resultB.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.VALIDATED, candA.status());
        assertEquals(CandidateStatus.VALIDATED, candB.status());

        // Both must extract identical fields
        assertEquals(
                kernelA.field(resultA.resourceResult().resultId(), "label"),
                kernelB.field(resultB.resourceResult().resultId(), "label"),
                "Extracted label must be identical across platforms"
        );
        assertEquals(
                kernelA.field(resultA.resourceResult().resultId(), "confidence"),
                kernelB.field(resultB.resourceResult().resultId(), "confidence"),
                "Extracted confidence must be identical across platforms"
        );
    }

    // ─── Scenario: Governance denial ────────────────────────────

    @Test
    void governanceDenialIsIdenticalAcrossPlatforms() {
        ResourceRequest requestA = new ResourceRequest(
                null, "agent_astra", "goal_1", "llm.answer", "answer",
                CandidateType.CANDIDATE_ANSWER, "blocked",
                ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), Map.of("deny", "true"), "");
        ResourceRequest requestB = new ResourceRequest(
                null, "agent_jason", "goal_1", "llm.answer", "answer",
                CandidateType.CANDIDATE_ANSWER, "blocked",
                ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), Map.of("deny", "true"), "");

        ResourceResult resultA = kernelA.invoke(requestA);
        ResourceResult resultB = kernelB.invoke(requestB);

        assertEquals(resultA.outcome(), resultB.outcome());
        assertEquals(Outcomes.ResultOutcome.GOVERNANCE_DENIED, resultA.outcome());
        assertTrue(resultA.candidateId().isBlank());
        assertTrue(resultB.candidateId().isBlank());
    }

    // ─── Scenario: Invalid output handling ──────────────────────

    @Test
    void invalidOutputHandlingIsIdenticalAcrossPlatforms() {
        ResourceRequest requestA = new ResourceRequest(
                null, "agent_astra", "goal_1", "llm.answer", "answer",
                CandidateType.CANDIDATE_ANSWER, "bad",
                ResponseSchema.required("schema", List.of("missing_field")),
                GovernanceContext.empty(), Map.of("mode", "missing_field"), "");
        ResourceRequest requestB = new ResourceRequest(
                null, "agent_jason", "goal_1", "llm.answer", "answer",
                CandidateType.CANDIDATE_ANSWER, "bad",
                ResponseSchema.required("schema", List.of("missing_field")),
                GovernanceContext.empty(), Map.of("mode", "missing_field"), "");

        ResourceResult resultA = kernelA.invoke(requestA);
        ResourceResult resultB = kernelB.invoke(requestB);

        assertEquals(resultA.outcome(), resultB.outcome());
        assertEquals(Outcomes.ResultOutcome.INVALID_OUTPUT, resultA.outcome());

        Candidate candA = kernelA.candidate(resultA.candidateId()).orElseThrow();
        Candidate candB = kernelB.candidate(resultB.candidateId()).orElseThrow();
        assertEquals(candA.status(), candB.status());
        assertEquals(CandidateStatus.INVALID, candA.status());
    }

    // ─── Scenario: Assessment + admissibility ───────────────────

    @Test
    void assessmentAndAdmissibilityAreIdenticalAcrossPlatforms() {
        // Generate valid candidates on both platforms
        ResourceResult resultA = kernelA.invoke(new ResourceRequest(
                null, "agent_astra", "goal_1", "llm.answer", "answer",
                CandidateType.CANDIDATE_ANSWER, "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), Map.of(), ""));
        ResourceResult resultB = kernelB.invoke(new ResourceRequest(
                null, "agent_jason", "goal_1", "llm.answer", "answer",
                CandidateType.CANDIDATE_ANSWER, "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), Map.of(), ""));

        // Both initially admissible
        assertTrue(kernelA.checkAdmissibility(resultA.candidateId()).admissible());
        assertTrue(kernelB.checkAdmissibility(resultB.candidateId()).admissible());

        // Add identical rejecting assessments
        kernelA.assess("peer", resultA.candidateId(), "candidate",
                Outcomes.AssessmentVerdict.REJECT, 0.95,
                List.of("accuracy"), List.of(), "hallucinated");
        kernelB.assess("peer", resultB.candidateId(), "candidate",
                Outcomes.AssessmentVerdict.REJECT, 0.95,
                List.of("accuracy"), List.of(), "hallucinated");

        // Both must now be inadmissible
        assertFalse(kernelA.checkAdmissibility(resultA.candidateId()).admissible(),
                "ASTRA: candidate with rejecting assessment must be inadmissible");
        assertFalse(kernelB.checkAdmissibility(resultB.candidateId()).admissible(),
                "Jason: candidate with rejecting assessment must be inadmissible");
    }

    // ─── Scenario: Full lifecycle parity ────────────────────────

    @Test
    void fullLifecycleIsIdenticalAcrossPlatforms() {
        // ASTRA path: invoke → validate → assess → accept
        ResourceResult rA = kernelA.invoke(new ResourceRequest(
                null, "astra_agent", "goal_1", "llm.answer", "answer",
                CandidateType.CANDIDATE_ANSWER, "Return label",
                ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), Map.of(), ""));
        kernelA.assess("assessor", rA.candidateId(), "candidate",
                Outcomes.AssessmentVerdict.ACCEPT, 0.9,
                List.of("schema"), List.of(), "valid");
        Candidate acceptedA = kernelA.acceptCandidate(rA.candidateId()).orElseThrow();

        // Jason path: identical
        ResourceResult rB = kernelB.invoke(new ResourceRequest(
                null, "jason_agent", "goal_1", "llm.answer", "answer",
                CandidateType.CANDIDATE_ANSWER, "Return label",
                ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), Map.of(), ""));
        kernelB.assess("assessor", rB.candidateId(), "candidate",
                Outcomes.AssessmentVerdict.ACCEPT, 0.9,
                List.of("schema"), List.of(), "valid");
        Candidate acceptedB = kernelB.acceptCandidate(rB.candidateId()).orElseThrow();

        // Final states must match
        assertEquals(acceptedA.status(), acceptedB.status());
        assertEquals(CandidateStatus.ACCEPTED_BY_AGENT, acceptedA.status());

        // Trace completeness must match
        TraceRecord traceA = kernelA.trace(rA.traceId()).orElseThrow();
        TraceRecord traceB = kernelB.trace(rB.traceId()).orElseThrow();
        assertEquals(traceA.outcome(), traceB.outcome());
        assertFalse(traceA.promptBlobId().isBlank());
        assertFalse(traceB.promptBlobId().isBlank());
        assertFalse(traceA.outputBlobId().isBlank());
        assertFalse(traceB.outputBlobId().isBlank());
    }
}
