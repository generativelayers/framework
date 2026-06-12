package gl.provider;

import gl.KernelDefaults;
import gl.KernelPorts;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Abstract registry mapping provider names to factory functions.
 *
 * <p>This class is the <b>only</b> integration point between the abstract
 * framework and concrete provider implementations. It uses <b>reflection</b>
 * for built-in providers, so it never imports concrete classes at compile time.
 *
 * <h3>Built-in convention</h3>
 * Each built-in provider class must have a public static method:
 * <pre>
 *   public static GenerativeProvider create(ProviderConfig config)
 * </pre>
 *
 * <h3>Custom providers</h3>
 * <pre>
 *   ProviderRegistry.register("my-llm", config -&gt; new MyLlmProvider(config));
 * </pre>
 *
 * <h3>Usage from ASTRA (GL v2)</h3>
 * <pre>
 *   string bid = gl.bind("agent1", "gemini", "gemini-2.5-flash", "");
 *   string rid = gl.call(bid, "goal1", "llm.answer", "ANSWER", "prompt", "label", "");
 * </pre>
 */
public final class ProviderRegistry {
    private ProviderRegistry() {}

    // -- Custom factories (registered at runtime) ----------------

    private static final Map<String, Function<ProviderConfig, KernelPorts.GenerativeProvider>>
            CUSTOM = new ConcurrentHashMap<>();
    // -- Built-in provider class names (resolved via reflection) -
    // All Chat Completions API providers share one class.
    // Known aliases are defined inside ChatCompletionsProvider.KNOWN_ENDPOINTS.
    // Users can add any provider via bind("agent", "chatcompletions", "model", "endpoint=...").

    private static final String CHAT_COMPLETIONS = "gl.provider.ChatCompletionsProvider";

    private static final Map<String, String> BUILT_IN = Map.of(
            "gemini",           "gl.provider.GeminiProvider",
            "openai",           CHAT_COMPLETIONS,
            "groq",             CHAT_COMPLETIONS,
            "cerebras",         CHAT_COMPLETIONS,
            "deepseek",         CHAT_COMPLETIONS,
            "chatcompletions",  CHAT_COMPLETIONS
    );

    /**
     * Deterministic priority order for auto-detection.
     * Checked in this order when no explicit provider is configured.
     */
    private static final List<String> DETECTION_ORDER = List.of(
            "gemini", "cerebras", "groq", "openai", "deepseek"
    );

    /**
     * Register a custom provider factory by name.
     *
     * @param name    short name (e.g. "claude", "ollama")
     * @param factory function that takes a {@link ProviderConfig} and
     *                returns a {@link KernelPorts.GenerativeProvider}
     */
    public static void register(String name,
                                Function<ProviderConfig, KernelPorts.GenerativeProvider> factory) {
        CUSTOM.put(name.toLowerCase(), factory);
    }

    /**
     * Create a provider instance by name with the given configuration.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>"fake" / "deterministic" > built-in fake provider</li>
     *   <li>Custom registry (registered via {@link #register})</li>
     *   <li>Built-in class name map (resolved via reflection)</li>
     * </ol>
     *
     * @throws IllegalArgumentException if the provider name is unknown
     */
    public static KernelPorts.GenerativeProvider create(String name, ProviderConfig config) {
        String key = name.toLowerCase();

        // 1. Built-in fake
        if ("fake".equals(key) || "deterministic".equals(key)) {
            return new KernelDefaults.DeterministicFakeProvider();
        }

        // 2. Custom registry
        Function<ProviderConfig, KernelPorts.GenerativeProvider> custom = CUSTOM.get(key);
        if (custom != null) return custom.apply(config);

        // 3. Built-in via reflection
        String className = BUILT_IN.get(key);
        if (className != null) {
            return createViaReflection(className, config);
        }

        // 4. Unknown name -- fall back to ChatCompletionsProvider if user set an endpoint
        if (!config.endpoint().isEmpty()) {
            System.out.println("[GL] Using custom endpoint for '" + name + "'");
            return createViaReflection(CHAT_COMPLETIONS, config);
        }

        throw new IllegalArgumentException(
                "[GL] Unknown provider: '" + name + "'. "
              + "Available: " + available() + ". "
              + "Or set a custom endpoint in bind() config: \"endpoint=https://...\"");
    }

    /**
     * Resolve which provider to use based on config and environment.
     *
     * <p>If config explicitly names a provider (including "fake"), use that.
     * If no provider was set, auto-detect from env vars in priority order:
     * {@code GEMINI > CEREBRAS > GROQ > OPENAI > DEEPSEEK}.
     * Falls back to a deterministic fake provider for testing.
     */
    public static KernelPorts.GenerativeProvider resolve(ProviderConfig config) {
        // If provider was explicitly set in config, respect it -- even "fake"
        boolean explicitProvider = config.asMap().containsKey("provider");

        if (explicitProvider) {
            String name = config.provider();
            if ("fake".equals(name)) {
                System.out.println("[GL] Provider: deterministic fake (explicit)");
                return new KernelDefaults.DeterministicFakeProvider();
            }
            System.out.println("[GL] Provider: " + name
                    + (config.model().isEmpty() ? "" : " (" + config.model() + ")"));
            return create(name, config);
        }

        // Auto-detect from environment in deterministic priority order
        for (String name : DETECTION_ORDER) {
            String envVar = name.toUpperCase() + "_API_KEY";
            String val = System.getenv(envVar);
            if (val != null && !val.isBlank()) {
                System.out.println("[GL] Provider (auto-detected): " + name);
                return create(name, config.with("provider", name));
            }
        }

        System.out.println("[GL] Provider (fallback): deterministic fake");
        return new KernelDefaults.DeterministicFakeProvider();
    }

    /** Return the set of all available provider names. */
    public static Set<String> available() {
        Set<String> all = new java.util.TreeSet<>();
        all.add("fake");
        all.addAll(BUILT_IN.keySet());
        all.addAll(CUSTOM.keySet());
        return all;
    }

    // -- Reflection loader ---------------------------------------

    /**
     * Load a provider class by name and invoke its
     * {@code public static GenerativeProvider create(ProviderConfig)} method.
     */
    private static KernelPorts.GenerativeProvider createViaReflection(
            String className, ProviderConfig config) {
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod("create", ProviderConfig.class);
            return (KernelPorts.GenerativeProvider) method.invoke(null, config);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "[GL] Provider class not found: " + className
                  + ". Is the dependency on the classpath?", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "[GL] Provider " + className
                  + " missing: public static GenerativeProvider create(ProviderConfig)", e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "[GL] Failed to create provider " + className + ": " + e.getMessage(), e);
        }
    }
}
