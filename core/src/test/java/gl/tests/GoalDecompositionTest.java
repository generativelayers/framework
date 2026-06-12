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
 * Goal decomposition test.
 *
 * <p>Proves that the {@link BodyAffordance#DECOMPOSE_GOAL} affordance
 * produces a {@link CandidateType#TASK_DECOMPOSITION} candidate
 * containing structured sub-steps that a BDI agent can adopt
 * as sub-goals.
 *
 * <p>This maps to the modern "Plan-and-Execute" pattern used by
 * LangGraph and similar frameworks, but with formal BDI semantics:
 * decomposition is a candidate that must be validated, assessed,
 * and explicitly accepted before sub-goals are adopted.
 *
 * <p>Paper 2 evidence: BDI sub-goaling with try/recover provides
 * fault-tolerant decomposition that ad-hoc planners lack.
 */
public final class GoalDecompositionTest {

    private GovernanceKernel kernel;
    private GenerativeBodyRegistry bodies;

    @BeforeEach
    void setUp() {
        kernel = GovernanceKernelFactory.deterministicInMemory();
        bodies = GenerativeBodyRuntime.createStandardRegistry(kernel);
    }

    // --- Basic decomposition ------------------------------------

    @Test
    void decomposeGoalProducesTaskDecompositionCandidate() {
        InvocationResult result = bodies.require("planner.decompose").invoke(new BodyInvocation(
                "planner_agent", "plan_research", "planner.decompose",
                BodyAffordance.DECOMPOSE_GOAL,
                "Decompose: research and summarise a topic. Return step1, step2, step3, and confidence.",
                List.of("step1", "step2", "step3", "confidence"),
                Map.of("mode", "plan")
        ));

        assertEquals(InvocationStatus.CANDIDATE_READY, result.status());
        assertTrue(result.resourceResult().success());

        // Candidate must be TASK_DECOMPOSITION type
        Candidate candidate = kernel.candidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateType.TASK_DECOMPOSITION, candidate.type(),
                "DECOMPOSE_GOAL affordance must produce TASK_DECOMPOSITION candidate");
        assertEquals(CandidateStatus.VALIDATED, candidate.status());
    }

    // --- Decomposed steps are extractable -----------------------

    @Test
    void decomposedStepsAreExtractableAsFields() {
        InvocationResult result = bodies.require("planner.decompose").invoke(new BodyInvocation(
                "planner_agent", "plan_task", "planner.decompose",
                BodyAffordance.DECOMPOSE_GOAL,
                "Break into steps. Return step1, step2, step3, and confidence.",
                List.of("step1", "step2", "step3", "confidence"),
                Map.of("mode", "plan")
        ));

        assertTrue(result.resourceResult().success());
        Candidate c = kernel.candidate(result.candidateId()).orElseThrow();
        String step1 = c.fields().get("step1");
        String step2 = c.fields().get("step2");
        String step3 = c.fields().get("step3");

        assertFalse(step1.isBlank(), "step1 must be extractable");
        assertFalse(step2.isBlank(), "step2 must be extractable");
        assertFalse(step3.isBlank(), "step3 must be extractable");
    }

    // --- Decomposition is not auto-executed ---------------------

    @Test
    void decompositionRequiresExplicitAdoption() {
        InvocationResult result = bodies.require("planner.decompose").invoke(new BodyInvocation(
                "planner_agent", "plan_task", "planner.decompose",
                BodyAffordance.DECOMPOSE_GOAL,
                "Break into steps. Return step1, step2, step3, and confidence.",
                List.of("step1", "step2", "step3", "confidence"),
                Map.of("mode", "plan")
        ));

        // Before acceptance: candidate is VALIDATED, not ACCEPTED
        Candidate before = kernel.candidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.VALIDATED, before.status(),
                "Plan must be VALIDATED, not auto-executed -- agent decides when to adopt sub-goals");

        // Agent explicitly accepts the plan
        kernel.recordDecision(result.candidateId(), DecisionType.ACCEPTED, "plan approved");
        Candidate accepted = kernel.candidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.ACCEPTED_BY_AGENT, accepted.status(),
                "After accept, status must be ACCEPTED_BY_AGENT");
    }

    // --- Agent can reject a bad plan ----------------------------

    @Test
    void agentCanRejectBadDecomposition() {
        InvocationResult result = bodies.require("planner.decompose").invoke(new BodyInvocation(
                "planner_agent", "plan_task", "planner.decompose",
                BodyAffordance.DECOMPOSE_GOAL,
                "Break into steps. Return step1, step2, step3, and confidence.",
                List.of("step1", "step2", "step3", "confidence"),
                Map.of("mode", "plan")
        ));

        // Agent assesses the plan as low quality
        kernel.assess("planner_agent", result.candidateId(), "candidate",
                Outcomes.AssessmentVerdict.REJECT_VERDICT, 0.85,
                List.of("feasibility"), List.of(), "plan steps are too vague");

        // Plan should be inadmissible now
        assertFalse(kernel.checkAdmissibility(result.candidateId()).admissible(),
                "Rejected plan must not be admissible");

        // Agent explicitly rejects
        kernel.recordDecision(result.candidateId(), DecisionType.REJECTED, "plan too vague");
        Candidate rejected = kernel.candidate(result.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.REJECTED_BY_AGENT, rejected.status());
    }

    // --- Decomposition has its own trace ------------------------

    @Test
    void decompositionProducesAuditTrace() {
        InvocationResult result = bodies.require("planner.decompose").invoke(new BodyInvocation(
                "planner_agent", "plan_task", "planner.decompose",
                BodyAffordance.DECOMPOSE_GOAL,
                "Break into steps. Return step1, step2, step3, and confidence.",
                List.of("step1", "step2", "step3", "confidence"),
                Map.of("mode", "plan")
        ));

        assertFalse(result.traceId().isBlank(), "Decomposition must produce a trace");
        TraceRecord trace = kernel.trace(result.traceId()).orElseThrow();
        assertEquals("planner_agent", trace.agentId());
        assertEquals("plan_task", trace.goalId());
        assertEquals(Outcomes.ResultOutcome.SUCCESS, trace.outcome());
    }
}
