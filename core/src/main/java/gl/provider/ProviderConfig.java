package gl.provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration for a generative provider.
 *
 * <p>Holds all parameters needed to create any provider:
 * name, model, temperature, token limits, API key, endpoint, etc.
 * Provider implementations read the fields they need and ignore the rest.
 *
 * <p>Usage from ASTRA:
 * <pre>
 *   gl.configure("provider", "gemini");
 *   gl.configure("model", "gemini-2.5-flash");
 *   gl.configure("temperature", "0.2");
 *   gl.use_provider();
 * </pre>
 */
public final class ProviderConfig {
    private final Map<String, String> values;

    private ProviderConfig(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    /** Create an empty config. */
    public static ProviderConfig empty() {
        return new ProviderConfig(Map.of());
    }

    /** Create a config with a single provider name. */
    public static ProviderConfig of(String provider) {
        return new Builder().set("provider", provider).build();
    }

    // ── Typed accessors ─────────────────────────────────────────

    public String provider()      { return get("provider", "fake"); }
    public String model()         { return get("model", ""); }
    public double temperature()   { return getDouble("temperature", 0.2); }
    public int maxTokens()        { return getInt("maxTokens", 1024); }
    public String endpoint()      { return get("endpoint", ""); }

    /**
     * Resolve the API key. Reads the value of the env var named
     * by the {@code apiKeyEnv} config field (default: provider-specific).
     */
    public String apiKey() {
        String envVar = get("apiKeyEnv", "");
        if (envVar.isEmpty()) {
            // Convention: PROVIDER_API_KEY  (e.g. GEMINI_API_KEY)
            envVar = provider().toUpperCase() + "_API_KEY";
        }
        String key = System.getenv(envVar);
        return key == null ? "" : key;
    }

    /** Raw access to any config value. */
    public String get(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        String v = values.get(key);
        if (v == null || v.isBlank()) return defaultValue;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return defaultValue; }
    }

    public int getInt(String key, int defaultValue) {
        String v = values.get(key);
        if (v == null || v.isBlank()) return defaultValue;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return defaultValue; }
    }

    /** Return a new config with an additional/overridden key. */
    public ProviderConfig with(String key, String value) {
        Map<String, String> copy = new LinkedHashMap<>(values);
        copy.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
        return new ProviderConfig(copy);
    }

    public Map<String, String> asMap() { return values; }

    @Override
    public String toString() { return "ProviderConfig" + values; }

    // ── Builder ─────────────────────────────────────────────────

    public static class Builder {
        private final Map<String, String> values = new LinkedHashMap<>();

        public Builder set(String key, String value) {
            values.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
            return this;
        }

        public boolean has(String key) { return values.containsKey(key); }

        public ProviderConfig build() { return new ProviderConfig(values); }
    }
}
