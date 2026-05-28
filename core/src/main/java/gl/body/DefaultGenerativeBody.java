package gl.body;

import gl.kernel.*;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class DefaultGenerativeBody implements GenerativeBody {
    private final GovernanceKernel kernel;
    private final BodyDescriptor descriptor;

    public DefaultGenerativeBody(GovernanceKernel kernel, BodyDescriptor descriptor) {
        this.kernel = Objects.requireNonNull(kernel);
        this.descriptor = Objects.requireNonNull(descriptor);
    }

    public BodyDescriptor descriptor() { return descriptor; }

    public InvocationResult invoke(BodyInvocation invocation) {
        Objects.requireNonNull(invocation);
        CandidateType type = typeFor(invocation.affordance(), descriptor.defaultCandidateType());
        ResponseSchema schema = invocation.requiredFields().isEmpty()
                ? ResponseSchema.freeText()
                : ResponseSchema.required(invocation.affordance().name().toLowerCase() + "_schema", invocation.requiredFields());
        ResourceRequest request = new ResourceRequest(null, invocation.agentId(), invocation.goalId(), descriptor.bodyId(),
                invocation.affordance().name().toLowerCase(), type, invocation.prompt(), schema,
                GovernanceContext.empty(), null, invocation.parameters());
        ResourceResult result = kernel.invoke(request);
        return new InvocationResult(descriptor.bodyId(), statusFor(result.outcome()), result,
                result.candidateId(), result.outputBlobId(), result.traceId(), result.message(), Instant.now());
    }

    private static CandidateType typeFor(BodyAffordance affordance, CandidateType fallback) {
        if (affordance == null) return fallback;
        return switch (affordance) {
            case ANSWER, CLASSIFY, SUMMARISE -> CandidateType.CANDIDATE_ANSWER;
            case GROUND_FACT -> CandidateType.GROUNDED_FACT;
            case DECOMPOSE_GOAL -> CandidateType.TASK_DECOMPOSITION;
            case PROPOSE_ACTION -> CandidateType.ACTION_PROPOSAL;
            case PROPOSE_TOOL_CALL -> CandidateType.TOOL_CALL_PROPOSAL;
            case RETRIEVE_MEMORY -> CandidateType.MEMORY_USE;
            case REFLECT, CRITIQUE -> CandidateType.REFLECTION_NOTE;
            case ASSESS, EXPLAIN, ESCALATE -> CandidateType.EXPLANATION;
        };
    }

    private static InvocationStatus statusFor(Outcomes.ResultOutcome outcome) {
        if (outcome == null) return InvocationStatus.CREATED;
        return switch (outcome) {
            case SUCCESS -> InvocationStatus.CANDIDATE_READY;
            case GOVERNANCE_DENIED -> InvocationStatus.DENIED;
            case GOVERNANCE_ESCALATED -> InvocationStatus.ESCALATED;
            case PROVIDER_FAILED -> InvocationStatus.PROVIDER_ERROR;
            case INVALID_OUTPUT -> InvocationStatus.INVALID_OUTPUT;
            case STORED_ONLY -> InvocationStatus.CREATED;
        };
    }
}
