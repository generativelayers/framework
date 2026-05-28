package layer.kernel;

import java.util.Map;

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
        BeliefSnapshot beliefSnapshot,
        Map<String, String> parameters
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
        beliefSnapshot = beliefSnapshot == null ? BeliefSnapshot.empty(agentId) : beliefSnapshot;
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
