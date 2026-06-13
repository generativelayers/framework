package gl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates prompt/response turns for multi-turn conversations.
 *
 * <p>Thread-safe. Each conversation has a unique ID. Past turns are
 * prepended to new prompts so the LLM sees the full dialogue history.
 */
public final class ConversationContext {

    /** A single turn in the conversation. */
    public record Turn(String role, String content) {}

    /** Maximum number of conversation turns retained. Oldest turns are evicted
     *  when this limit is exceeded, preventing unbounded prompt/memory growth. */
    private static final int MAX_TURNS = 20;

    private final String contextId;
    private final List<Turn> turns = Collections.synchronizedList(new ArrayList<>());

    public ConversationContext(String contextId) {
        this.contextId = contextId;
    }

    public String contextId() { return contextId; }

    /** Add a turn to the conversation history.
     *  Evicts oldest turns when the history exceeds {@link #MAX_TURNS}. */
    public void addTurn(String role, String content) {
        synchronized (turns) {
            turns.add(new Turn(role, content));
            while (turns.size() > MAX_TURNS) turns.remove(0);
        }
    }

    /** Get an unmodifiable snapshot of the current turns. */
    public List<Turn> turns() {
        synchronized (turns) { return List.copyOf(turns); }
    }

    /**
     * Build a prompt with conversation history prepended.
     *
     * @param newPrompt the current user prompt
     * @return the full prompt with history context
     */
    public String buildPrompt(String newPrompt) {
        List<Turn> snapshot = turns();
        if (snapshot.isEmpty()) return newPrompt;
        StringBuilder sb = new StringBuilder();
        sb.append("Conversation history:\n");
        for (Turn t : snapshot) {
            sb.append(t.role()).append(": ").append(t.content()).append('\n');
        }
        sb.append("\nCurrent request:\n").append(newPrompt);
        return sb.toString();
    }

    /** Number of turns in this conversation. */
    public int size() { return turns.size(); }
}
