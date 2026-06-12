package gl.tests;

import gl.*;
import gl.model.*;
import gl.body.GenerativeBodyRuntime;
import gl.adapter.DirectAdapter;


import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for multi-turn conversation context.
 */
class ConversationContextTest {

    @Test
    void conversationAccumulatesTurns() {
        ConversationContext ctx = new ConversationContext("conv-1");
        assertEquals(0, ctx.size());

        ctx.addTurn("user", "Hello");
        ctx.addTurn("assistant", "Hi there");
        assertEquals(2, ctx.size());

        var turns = ctx.turns();
        assertEquals("user", turns.get(0).role());
        assertEquals("Hello", turns.get(0).content());
    }

    @Test
    void buildPromptWithoutHistoryReturnsOriginal() {
        ConversationContext ctx = new ConversationContext("conv-2");
        assertEquals("What is 2+2?", ctx.buildPrompt("What is 2+2?"));
    }

    @Test
    void buildPromptPrependsHistory() {
        ConversationContext ctx = new ConversationContext("conv-3");
        ctx.addTurn("user", "What is AI?");
        ctx.addTurn("assistant", "Artificial Intelligence");

        String prompt = ctx.buildPrompt("Tell me more");
        assertTrue(prompt.startsWith("Conversation history:"));
        assertTrue(prompt.contains("user: What is AI?"));
        assertTrue(prompt.contains("assistant: Artificial Intelligence"));
        assertTrue(prompt.contains("Current request:\nTell me more"));
    }

    @Test
    void kernelRecordsTurnsOnSuccess() {
        GovernanceKernel kernel = GovernanceKernelFactory.deterministicInMemory();

        // First turn
        ResourceResult r1 = kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), Map.of(), "conv-test"));

        assertEquals(Outcomes.ResultOutcome.SUCCESS, r1.outcome());
        ConversationContext ctx = kernel.conversation("conv-test");
        assertEquals(2, ctx.size()); // 1 user turn + 1 assistant turn
    }

    @Test
    void separateConversationsAreIsolated() {
        GovernanceKernel kernel = GovernanceKernelFactory.deterministicInMemory();

        kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "Q1", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), Map.of(), "conv-A"));

        kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "Q2", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), Map.of(), "conv-B"));

        assertEquals(2, kernel.conversation("conv-A").size()); // 1 turn
        assertEquals(2, kernel.conversation("conv-B").size()); // 1 turn (separate)
    }

    @Test
    void emptyConversationIdSkipsContext() {
        GovernanceKernel kernel = GovernanceKernelFactory.deterministicInMemory();

        kernel.invoke(new ResourceRequest(
                null, "a", "g", "r", "t", CandidateType.CANDIDATE_ANSWER,
                "Q1", ResponseSchema.required("s", List.of("label")),
                GovernanceContext.empty(), Map.of(), ""));

        // No conversation should be created for empty ID
        assertEquals(0, kernel.conversation("").size()); // fresh empty context
    }

    @Test
    void adapterExposesStatefulCall() {
        // GL v2: use bind() + call() instead of ask()
        var adapter = new DirectAdapter();
        String bid = adapter.bind("agent1", "fake", "", "");
        assertFalse(bid.startsWith("ERROR:"), "Binding must succeed");

        String rid1 = adapter.call(bid, "goal1", "llm.answer", "ANSWER", "Q1", "label", "");
        String rid2 = adapter.call(bid, "goal1", "llm.answer", "ANSWER", "Q2", "label", "");

        // Both calls should produce result IDs (not errors)
        assertFalse(rid1.startsWith("ERROR:"), "First call must succeed");
        assertFalse(rid2.startsWith("ERROR:"), "Second call must succeed");

        // Both should have valid results
        assertEquals("SUCCESS", adapter.result(rid1));
        assertEquals("SUCCESS", adapter.result(rid2));
    }
}
