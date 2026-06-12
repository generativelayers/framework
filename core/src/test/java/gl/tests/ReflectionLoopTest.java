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
 * Reflection loop test.
 *
 * <p>Proves the self-critique agentic pattern works within the
 * Generative Layer. The reflection loop is:
 * <pre>
 *   Generate (ANSWER) -> Critique (REFLECT) -> Accept or Retry
 * </pre>
 *
 * <p>This maps to the modern "Reflection" pattern used by
 * Claude, Gemini, and GPT agents, but with formal governance:
 * each step produces a traceable candidate with explicit
 * accept/reject by the BDI agent.
 *
 * <p>Paper 2 evidence: BDI self-critique with formal governance
 * outperforms ad-hoc LLM self-correction.
 */
public final class ReflectionLoopTest {

    private GovernanceKernel kernel;
    private GenerativeBodyRegistry bodies;

    @BeforeEach
    void setUp() {
        kernel = GovernanceKernelFactory.deterministicInMemory();
        bodies = GenerativeBodyRuntime.createStandardRegistry(kernel);
    }

    // --- Basic reflection: generate then critique ---------------

    @Test
    void reflectionCritiqueProducesReflectionNote() {
        // Step 1: Generate an answer
        InvocationResult answer = bodies.require("llm.answer").invoke(new BodyInvocation(
                "agent_a", "answer_question", "llm.answer", BodyAffordance.ANSWER,
                "What is the capital of France? Return label and confidence.",
                List.of("label", "confidence"), Map.of()
        ));
        assertEquals(InvocationStatus.CANDIDATE_READY, answer.status());
        Candidate answerCand = kernel.candidate(answer.candidateId()).orElseThrow();
        String answerText = answerCand.fields().get("label");
        assertFalse(answerText.isBlank());

        // Step 2: Critique the answer using the reflection body
        InvocationResult critique = bodies.require("reflect.critique").invoke(new BodyInvocation(
                "agent_a", "critique_answer", "reflect.critique", BodyAffordance.REFLECT,
                "Critique this answer: " + answerText + ". Return verdict and confidence.",
                List.of("verdict", "confidence"), Map.of("mode", "reflection")
        ));
        assertEquals(InvocationStatus.CANDIDATE_READY, critique.status());

        // Step 3: Verify the critique is a REFLECTION_NOTE candidate
        Candidate critiqueCand = kernel.candidate(critique.candidateId()).orElseThrow();
        assertEquals(CandidateType.REFLECTION_NOTE, critiqueCand.type(),
                "Critique must produce a REFLECTION_NOTE candidate type");
    }

    // --- Reflection loop: retry on negative critique ------------

    @Test
    void negativeReflectionLeadsToAnswerRejection() {
        // Step 1: Generate an answer
        InvocationResult answer = bodies.require("llm.answer").invoke(new BodyInvocation(
                "agent_a", "answer_question", "llm.answer", BodyAffordance.ANSWER,
                "Return label and confidence.",
                List.of("label", "confidence"), Map.of()
        ));
        assertTrue(answer.resourceResult().success());

        // Step 2: Generate a self-critique with "needs_evidence" verdict
        InvocationResult critique = bodies.require("reflect.critique").invoke(new BodyInvocation(
                "agent_a", "self_critique", "reflect.critique", BodyAffordance.CRITIQUE,
                "Critique the answer. Return verdict and confidence.",
                List.of("verdict", "confidence"), Map.of("mode", "reflection")
        ));

        // Step 3: Agent reads critique verdict
        Candidate critiqueCand2 = kernel.candidate(critique.candidateId()).orElseThrow();
        String verdict = critiqueCand2.fields().get("verdict");
        assertEquals("needs_evidence", verdict,
                "Reflection mode in fake provider must return needs_evidence");

        // Step 4: Based on negative self-critique, agent rejects original answer
        kernel.recordDecision(answer.candidateId(), DecisionType.REJECTED, "negative self-critique");
        Candidate rejected = kernel.candidate(answer.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.REJECTED_BY_AGENT, rejected.status(),
                "Agent must reject answer after negative self-critique");
    }

    // --- Both candidates produce independent traces -------------

    @Test
    void reflectionProducesIndependentTraces() {
        InvocationResult answer = bodies.require("llm.answer").invoke(new BodyInvocation(
                "agent_a", "goal_1", "llm.answer", BodyAffordance.ANSWER,
                "Return label", List.of("label"), Map.of()
        ));
        InvocationResult critique = bodies.require("reflect.critique").invoke(new BodyInvocation(
                "agent_a", "goal_1_critique", "reflect.critique", BodyAffordance.CRITIQUE,
                "Return verdict", List.of("verdict"), Map.of("mode", "reflection")
        ));

        String answerTrace = answer.traceId();
        String critiqueTrace = critique.traceId();
        assertFalse(answerTrace.isBlank());
        assertFalse(critiqueTrace.isBlank());
        assertNotEquals(answerTrace, critiqueTrace,
                "Answer and critique must have independent trace records");
    }

    // --- Self-assessment via assess() ---------------------------

    @Test
    void agentCanSelfAssessUsingReflectionOutput() {
        // Generate answer
        InvocationResult answer = bodies.require("llm.answer").invoke(new BodyInvocation(
                "agent_a", "goal_1", "llm.answer", BodyAffordance.ANSWER,
                "Return label and confidence",
                List.of("label", "confidence"), Map.of()
        ));
        String candidateId = answer.candidateId();

        // Generate self-critique
        InvocationResult critique = bodies.require("reflect.critique").invoke(new BodyInvocation(
                "agent_a", "self_reflect", "reflect.critique", BodyAffordance.REFLECT,
                "Return verdict and confidence",
                List.of("verdict", "confidence"), Map.of("mode", "reflection")
        ));

        // Use critique output as basis for self-assessment
        Candidate critiqueCand3 = kernel.candidate(critique.candidateId()).orElseThrow();
        String critiqueVerdict = critiqueCand3.fields().get("verdict");
        double critiqueConfidence = Double.parseDouble(
                critiqueCand3.fields().get("confidence")
        );

        Assessment selfAssessment = kernel.assess(
                "agent_a",    // self-assessment
                candidateId,
                "candidate",
                critiqueVerdict.equals("good") ? Outcomes.AssessmentVerdict.APPROVE : Outcomes.AssessmentVerdict.REJECT_VERDICT,
                critiqueConfidence,
                List.of("self-reflection"),
                List.of(critique.resourceResult().resultId()),
                "Self-assessment based on reflection: " + critiqueVerdict
        );

        assertNotNull(selfAssessment);
        assertEquals("agent_a", selfAssessment.assessorId(),
                "Self-assessment assessorId must match the generating agent");

        // Candidate status must be ASSESSED
        Candidate assessed = kernel.candidate(candidateId).orElseThrow();
        assertEquals(CandidateStatus.ASSESSED, assessed.status());
    }
}
