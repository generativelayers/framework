package gl.tests;

import gl.body.*;
import gl.kernel.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Generative Layer GovernanceKernel tests.
 *
 * These tests use the DeterministicFakeProvider and in-memory stores to prove
 * the core Generative Layer semantic claims:
 *
 * 1. Generated material is candidate material, not automatic belief.
 * 2. Invalid output is rejected before reaching candidate status.
 * 3. Governance denial prevents provider invocation.
 * 4. Assessments are first-class traceable artefacts.
 * 5. Admissibility checks prevent action on unvalidated candidates.
 * 6. Traces record the full lifecycle.
 * 7. Explicit adoption by the BDI agent is required.
 */
final class KernelTest {

    // ─── Valid candidate flow ────────────────────────────────────────────

    @Test
    void validCandidateIsCreatedButNotAutomaticallyAccepted() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        GenerativeBodyRegistry registry = GenerativeBodyRuntime.createStandardRegistry(GovernanceKernel);
        InvocationResult invocation = registry.require("llm.answer").invoke(new BodyInvocation(
                "agent_a", "goal_1", "llm.answer", BodyAffordance.ANSWER,
                "Return label and confidence", List.of("label", "confidence"), Map.of()
        ));

        assertEquals(InvocationStatus.CANDIDATE_READY, invocation.status());
        assertTrue(invocation.resourceResult().success());
        Candidate candidate = GovernanceKernel.candidate(invocation.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.VALIDATED, candidate.status(),
                "Candidate must be VALIDATED, not ACCEPTED — adoption is the agent's decision");
    }

    // ─── Governance ─────────────────────────────────────────────────────

    @Test
    void governanceDenialDoesNotCreateCandidate() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "blocked", ResponseSchema.required("schema", List.of("label")), GovernanceContext.empty(), null,
                Map.of("deny", "true")
        ));

        assertEquals(Outcomes.ResultOutcome.GOVERNANCE_DENIED, result.outcome());
        assertTrue(result.candidateId().isBlank(), "Denied request must not create a candidate");
        assertFalse(GovernanceKernel.valid(result.resultId()));
    }

    @Test
    void governanceEscalationDoesNotCreateCandidate() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "escalated", ResponseSchema.required("schema", List.of("label")), GovernanceContext.empty(), null,
                Map.of("escalate", "true")
        ));

        assertEquals(Outcomes.ResultOutcome.GOVERNANCE_ESCALATED, result.outcome());
        assertTrue(result.candidateId().isBlank(), "Escalated request must not create a candidate");
    }

    // ─── Validation ─────────────────────────────────────────────────────

    @Test
    void invalidOutputIsRejectedBySchema() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "bad", ResponseSchema.required("schema", List.of("missing")), GovernanceContext.empty(), null,
                Map.of("mode", "missing_field")
        ));

        assertEquals(Outcomes.ResultOutcome.INVALID_OUTPUT, result.outcome());
        Candidate candidate = GovernanceKernel.candidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.INVALID, candidate.status());
    }

    @Test
    void fieldExtractionFromValidResult() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));

        assertTrue(result.success());
        assertEquals("test", GovernanceKernel.field(result.resultId(), "label"));
        assertEquals("1.0", GovernanceKernel.field(result.resultId(), "confidence"));
        assertEquals("", GovernanceKernel.field(result.resultId(), "nonexistent"),
                "Missing field must return empty string, not throw");
    }

    // ─── Provider failure ───────────────────────────────────────────────

    @Test
    void providerFailureReturnsFailedOutcome() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "fail", ResponseSchema.required("schema", List.of("label")), GovernanceContext.empty(), null,
                Map.of("mode", "fail")
        ));

        assertEquals(Outcomes.ResultOutcome.PROVIDER_FAILED, result.outcome());
        assertFalse(result.success());
        assertFalse(GovernanceKernel.valid(result.resultId()));
    }

    // ─── Assessment ─────────────────────────────────────────────────────

    @Test
    void assessmentMarksCandidateAssessed() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence", ResponseSchema.required("schema", List.of("label", "confidence")), GovernanceContext.empty(), null,
                Map.of()
        ));
        String candidateId = result.candidateId();
        GovernanceKernel.assess("agent_b", candidateId, "candidate", Outcomes.AssessmentVerdict.ACCEPT, 0.9, List.of("schema"), List.of(result.outputBlobId()), "acceptable");
        assertEquals(CandidateStatus.ASSESSED, GovernanceKernel.candidate(candidateId).orElseThrow().status());
    }

    @Test
    void multipleAssessmentsAreStoredForSameCandidate() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));
        String candidateId = result.candidateId();

        Assessment a1 = GovernanceKernel.assess("agent_b", candidateId, "candidate",
                Outcomes.AssessmentVerdict.ACCEPT, 0.9, List.of("schema"), List.of(), "good");
        Assessment a2 = GovernanceKernel.assess("agent_c", candidateId, "candidate",
                Outcomes.AssessmentVerdict.REJECT, 0.8, List.of("accuracy"), List.of(), "bad grounding");

        assertNotEquals(a1.assessmentId(), a2.assessmentId());
        assertEquals("agent_b", a1.assessorId());
        assertEquals("agent_c", a2.assessorId());
    }

    // ─── Explicit accept / reject ───────────────────────────────────────

    @Test
    void explicitCandidateAcceptChangesStatus() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));

        Candidate accepted = GovernanceKernel.acceptCandidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.ACCEPTED_BY_AGENT, accepted.status(),
                "Explicit acceptance must move candidate to ACCEPTED_BY_AGENT");
    }

    @Test
    void explicitCandidateRejectChangesStatus() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));

        Candidate rejected = GovernanceKernel.rejectCandidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.REJECTED_BY_AGENT, rejected.status(),
                "Explicit rejection must move candidate to REJECTED_BY_AGENT");
    }

    // ─── Admissibility ──────────────────────────────────────────────────

    @Test
    void admissibilityDeniesProposedOnlyCandidates() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        // Create an INVALID candidate via missing field
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "bad", ResponseSchema.required("schema", List.of("missing")), GovernanceContext.empty(), null,
                Map.of("mode", "missing_field")
        ));

        AdmissibilityDecision decision = GovernanceKernel.checkAdmissibility(result.candidateId());
        assertFalse(decision.admissible(),
                "Invalid candidate must not be admissible");
        assertEquals(Outcomes.AdmissibilityOutcome.INADMISSIBLE, decision.outcome());
    }

    @Test
    void admissibilityDeniesAfterRejectingAssessment() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));
        String candidateId = result.candidateId();

        // Initially admissible (validated)
        assertTrue(GovernanceKernel.checkAdmissibility(candidateId).admissible());

        // Add rejecting assessment with high confidence
        GovernanceKernel.assess("validator", candidateId, "candidate",
                Outcomes.AssessmentVerdict.REJECT, 0.95, List.of("accuracy"), List.of(), "factually wrong");

        // Now must be inadmissible
        assertFalse(GovernanceKernel.checkAdmissibility(candidateId).admissible(),
                "Candidate with high-confidence REJECT assessment must be inadmissible");
    }

    // ─── Trace and blob retrieval ───────────────────────────────────────

    @Test
    void traceIsRecordedForEveryInvocation() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of()
        ));

        assertFalse(result.traceId().isBlank(), "Every invocation must produce a trace");
        TraceRecord trace = GovernanceKernel.trace(result.traceId()).orElseThrow();
        assertEquals("agent_a", trace.agentId());
        assertEquals("goal_1", trace.goalId());
        assertEquals(Outcomes.ResultOutcome.SUCCESS, trace.outcome());
        assertFalse(trace.promptBlobId().isBlank());
        assertFalse(trace.outputBlobId().isBlank());
    }

    @Test
    void blobsStorePromptAndOutput() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();
        ResourceResult result = GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label",
                ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of()
        ));

        assertFalse(result.promptBlobId().isBlank());
        assertFalse(result.outputBlobId().isBlank());

        Blob promptBlob = GovernanceKernel.blob(result.promptBlobId()).orElseThrow();
        assertEquals(BlobType.PROMPT, promptBlob.type());
        assertEquals("Return label", promptBlob.content());

        Blob outputBlob = GovernanceKernel.blob(result.outputBlobId()).orElseThrow();
        assertEquals(BlobType.GENERATIVE_OUTPUT, outputBlob.type());
        assertTrue(outputBlob.content().contains("label=test"));
    }

    // ─── Metrics ────────────────────────────────────────────────────────

    @Test
    void metricsTrackInvocationOutcomes() {
        GovernanceKernel GovernanceKernel = GovernanceKernelFactory.deterministicInMemory();

        // One successful invocation
        GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of()
        ));

        // One denied invocation
        GovernanceKernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "blocked", ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of("deny", "true")
        ));

        List<String> metrics = GovernanceKernel.metrics();
        assertTrue(metrics.stream().anyMatch(m -> m.contains("gl.invoke.total")), "Must track total invocations");
        assertTrue(metrics.stream().anyMatch(m -> m.contains("gl.invoke.success")), "Must track successful invocations");
        assertTrue(metrics.stream().anyMatch(m -> m.contains("gl.invoke.denied")), "Must track denied invocations");
    }
}
