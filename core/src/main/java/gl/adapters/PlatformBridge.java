package gl.adapters;

import gl.provider.ProviderConfig;

/**
 * Shared bridge between platform-specific adapters and the core
 * {@link DirectAdapter}.
 *
 * <p>Manages the {@link ProviderConfig.Builder} lifecycle and
 * {@link DirectAdapter} instantiation — logic that every platform
 * adapter (ASTRA, Jason, or any future platform) needs identically.
 *
 * <p>Platform adapters hold a {@code PlatformBridge} instance and
 * delegate all {@link ResourceActions} calls through it, ensuring
 * identical behaviour regardless of host platform.
 *
 * <h3>Architecture</h3>
 * <pre>
 *   ResourceActions (interface)        — canonical 14-command contract
 *       ↑ implements
 *   AdapterBase (abstract)             — kernel + body wiring
 *       ↑ extends
 *   DirectAdapter (concrete)           — in-process adapter
 *       ↑ held by
 *   PlatformBridge (this class)        — provider lifecycle, shared state
 *       ↑ used by
 *   ├── Adapter  (gl.adapters.astra)
 *   └── Adapter  (gl.adapters.jason)
 * </pre>
 */
public final class PlatformBridge implements ResourceActions {

    private volatile DirectAdapter adapter;
    private final ProviderConfig.Builder configBuilder = new ProviderConfig.Builder();
    private final String platformTag;

    public PlatformBridge(String platformTag) {
        this.platformTag = platformTag;
        this.adapter = new DirectAdapter();
    }

    // ── Provider lifecycle ──────────────────────────────────────

    @Override
    public synchronized boolean configure(String key, String value) {
        configBuilder.set(key, value);
        return true;
    }

    @Override
    public synchronized boolean use_provider() {
        ProviderConfig config = configBuilder.build();
        adapter = DirectAdapter.withConfig(config);
        System.out.println("[GL:" + platformTag + "] Provider activated: " + config);
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

    // ── Generative body invocation ──────────────────────────────

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

    // ── Result inspection ──────────────────────────────────────

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

    // ── Candidate deliberation ─────────────────────────────────

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
