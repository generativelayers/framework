package gl.adapters.astra;

import astra.core.Module;
import gl.adapters.DirectAdapter;
import gl.adapters.ResourceActions;
import gl.provider.ProviderConfig;
import gl.provider.ProviderRegistry;

/**
 * ASTRA adapter for the Generative Layers framework.
 *
 * <p>Thin wrapper that exposes the {@link ResourceActions} contract
 * as ASTRA {@code @ACTION}/{@code @TERM} methods. Command names,
 * parameter counts, and types are identical to the Jason adapter.
 *
 * <p>Usage in .astra files:
 * <pre>
 *   module gl.adapters.astra.ResourceModule gl;
 *
 *   rule +!main(list args) {
 *       gl.use_provider("gemini");
 *       string resultId = gl.invoke("agent", "goal", "llm.answer",
 *                                     "ANSWER", "prompt", "field1,field2");
 *       boolean isValid = gl.valid(resultId);
 *       if (isValid) { gl.accept(gl.candidate(resultId)); }
 *   }
 * </pre>
 */
public class ResourceModule extends Module {

    private ResourceActions actions = new DirectAdapter();
    private ProviderConfig.Builder configBuilder = new ProviderConfig.Builder();

    // ── Provider configuration ─────────────────────────────────

    @ACTION
    public boolean configure(String key, String value) {
        configBuilder.set(key, value);
        return true;
    }

    @ACTION
    public boolean use_provider() {
        ProviderConfig config = configBuilder.build();
        actions = DirectAdapter.withConfig(config);
        System.out.println("[ResourceModule] Provider activated: " + config);
        return true;
    }

    @ACTION
    public boolean use_provider(String providerName) {
        configBuilder.set("provider", providerName);
        return use_provider();
    }

    @ACTION
    public boolean use_provider(String providerName, String model) {
        configBuilder.set("provider", providerName);
        configBuilder.set("model", model);
        return use_provider();
    }

    @TERM
    public String providers() {
        return actions.providers();
    }

    // ── Generative body invocation ─────────────────────────────

    @TERM
    public String invoke(String agentId, String goalId,
                         String bodyId, String affordance,
                         String prompt, String requiredCsv) {
        return actions.invoke(agentId, goalId, bodyId, affordance, prompt, requiredCsv);
    }

    @TERM
    public String ask(String agentId, String goalId, String prompt) {
        return actions.ask(agentId, goalId, prompt);
    }

    // ── Result inspection ──────────────────────────────────────

    @TERM
    public boolean valid(String resultId) {
        return actions.valid(resultId);
    }

    @TERM
    public String field(String resultId, String fieldName) {
        return actions.field(resultId, fieldName);
    }

    @TERM
    public String candidate(String resultId) {
        return actions.candidate(resultId);
    }

    @TERM
    public String trace(String resultId) {
        return actions.trace(resultId);
    }

    @TERM
    public String outcome(String resultId) {
        return actions.outcome(resultId);
    }

    // ── Candidate deliberation ─────────────────────────────────

    @TERM
    public boolean admissible(String candidateId) {
        return actions.admissible(candidateId);
    }

    @ACTION
    public boolean accept(String candidateId) {
        return actions.accept(candidateId);
    }

    @ACTION
    public boolean reject(String candidateId) {
        return actions.reject(candidateId);
    }

    @ACTION
    public boolean assess(String assessorId, String candidateId,
                          String verdict, double confidence,
                          String explanation) {
        return actions.assess(assessorId, candidateId, verdict, confidence, explanation);
    }
}
