package gl.model;

import gl.Ids;

import java.util.Map;

/** A request to generate content via the governance kernel.
 *  Captures everything needed: agent identity, goal context, prompt,
 *  expected candidate type, response schema, and governance context. */
public record ResourceRequest(
        String requestId,
        String agentId,
        String goalId,
        String resourceId,
        String taskType,
        CandidateType expectedCandidateType,
        String prompt,
        ResponseSchema schema,
        GovernanceContext governanceContext,
        Map<String, String> parameters,
        String conversationId
) {
    public ResourceRequest {
        requestId = requestId == null || requestId.isBlank() ? Ids.id("req") : requestId;
        agentId = agentId == null ? "" : agentId;
        goalId = goalId == null ? "" : goalId;
        resourceId = resourceId == null || resourceId.isBlank() ? "default" : resourceId;
        taskType = taskType == null || taskType.isBlank() ? "generate" : taskType;
        expectedCandidateType = expectedCandidateType == null ? CandidateType.CANDIDATE_ANSWER : expectedCandidateType;
        prompt = prompt == null ? "" : prompt;
        schema = schema == null ? ResponseSchema.freeText() : schema;
        governanceContext = governanceContext == null ? GovernanceContext.empty() : governanceContext;
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        conversationId = conversationId == null ? "" : conversationId;
    }
}
