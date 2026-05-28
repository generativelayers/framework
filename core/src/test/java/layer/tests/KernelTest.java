package layer.tests;

import layer.body.*;
import layer.kernel.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Generative Layer kernel tests.
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
        Kernel kernel = KernelFactory.deterministicInMemory();
        GenerativeBodyRegistry registry = GenerativeBodyRuntime.createStandardRegistry(kernel);
        InvocationResult invocation = registry.require("llm.answer").invoke(new BodyInvocation(
                "agent_a", "goal_1", "llm.answer", BodyAffordance.ANSWER,
                "Return label and confidence", List.of("label", "confidence"), Map.of()
        ));

        assertEquals(InvocationStatus.CANDIDATE_READY, invocation.status());
        assertTrue(invocation.resourceResult().success());
        Candidate candidate = kernel.candidate(invocation.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.VALIDATED, candidate.status(),
                "Candidate must be VALIDATED, not ACCEPTED — adoption is the agent's decision");
    }

    // ─── Governance ─────────────────────────────────────────────────────

    @Test
    void governanceDenialDoesNotCreateCandidate() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "blocked", ResponseSchema.required("schema", List.of("label")), GovernanceContext.empty(), null,
                Map.of("deny", "true")
        ));

        assertEquals(Outcomes.ResultOutcome.GOVERNANCE_DENIED, result.outcome());
        assertTrue(result.candidateId().isBlank(), "Denied request must not create a candidate");
        assertFalse(kernel.valid(result.resultId()));
    }

    @Test
    void governanceEscalationDoesNotCreateCandidate() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
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
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "bad", ResponseSchema.required("schema", List.of("missing")), GovernanceContext.empty(), null,
                Map.of("mode", "missing_field")
        ));

        assertEquals(Outcomes.ResultOutcome.INVALID_OUTPUT, result.outcome());
        Candidate candidate = kernel.candidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.INVALID, candidate.status());
    }

    @Test
    void fieldExtractionFromValidResult() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));

        assertTrue(result.success());
        assertEquals("test", kernel.field(result.resultId(), "label"));
        assertEquals("1.0", kernel.field(result.resultId(), "confidence"));
        assertEquals("", kernel.field(result.resultId(), "nonexistent"),
                "Missing field must return empty string, not throw");
    }

    // ─── Provider failure ───────────────────────────────────────────────

    @Test
    void providerFailureReturnsFailedOutcome() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "fail", ResponseSchema.required("schema", List.of("label")), GovernanceContext.empty(), null,
                Map.of("mode", "fail")
        ));

        assertEquals(Outcomes.ResultOutcome.PROVIDER_FAILED, result.outcome());
        assertFalse(result.success());
        assertFalse(kernel.valid(result.resultId()));
    }

    // ─── Assessment ─────────────────────────────────────────────────────

    @Test
    void assessmentMarksCandidateAssessed() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence", ResponseSchema.required("schema", List.of("label", "confidence")), GovernanceContext.empty(), null,
                Map.of()
        ));
        String candidateId = result.candidateId();
        kernel.assess("agent_b", candidateId, "candidate", Outcomes.AssessmentVerdict.ACCEPT, 0.9, List.of("schema"), List.of(result.outputBlobId()), "acceptable");
        assertEquals(CandidateStatus.ASSESSED, kernel.candidate(candidateId).orElseThrow().status());
    }

    @Test
    void multipleAssessmentsAreStoredForSameCandidate() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));
        String candidateId = result.candidateId();

        Assessment a1 = kernel.assess("agent_b", candidateId, "candidate",
                Outcomes.AssessmentVerdict.ACCEPT, 0.9, List.of("schema"), List.of(), "good");
        Assessment a2 = kernel.assess("agent_c", candidateId, "candidate",
                Outcomes.AssessmentVerdict.REJECT, 0.8, List.of("accuracy"), List.of(), "bad grounding");

        assertNotEquals(a1.assessmentId(), a2.assessmentId());
        assertEquals("agent_b", a1.assessorId());
        assertEquals("agent_c", a2.assessorId());
    }

    // ─── Explicit accept / reject ───────────────────────────────────────

    @Test
    void explicitCandidateAcceptChangesStatus() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));

        Candidate accepted = kernel.acceptCandidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.ACCEPTED_BY_AGENT, accepted.status(),
                "Explicit acceptance must move candidate to ACCEPTED_BY_AGENT");
    }

    @Test
    void explicitCandidateRejectChangesStatus() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));

        Candidate rejected = kernel.rejectCandidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.REJECTED_BY_AGENT, rejected.status(),
                "Explicit rejection must move candidate to REJECTED_BY_AGENT");
    }

    // ─── Admissibility ──────────────────────────────────────────────────

    @Test
    void admissibilityDeniesProposedOnlyCandidates() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        // Create an INVALID candidate via missing field
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "bad", ResponseSchema.required("schema", List.of("missing")), GovernanceContext.empty(), null,
                Map.of("mode", "missing_field")
        ));

        AdmissibilityDecision decision = kernel.checkAdmissibility(result.candidateId());
        assertFalse(decision.admissible(),
                "Invalid candidate must not be admissible");
        assertEquals(Outcomes.AdmissibilityOutcome.INADMISSIBLE, decision.outcome());
    }

    @Test
    void admissibilityDeniesAfterRejectingAssessment() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));
        String candidateId = result.candidateId();

        // Initially admissible (validated)
        assertTrue(kernel.checkAdmissibility(candidateId).admissible());

        // Add rejecting assessment with high confidence
        kernel.assess("validator", candidateId, "candidate",
                Outcomes.AssessmentVerdict.REJECT, 0.95, List.of("accuracy"), List.of(), "factually wrong");

        // Now must be inadmissible
        assertFalse(kernel.checkAdmissibility(candidateId).admissible(),
                "Candidate with high-confidence REJECT assessment must be inadmissible");
    }

    // ─── Trace and blob retrieval ───────────────────────────────────────

    @Test
    void traceIsRecordedForEveryInvocation() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of()
        ));

        assertFalse(result.traceId().isBlank(), "Every invocation must produce a trace");
        TraceRecord trace = kernel.trace(result.traceId()).orElseThrow();
        assertEquals("agent_a", trace.agentId());
        assertEquals("goal_1", trace.goalId());
        assertEquals(Outcomes.ResultOutcome.SUCCESS, trace.outcome());
        assertFalse(trace.promptBlobId().isBlank());
        assertFalse(trace.outputBlobId().isBlank());
    }

    @Test
    void blobsStorePromptAndOutput() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label",
                ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of()
        ));

        assertFalse(result.promptBlobId().isBlank());
        assertFalse(result.outputBlobId().isBlank());

        Blob promptBlob = kernel.blob(result.promptBlobId()).orElseThrow();
        assertEquals(BlobType.PROMPT, promptBlob.type());
        assertEquals("Return label", promptBlob.content());

        Blob outputBlob = kernel.blob(result.outputBlobId()).orElseThrow();
        assertEquals(BlobType.GENERATIVE_OUTPUT, outputBlob.type());
        assertTrue(outputBlob.content().contains("label=test"));
    }

    // ─── Metrics ────────────────────────────────────────────────────────

    @Test
    void metricsTrackInvocationOutcomes() {
        Kernel kernel = KernelFactory.deterministicInMemory();

        // One successful invocation
        kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of()
        ));

        // One denied invocation
        kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "blocked", ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of("deny", "true")
        ));

        List<String> metrics = kernel.metrics();
        assertTrue(metrics.stream().anyMatch(m -> m.contains("layer.invoke.total")), "Must track total invocations");
        assertTrue(metrics.stream().anyMatch(m -> m.contains("layer.invoke.success")), "Must track successful invocations");
        assertTrue(metrics.stream().anyMatch(m -> m.contains("layer.invoke.denied")), "Must track denied invocations");
    }
}
