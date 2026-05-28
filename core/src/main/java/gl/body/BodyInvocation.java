package gl.body;

import java.util.List;
import java.util.Map;

public record BodyInvocation(
        String agentId,
        String goalId,
        String bodyId,
        BodyAffordance affordance,
        String prompt,
        List<String> requiredFields,
        Map<String, String> parameters
) {
    public BodyInvocation {
        agentId = agentId == null ? "" : agentId;
        goalId = goalId == null ? "" : goalId;
        bodyId = bodyId == null || bodyId.isBlank() ? "llm.answer" : bodyId;
        affordance = affordance == null ? BodyAffordance.ANSWER : affordance;
        prompt = prompt == null ? "" : prompt;
        requiredFields = requiredFields == null ? List.of() : List.copyOf(requiredFields);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
