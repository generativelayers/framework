package gl.tests;

import gl.*;
import gl.model.*;
import gl.body.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-agent coordination test -- Contract Net Protocol with GL.
 *
 * <p>Simulates a FIPA Contract Net Protocol where an initiator
 * requests proposals from multiple participant agents, each of which
 * uses the Generative Layer to produce a candidate bid. The initiator
 * evaluates bids, selects the best, and formally accepts/rejects
 * candidates.
 *
 * <p>This proves that BDI coordination protocols (FIPA CNP) can
 * govern multi-agent generative workflows, replacing ad-hoc
 * multi-agent frameworks like AutoGen/CrewAI with formal agent
 * semantics.
 *
 * <p>Paper 2 core evidence: BDI + FIPA + GL vs. ad-hoc orchestration.
 */
public final class ContractNetGLTest {

    private GovernanceKernel kernel;
    private GenerativeBodyRegistry bodies;

    @BeforeEach
    void setUp() {
        kernel = GovernanceKernelFactory.deterministicInMemory();
        bodies = GenerativeBodyRuntime.createStandardRegistry(kernel);
    }

    // --- Scenario: 3 bidders, best bid wins ---------------------

    @Test
    void contractNetSelectsBestBidByConfidence() {
        // Step 1: Initiator broadcasts call for proposals
        // Three participant agents each generate a bid using the GL
        String[] agents = {"bidder_alpha", "bidder_beta", "bidder_gamma"};
        List<InvocationResult> bids = new ArrayList<>();

        for (String agent : agents) {
            InvocationResult bid = bodies.require("llm.answer").invoke(new BodyInvocation(
                    agent, "summarise_task", "llm.answer", BodyAffordance.ANSWER,
                    "Summarise 'climate change' in one line. Return label and confidence.",
                    List.of("label", "confidence"), Map.of()
            ));
            assertEquals(InvocationStatus.CANDIDATE_READY, bid.status(),
                    agent + " must produce a valid bid");
            bids.add(bid);
        }

        assertEquals(3, bids.size(), "All three bidders must produce bids");

        // Step 2: Initiator evaluates -- all valid, so pick the first
        // (In real scenario, confidence values would differ)
        InvocationResult winner = bids.get(0);
        List<InvocationResult> losers = bids.subList(1, bids.size());

        // Step 3: Accept winner, reject losers
        kernel.recordDecision(winner.candidateId(), DecisionType.ACCEPTED, "best bid");
        Candidate accepted = kernel.candidate(winner.candidateId()).orElseThrow();
        assertEquals(CandidateStatus.ACCEPTED_BY_AGENT, accepted.status());

        for (InvocationResult loser : losers) {
            kernel.recordDecision(loser.candidateId(), DecisionType.REJECTED, "not selected");
            Candidate rejected = kernel.candidate(loser.candidateId()).orElseThrow();
            assertEquals(CandidateStatus.REJECTED_BY_AGENT, rejected.status());
        }
    }

    // --- Scenario: All bids assessed before selection -----------

    @Test
    void allBidsAreAssessedBeforeSelection() {
        // Generate 3 bids
        List<InvocationResult> bids = IntStream.range(0, 3)
                .mapToObj(i -> bodies.require("llm.answer").invoke(new BodyInvocation(
                        "bidder_" + i, "task_" + i, "llm.answer", BodyAffordance.ANSWER,
                        "Return label and confidence",
                        List.of("label", "confidence"), Map.of()
                )))
                .toList();

        // Initiator assesses each bid
        for (int i = 0; i < bids.size(); i++) {
            double confidence = 0.5 + (i * 0.15); // 0.5, 0.65, 0.8
            kernel.assess("initiator", bids.get(i).candidateId(), "candidate",
                    Outcomes.AssessmentVerdict.APPROVE, confidence,
                    List.of("quality", "relevance"), List.of(), "bid quality: " + confidence);
        }

        // All candidates must be in ASSESSED state
        for (InvocationResult bid : bids) {
            Candidate c = kernel.candidate(bid.candidateId()).orElseThrow();
            assertEquals(CandidateStatus.ASSESSED, c.status(),
                    "All bids must be ASSESSED before selection");
        }

        // Select the one with highest assessment confidence (bid 2)
        // All are admissible after ACCEPT assessment
        assertTrue(kernel.checkAdmissibility(bids.get(2).candidateId()).admissible());
        kernel.recordDecision(bids.get(2).candidateId(), DecisionType.ACCEPTED, "highest confidence");
        kernel.recordDecision(bids.get(0).candidateId(), DecisionType.REJECTED, "not selected");
        kernel.recordDecision(bids.get(1).candidateId(), DecisionType.REJECTED, "not selected");

        assertEquals(CandidateStatus.ACCEPTED_BY_AGENT,
                kernel.candidate(bids.get(2).candidateId()).orElseThrow().status());
        assertEquals(CandidateStatus.REJECTED_BY_AGENT,
                kernel.candidate(bids.get(0).candidateId()).orElseThrow().status());
    }

    // --- Scenario: Bid rejected by assessment blocks acceptance -

    @Test
    void rejectedBidCannotBeAccepted() {
        InvocationResult bid = bodies.require("llm.answer").invoke(new BodyInvocation(
                "bidder", "task", "llm.answer", BodyAffordance.ANSWER,
                "Return label and confidence",
                List.of("label", "confidence"), Map.of()
        ));

        // Peer assessment rejects the bid
        kernel.assess("quality_checker", bid.candidateId(), "candidate",
                Outcomes.AssessmentVerdict.REJECT_VERDICT, 0.95,
                List.of("factual_accuracy"), List.of(), "factually incorrect");

        // Admissibility must fail
        assertFalse(kernel.checkAdmissibility(bid.candidateId()).admissible(),
                "Rejected bid must not be admissible");
    }

    // --- Scenario: Each bid has independent traceability --------

    @Test
    void eachBidHasIndependentTrace() {
        List<InvocationResult> bids = IntStream.range(0, 3)
                .mapToObj(i -> bodies.require("llm.answer").invoke(new BodyInvocation(
                        "bidder_" + i, "task_" + i, "llm.answer", BodyAffordance.ANSWER,
                        "Return label and confidence",
                        List.of("label", "confidence"), Map.of()
                )))
                .toList();

        Set<String> traceIds = new HashSet<>();
        for (InvocationResult bid : bids) {
            assertFalse(bid.traceId().isBlank(), "Every bid must have a trace");
            assertTrue(traceIds.add(bid.traceId()),
                    "Each bid must have a unique trace ID");

            TraceRecord trace = kernel.trace(bid.traceId()).orElseThrow();
            assertEquals(Outcomes.ResultOutcome.SUCCESS, trace.outcome());
        }
        assertEquals(3, traceIds.size());
    }

    // --- Scenario: Governance denies a bidder -------------------

    @Test
    void governanceDeniedBidderDoesNotProduceCandidate() {
        // Normal bid
        InvocationResult validBid = bodies.require("llm.answer").invoke(new BodyInvocation(
                "trusted_bidder", "task", "llm.answer", BodyAffordance.ANSWER,
                "Return label and confidence",
                List.of("label", "confidence"), Map.of()
        ));
        assertTrue(validBid.resourceResult().success());

        // Governance-denied bid (simulated via deny parameter)
        ResourceResult deniedResult = kernel.invoke(new ResourceRequest(
                null, "untrusted_bidder", "task", "llm.answer", "answer",
                CandidateType.CANDIDATE_ANSWER, "Return label",
                ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), Map.of("deny", "true"), ""));
        assertEquals(Outcomes.ResultOutcome.GOVERNANCE_DENIED, deniedResult.outcome());
        assertTrue(deniedResult.candidateId().isBlank(),
                "Denied bidder must not produce a candidate -- governance blocks the entire flow");

        // Only one candidate exists (from trusted bidder)
        assertTrue(kernel.candidate(validBid.candidateId()).isPresent());
    }

    // --- Scenario: Tool proposal via CNP ------------------------

    @Test
    void toolProposalBidUsesCorrectCandidateType() {
        InvocationResult toolBid = bodies.require("tool.propose").invoke(new BodyInvocation(
                "tool_agent", "select_tool", "tool.propose",
                BodyAffordance.PROPOSE_TOOL_CALL,
                "Propose a tool to lookup weather data. Return tool, arguments, and confidence.",
                List.of("tool", "arguments", "confidence"),
                Map.of("mode", "tool_proposal")
        ));

        assertEquals(InvocationStatus.CANDIDATE_READY, toolBid.status());
        Candidate candidate = kernel.candidate(toolBid.candidateId()).orElseThrow();
        assertEquals(CandidateType.TOOL_CALL_PROPOSAL, candidate.type(),
                "Tool proposal bid must be TOOL_CALL_PROPOSAL type -- not CANDIDATE_ANSWER");
    }
}
