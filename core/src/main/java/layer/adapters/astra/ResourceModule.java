package layer.adapters.astra;

import astra.core.Module;
import layer.adapters.DirectAdapter;
import layer.provider.ProviderConfig;
import layer.provider.ProviderRegistry;

import java.util.Set;

/**
 * ASTRA adapter for the Generative Resource Layer.
 *
 * <p>Translates between ASTRA's {@code @ACTION}/{@code @TERM} conventions
 * and the platform-agnostic {@link DirectAdapter}.
 *
 * <p>Usage in .astra files:
 * <pre>
 *   module layer.adapters.astra.ResourceModule layer;
 *
 *   rule +!main(list args) {
 *       layer.useProvider("gemini");
 *       string resultId = layer.invoke("agent", "goal", "llm.answer",
 *                                     "ANSWER", "prompt", "field1,field2");
 *       boolean isValid = layer.valid(resultId);
 *       if (isValid) { layer.accept(layer.candidate(resultId)); }
 *   }
 * </pre>
 */
public class ResourceModule extends Module {

    private DirectAdapter adapter = new DirectAdapter();
    private ProviderConfig.Builder configBuilder = new ProviderConfig.Builder();

    // ── Provider configuration ─────────────────────────────────

    /** Set a configuration key (provider, model, temperature, maxTokens, endpoint, apiKeyEnv). */
    @ACTION
    public boolean configure(String key, String value) {
        configBuilder.set(key, value);
        return true;
    }

    /** Activate the provider with the current configuration. */
    @ACTION
    public boolean useProvider() {
        ProviderConfig config = configBuilder.build();
        adapter = DirectAdapter.withConfig(config);
        System.out.println("[ResourceModule] Provider activated: " + config);
        return true;
    }

    /** Shorthand: activate by provider name (uses defaults for other settings). */
    @ACTION
    public boolean useProvider(String providerName) {
        configBuilder.set("provider", providerName);
        return useProvider();
    }

    /** Shorthand: activate by provider name + model. */
    @ACTION
    public boolean useProvider(String providerName, String model) {
        configBuilder.set("provider", providerName);
        configBuilder.set("model", model);
        return useProvider();
    }

    /** Return comma-separated list of available provider names. */
    @TERM
    public String providers() {
        return String.join(",", ProviderRegistry.available());
    }

    // ── @TERM — return values ──────────────────────────────────

    @TERM
    public String invoke(String agentId, String goalId,
                         String bodyId, String affordance,
                         String prompt, String requiredCsv) {
        return adapter.invoke(agentId, goalId, bodyId, affordance, prompt, requiredCsv);
    }

    @TERM
    public String ask(String agentId, String goalId, String prompt) {
        return adapter.ask(agentId, goalId, prompt);
    }

    @TERM
    public boolean valid(String resultId) {
        return adapter.validResult(resultId);
    }

    @TERM
    public String field(String resultId, String fieldName) {
        return adapter.resultField(resultId, fieldName);
    }

    @TERM
    public String candidate(String resultId) {
        return adapter.candidateId(resultId);
    }

    @TERM
    public String trace(String resultId) {
        return adapter.traceId(resultId);
    }

    @TERM
    public String outcome(String resultId) {
        return adapter.outcomeName(resultId);
    }

    @TERM
    public boolean admissible(String candidateId) {
        return adapter.admissibleCandidate(candidateId);
    }

    // ── @ACTION — side-effects ─────────────────────────────────

    @ACTION
    public boolean accept(String candidateId) {
        return adapter.acceptCandidate(candidateId);
    }

    @ACTION
    public boolean reject(String candidateId) {
        return adapter.rejectCandidate(candidateId);
    }

    @ACTION
    public boolean assess(String assessorId, String candidateId,
                          String verdict, double confidence,
                          String explanation) {
        try {
            adapter.assessCandidate(assessorId, candidateId, verdict, confidence, "", "", explanation);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
