package gl.adapters.jason;

import gl.adapters.DirectAdapter;
import gl.adapters.ResourceActions;
import gl.provider.ProviderConfig;

/**
 * Thread-safe singleton implementing {@link ResourceActions} for Jason agents.
 *
 * <p>Jason Internal Actions (in {@code gl.adapters.jason.actions}) delegate
 * every command to this singleton, which in turn delegates to a
 * {@link DirectAdapter}. This ensures command parity with the ASTRA
 * adapter — same names, same parameter counts, same types.
 *
 * <p>Implements {@code ResourceActions} directly, so the compiler enforces
 * that every command from the contract is available.
 */
public final class GLAdapterSingleton implements ResourceActions {

    private static final GLAdapterSingleton INSTANCE = new GLAdapterSingleton();

    private volatile DirectAdapter adapter = new DirectAdapter();
    private final ProviderConfig.Builder configBuilder = new ProviderConfig.Builder();

    private GLAdapterSingleton() {}

    /** Get the singleton instance. */
    public static GLAdapterSingleton instance() { return INSTANCE; }

    // ── ResourceActions implementation ─────────────────────────

    @Override
    public synchronized boolean configure(String key, String value) {
        configBuilder.set(key, value);
        return true;
    }

    @Override
    public synchronized boolean use_provider() {
        ProviderConfig config = configBuilder.build();
        adapter = DirectAdapter.withConfig(config);
        System.out.println("[GL-Jason] Provider activated: " + config);
        return true;
    }

    @Override
    public synchronized boolean use_provider(String providerName) {
        configBuilder.set("provider", providerName);
        return use_provider();
    }

    @Override
    public synchronized boolean use_provider(String providerName, String model) {
        configBuilder.set("provider", providerName);
        configBuilder.set("model", model);
        return use_provider();
    }

    @Override
    public String providers() { return adapter.providers(); }

    @Override
    public String invoke(String agentId, String goalId,
                         String bodyId, String affordance,
                         String prompt, String requiredCsv) {
        return adapter.invoke(agentId, goalId, bodyId, affordance, prompt, requiredCsv);
    }

    @Override
    public String ask(String agentId, String goalId, String prompt) {
        return adapter.ask(agentId, goalId, prompt);
    }

    @Override
    public boolean valid(String resultId) { return adapter.valid(resultId); }

    @Override
    public String field(String resultId, String fieldName) { return adapter.field(resultId, fieldName); }

    @Override
    public String candidate(String resultId) { return adapter.candidate(resultId); }

    @Override
    public String trace(String resultId) { return adapter.trace(resultId); }

    @Override
    public String outcome(String resultId) { return adapter.outcome(resultId); }

    @Override
    public boolean admissible(String candidateId) { return adapter.admissible(candidateId); }

    @Override
    public boolean accept(String candidateId) { return adapter.accept(candidateId); }

    @Override
    public boolean reject(String candidateId) { return adapter.reject(candidateId); }

    @Override
    public boolean assess(String assessorId, String candidateId,
                          String verdict, double confidence,
                          String explanation) {
        return adapter.assess(assessorId, candidateId, verdict, confidence, explanation);
    }
}
