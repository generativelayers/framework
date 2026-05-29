/**
 * Generative Layers -- Provider SPI.
 *
 * <p>Pluggable LLM backend integration. The framework ships with two
 * built-in providers and supports custom ones via registration.
 *
 * <h2>Built-in Providers</h2>
 * <ul>
 *   <li>{@link gl.provider.GeminiProvider} -- Google Gemini REST API
 *       (default model: gemini-2.5-flash)</li>
 *   <li>{@link gl.provider.OpenAiProvider} -- OpenAI-compatible REST API
 *       (works with OpenAI, Azure, Ollama, etc.)</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li>{@link gl.provider.ProviderConfig} -- Immutable configuration:
 *       provider name, model, temperature, max tokens, API key env var</li>
 *   <li>{@link gl.provider.ProviderRegistry} -- Factory registry:
 *       resolves provider name → instance via reflection or custom factory</li>
 * </ul>
 *
 * <h2>Usage from Agent Code</h2>
 * <pre>
 *   // ASTRA
 *   gl.use_provider("gemini", "gemini-2.5-flash");
 *
 *   // Java
 *   ProviderRegistry.register("my-llm", config -&gt; new MyProvider(config));
 * </pre>
 *
 * <h2>Adding a Custom Provider</h2>
 * <p>Implement {@link gl.KernelPorts.GenerativeProvider} and register
 * it with {@link gl.provider.ProviderRegistry#register}.
 *
 * @see gl Core framework (governance engine, domain types)
 * @see gl.adapter Platform adapters (ASTRA, Jason)
 */
package gl.provider;
