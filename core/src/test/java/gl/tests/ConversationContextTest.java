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
    void adapterExposesStatefulAsk() {
        GovernanceKernel kernel = GovernanceKernelFactory.deterministicInMemory();
        var registry = GenerativeBodyRuntime.createStandardRegistry(kernel);
        var adapter = new DirectAdapter(kernel, registry);

        adapter.ask("agent1", "goal1", "Q1", "conv-adapter-test");
        adapter.ask("agent1", "goal1", "Q2", "conv-adapter-test");

        var conversation = kernel.conversation("conv-adapter-test");
        assertEquals(4, conversation.size()); // 2 turns * (user + assistant) = 4
    }
}
