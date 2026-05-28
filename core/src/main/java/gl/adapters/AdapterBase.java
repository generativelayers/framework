package gl.adapters;

import gl.body.*;
import gl.kernel.*;
import gl.provider.ProviderRegistry;
import java.util.*;

/**
 * Base class for all Generative Layer adapters.
 *
 * <p>Implements the full {@link ResourceActions} contract. Platform-specific
 * adapters (ASTRA, Jason) delegate to these methods, guaranteeing that
 * every command has identical name, parameter count, and types.
 */
public abstract class AdapterBase implements ResourceActions {
    private final GovernanceKernel kernel;
    private final GenerativeBodyRegistry bodies;

    protected AdapterBase(GovernanceKernel kernel, GenerativeBodyRegistry bodies) {
        this.kernel = Objects.requireNonNull(kernel);
        this.bodies = Objects.requireNonNull(bodies);
    }

    // ── Internal plumbing (not part of ResourceActions) ────────

    protected final String invokeBody(String agentId, String goalId, String bodyId, String affordance, String prompt, String requiredCsv, Map<String, String> parameters) {
        BodyInvocation invocation = new BodyInvocation(agentId, goalId, bodyId, parseAffordance(affordance), prompt, csv(requiredCsv), parameters == null ? Map.of() : parameters);
        InvocationResult result = bodies.require(bodyId).invoke(invocation);
        return result.resourceResult() == null ? "" : result.resourceResult().resultId();
    }

    protected final String outputBlob(String resultId) { return kernel.result(resultId).map(ResourceResult::outputBlobId).orElse(""); }

    protected final String assessFull(String assessorId, String candidateId, String verdict, double confidence, String criteriaCsv, String evidenceCsv, String explanation) {
        return kernel.assess(assessorId, candidateId, "candidate", parseVerdict(verdict), confidence, csv(criteriaCsv), csv(evidenceCsv), explanation).assessmentId();
    }

    // ── ResourceActions implementation ─────────────────────────

    @Override
    public boolean configure(String key, String value) {
        // Default no-op: subclasses with provider switching override this.
        return true;
    }

    @Override
    public boolean use_provider() { return true; }

    @Override
    public boolean use_provider(String providerName) { return true; }

    @Override
    public boolean use_provider(String providerName, String model) { return true; }

    @Override
    public String providers() {
        return String.join(",", ProviderRegistry.available());
    }

    @Override
    public String invoke(String agentId, String goalId, String bodyId, String affordance, String prompt, String requiredCsv) {
        return invokeBody(agentId, goalId, bodyId, affordance, prompt, requiredCsv, Map.of());
    }

    @Override
    public String ask(String agentId, String goalId, String prompt) {
        return invokeBody(agentId, goalId, "llm.answer", "ANSWER", prompt, "", Map.of());
    }

    @Override
    public boolean valid(String resultId) { return kernel.valid(resultId); }

    @Override
    public String field(String resultId, String fieldName) { return kernel.field(resultId, fieldName); }

    @Override
    public String candidate(String resultId) { return kernel.result(resultId).map(ResourceResult::candidateId).orElse(""); }

    @Override
    public String trace(String resultId) { return kernel.result(resultId).map(ResourceResult::traceId).orElse(""); }

    @Override
    public String outcome(String resultId) { return kernel.result(resultId).map(r -> r.outcome().name()).orElse("MISSING_RESULT"); }

    @Override
    public boolean admissible(String candidateId) { return kernel.checkAdmissibility(candidateId).admissible(); }

    @Override
    public boolean accept(String candidateId) { return kernel.acceptCandidate(candidateId).isPresent(); }

    @Override
    public boolean reject(String candidateId) { return kernel.rejectCandidate(candidateId).isPresent(); }

    @Override
    public boolean assess(String assessorId, String candidateId, String verdict, double confidence, String explanation) {
        try {
            assessFull(assessorId, candidateId, verdict, confidence, "", "", explanation);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Utilities ──────────────────────────────────────────────

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
