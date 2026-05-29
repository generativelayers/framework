package gl.body;

import java.util.List;
import java.util.Map;

/** Request to invoke a generative body: the agent's identity, goal context,
 *  which affordance to use, the prompt text, required output fields,
 *  belief context for RAG, and optional parameters. */
public record BodyInvocation(
        String agentId,
        String goalId,
        String bodyId,
        BodyAffordance affordance,
        String prompt,
        List<String> requiredFields,
        Map<String, String> parameters,
        List<String> beliefContext,
        String conversationId
) {
    public BodyInvocation {
        agentId = agentId == null ? "" : agentId;
        goalId = goalId == null ? "" : goalId;
        bodyId = bodyId == null || bodyId.isBlank() ? "llm.answer" : bodyId;
        affordance = affordance == null ? BodyAffordance.ANSWER : affordance;
        prompt = prompt == null ? "" : prompt;
        requiredFields = requiredFields == null ? List.of() : List.copyOf(requiredFields);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        beliefContext = beliefContext == null ? List.of() : List.copyOf(beliefContext);
        conversationId = conversationId == null ? "" : conversationId;
    }

    /** Convenience constructor without belief context or conversation. */
    public BodyInvocation(String agentId, String goalId, String bodyId,
                          BodyAffordance affordance, String prompt,
                          List<String> requiredFields, Map<String, String> parameters) {
        this(agentId, goalId, bodyId, affordance, prompt, requiredFields, parameters, List.of(), "");
    }
}
