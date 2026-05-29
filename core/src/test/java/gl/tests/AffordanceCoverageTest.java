package gl.tests;

import gl.*;
import gl.model.*;
import gl.body.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Affordance coverage test.
 *
 * <p>Proves that every {@link BodyAffordance} maps to the correct
 * {@link CandidateType} and produces a valid, traceable candidate
 * through the governance kernel.
 *
 * <p>This is essential evidence for Paper 1: the framework's 13
 * affordance types cover the full taxonomy of modern agentic AI
 * operations (answer, classify, summarise, ground, decompose,
 * propose tool/action, retrieve memory, reflect, critique,
 * assess, explain, escalate).
 */
public final class AffordanceCoverageTest {

    private GovernanceKernel kernel;
    private GenerativeBodyRegistry bodies;

    @BeforeEach
    void setUp() {
        kernel = GovernanceKernelFactory.deterministicInMemory();
        bodies = GenerativeBodyRuntime.createStandardRegistry(kernel);
    }

    // ─── Affordance → CandidateType mapping ─────────────────────

    @Test
    void answerAffordanceProducesCandidateAnswer() {
        assertAffordanceProduces("llm.answer", BodyAffordance.ANSWER, CandidateType.CANDIDATE_ANSWER);
    }

    @Test
    void classifyAffordanceProducesCandidateAnswer() {
        assertAffordanceProduces("llm.answer", BodyAffordance.CLASSIFY, CandidateType.CANDIDATE_ANSWER);
    }

    @Test
    void summariseAffordanceProducesCandidateAnswer() {
        assertAffordanceProduces("llm.answer", BodyAffordance.SUMMARISE, CandidateType.CANDIDATE_ANSWER);
    }

    @Test
    void groundFactAffordanceProducesGroundedFact() {
        assertAffordanceProduces("rag.ground", BodyAffordance.GROUND_FACT, CandidateType.GROUNDED_FACT);
    }

    @Test
    void decomposeGoalAffordanceProducesTaskDecomposition() {
        assertAffordanceProduces("planner.decompose", BodyAffordance.DECOMPOSE_GOAL, CandidateType.TASK_DECOMPOSITION);
    }

    @Test
    void proposeToolCallAffordanceProducesToolCallProposal() {
        assertAffordanceProduces("tool.propose", BodyAffordance.PROPOSE_TOOL_CALL, CandidateType.TOOL_CALL_PROPOSAL);
    }

    @Test
    void proposeActionAffordanceProducesActionProposal() {
        assertAffordanceProduces("tool.propose", BodyAffordance.PROPOSE_ACTION, CandidateType.ACTION_PROPOSAL);
    }

    @Test
    void retrieveMemoryAffordanceProducesMemoryUse() {
        assertAffordanceProduces("memory.retrieve", BodyAffordance.RETRIEVE_MEMORY, CandidateType.MEMORY_USE);
    }

    @Test
    void reflectAffordanceProducesReflectionNote() {
        assertAffordanceProduces("reflect.critique", BodyAffordance.REFLECT, CandidateType.REFLECTION_NOTE);
    }

    @Test
    void critiqueAffordanceProducesReflectionNote() {
        assertAffordanceProduces("reflect.critique", BodyAffordance.CRITIQUE, CandidateType.REFLECTION_NOTE);
    }

    @Test
    void explainAffordanceProducesExplanation() {
        assertAffordanceProduces("reflect.critique", BodyAffordance.EXPLAIN, CandidateType.EXPLANATION);
    }

    @Test
    void assessAffordanceProducesExplanation() {
        assertAffordanceProduces("llm.answer", BodyAffordance.ASSESS, CandidateType.EXPLANATION);
    }

    @Test
    void escalateAffordanceProducesExplanation() {
        assertAffordanceProduces("llm.answer", BodyAffordance.ESCALATE, CandidateType.EXPLANATION);
    }

    // ─── Every affordance produces a trace ──────────────────────

    @Test
    void allAffordancesProduceTraces() {
        record Pair(String body, BodyAffordance affordance) {}
        List<Pair> pairs = List.of(
                new Pair("llm.answer", BodyAffordance.ANSWER),
                new Pair("llm.answer", BodyAffordance.CLASSIFY),
                new Pair("llm.answer", BodyAffordance.SUMMARISE),
                new Pair("rag.ground", BodyAffordance.GROUND_FACT),
                new Pair("planner.decompose", BodyAffordance.DECOMPOSE_GOAL),
                new Pair("tool.propose", BodyAffordance.PROPOSE_TOOL_CALL),
                new Pair("tool.propose", BodyAffordance.PROPOSE_ACTION),
                new Pair("memory.retrieve", BodyAffordance.RETRIEVE_MEMORY),
                new Pair("reflect.critique", BodyAffordance.REFLECT),
                new Pair("reflect.critique", BodyAffordance.CRITIQUE),
                new Pair("reflect.critique", BodyAffordance.EXPLAIN)
        );

        for (Pair p : pairs) {
            InvocationResult result = bodies.require(p.body()).invoke(new BodyInvocation(
                    "test_agent", "test_goal", p.body(), p.affordance(),
                    "Return label", List.of("label"), Map.of()
            ));
            assertFalse(result.traceId().isBlank(),
                    p.affordance() + " affordance must produce a trace");
        }
    }

    // ─── Helper ─────────────────────────────────────────────────

    private void assertAffordanceProduces(String bodyId, BodyAffordance affordance, CandidateType expectedType) {
        InvocationResult result = bodies.require(bodyId).invoke(new BodyInvocation(
                "test_agent", "test_goal", bodyId, affordance,
                "Return label", List.of("label"), Map.of()
        ));

        assertEquals(InvocationStatus.CANDIDATE_READY, result.status(),
                affordance + " must produce CANDIDATE_READY status");
        assertTrue(result.resourceResult().success(),
                affordance + " must produce a successful result");

        Candidate candidate = kernel.candidate(result.candidateId()).orElseThrow();
        assertEquals(expectedType, candidate.type(),
                affordance + " must produce " + expectedType + " candidate type");
    }
}
