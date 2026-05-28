package gl.adapters;

import gl.body.*;
import gl.kernel.*;
import java.util.*;

public abstract class AdapterBase {
    private final GovernanceKernel GovernanceKernel;
    private final GenerativeBodyRegistry bodies;

    protected AdapterBase(GovernanceKernel GovernanceKernel, GenerativeBodyRegistry bodies) {
        this.GovernanceKernel = Objects.requireNonNull(GovernanceKernel);
        this.bodies = Objects.requireNonNull(bodies);
    }

    protected final String invokeBody(String agentId, String goalId, String bodyId, String affordance, String prompt, String requiredCsv, Map<String, String> parameters) {
        BodyInvocation invocation = new BodyInvocation(agentId, goalId, bodyId, parseAffordance(affordance), prompt, csv(requiredCsv), parameters == null ? Map.of() : parameters);
        InvocationResult result = bodies.require(bodyId).invoke(invocation);
        return result.resourceResult() == null ? "" : result.resourceResult().resultId();
    }

    protected final boolean valid(String resultId) { return GovernanceKernel.valid(resultId); }
    protected final String field(String resultId, String field) { return GovernanceKernel.field(resultId, field); }
    protected final String candidate(String resultId) { return GovernanceKernel.result(resultId).map(ResourceResult::candidateId).orElse(""); }
    protected final String outputBlob(String resultId) { return GovernanceKernel.result(resultId).map(ResourceResult::outputBlobId).orElse(""); }
    protected final String trace(String resultId) { return GovernanceKernel.result(resultId).map(ResourceResult::traceId).orElse(""); }
    protected final String outcome(String resultId) { return GovernanceKernel.result(resultId).map(r -> r.outcome().name()).orElse("MISSING_RESULT"); }
    protected final boolean admissible(String candidateId) { return GovernanceKernel.checkAdmissibility(candidateId).admissible(); }
    protected final boolean accept(String candidateId) { return GovernanceKernel.acceptCandidate(candidateId).isPresent(); }
    protected final boolean reject(String candidateId) { return GovernanceKernel.rejectCandidate(candidateId).isPresent(); }

    protected final String assess(String assessorId, String candidateId, String verdict, double confidence, String criteriaCsv, String evidenceCsv, String explanation) {
        return GovernanceKernel.assess(assessorId, candidateId, "candidate", parseVerdict(verdict), confidence, csv(criteriaCsv), csv(evidenceCsv), explanation).assessmentId();
    }

    protected static List<String> csv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static BodyAffordance parseAffordance(String value) {
        if (value == null || value.isBlank()) return BodyAffordance.ANSWER;
        try { return BodyAffordance.valueOf(value.trim().toUpperCase()); } catch (IllegalArgumentException ex) { return BodyAffordance.ANSWER; }
    }

    private static Outcomes.AssessmentVerdict parseVerdict(String value) {
        if (value == null || value.isBlank()) return Outcomes.AssessmentVerdict.UNCERTAIN;
        try { return Outcomes.AssessmentVerdict.valueOf(value.trim().toUpperCase()); } catch (IllegalArgumentException ex) { return Outcomes.AssessmentVerdict.UNCERTAIN; }
    }
}
