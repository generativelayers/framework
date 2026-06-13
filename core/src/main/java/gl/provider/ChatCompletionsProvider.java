package gl.provider;

import gl.model.Blob;
import gl.KernelPorts;
import gl.model.ProviderOutput;
import gl.model.ResourceRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Universal provider for any service implementing the Chat Completions API.
 *
 * <p>This REST API format -- originally defined by OpenAI -- is now an industry
 * standard adopted by Groq, Cerebras, DeepSeek, Together AI, Fireworks,
 * Ollama, LM Studio, and many others. This single class handles them all.
 *
 * <h3>Usage -- Known provider (GL v2):</h3>
 * <pre>
 *   String bid = gl.bind("agent1", "groq", "llama-3.3-70b-versatile", "");
 * </pre>
 *
 * <h3>Usage -- Custom endpoint (GL v2):</h3>
 * <pre>
 *   String bid = gl.bind("agent1", "chatcompletions", "any-model",
 *       "endpoint=https://api.any-provider.com/v1/chat/completions,apiKeyEnv=ANY_API_KEY");
 * </pre>
 *
 * <h3>Known aliases (convenience shortcuts):</h3>
 * <table>
 *   <tr><th>Name</th><th>Endpoint</th><th>Default key env var</th></tr>
 *   <tr><td>openai</td><td>api.openai.com</td><td>OPENAI_API_KEY</td></tr>
 *   <tr><td>groq</td><td>api.groq.com</td><td>GROQ_API_KEY</td></tr>
 *   <tr><td>cerebras</td><td>api.cerebras.ai</td><td>CEREBRAS_API_KEY</td></tr>
 *   <tr><td>deepseek</td><td>api.deepseek.com</td><td>DEEPSEEK_API_KEY</td></tr>
 * </table>
 */
public final class ChatCompletionsProvider implements KernelPorts.GenerativeProvider {

    // -- Known provider aliases > default endpoints --------------
    // Adding a new provider here is optional -- users can always
    // supply a custom endpoint via bind("agent", "chatcompletions", "model", "endpoint=...").

    private static final Map<String, String> KNOWN_ENDPOINTS = Map.of(
            "openai",   "https://api.openai.com/v1/chat/completions",
            "groq",     "https://api.groq.com/openai/v1/chat/completions",
            "cerebras", "https://api.cerebras.ai/v1/chat/completions",
            "deepseek", "https://api.deepseek.com/chat/completions"
    );

    /** Shared across all instances — Java HttpClient is designed for reuse. */
    private static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20)).build();

    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final String providerName;
    private final double temperature;

    public ChatCompletionsProvider(ProviderConfig config) {
        this.model = config.model();
        this.temperature = config.temperature();
        this.apiKey = config.apiKey();
        this.providerName = config.provider().isEmpty() ? "chatcompletions" : config.provider();

        // Resolve endpoint: user-configured > known alias > error
        String ep = config.endpoint();
        if (ep.isEmpty()) {
            ep = KNOWN_ENDPOINTS.getOrDefault(providerName, "");
        }
        if (ep.isEmpty()) {
            throw new IllegalStateException(
                    "[GL] No endpoint configured for provider '" + providerName + "'. "
                  + "Either use a known name (" + KNOWN_ENDPOINTS.keySet() + ") "
                  + "or set a custom endpoint in bind() config: \"endpoint=https://...\"");
        }
        this.endpoint = URI.create(ep);
    }

    // -- GenerativeProvider implementation ------------------------

    @Override
    public ProviderOutput generate(ResourceRequest request, Blob promptBlob)
            throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "[GL] API key not set for " + providerName + ". "
                  + "Set env var " + providerName.toUpperCase() + "_API_KEY, "
                  + "or pass apiKeyEnv in bind() config");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                    "[GL] No model specified. "
                  + "Pass model name in bind(): gl.bind(agent, provider, \"model-name\", config)");
        }

        List<String> required = request.schema() != null ? request.schema().requiredFields() : List.of();
        String jsonSchema;
        if (required.isEmpty()) {
            jsonSchema = "{ \"answer\": null }";
        } else {
            StringBuilder sb = new StringBuilder("{ ");
            for (int i = 0; i < required.size(); i++) {
                sb.append("\"").append(required.get(i)).append("\": null");
                if (i < required.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(" }");
            jsonSchema = sb.toString();
        }

        String systemInstruction =
                "You are a structured data extraction tool for a BDI agent system. "
              + "You MUST respond strictly in valid JSON format. "
              + "Do NOT include any conversational introduction, explanation, or markdown formatting (do not wrap in ```json). "
              + "Output ONLY the raw JSON string. "
              + "Always keep the exact same keys as the JSON schema.\n\n"
              + "JSON schema: " + jsonSchema;

        String userPrompt = "Input: " + request.prompt() + "\nJSON Response:";
        String body = "{\"model\":\"" + ProviderUtils.escape(model)
                + "\",\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + ProviderUtils.escape(systemInstruction) + "\"}"
                + ",{\"role\":\"user\",\"content\":\"" + ProviderUtils.escape(userPrompt) + "\"}"
                + "],\"temperature\":" + temperature + "}";

        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = null;
        int maxRetries = 4;
        long[] waitSeconds = {15, 30, 45, 60};
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            response = SHARED_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429 && attempt < maxRetries) {
                System.err.println("[ChatCompletionsProvider] Rate limited (429). "
                        + "Waiting " + waitSeconds[attempt] + "s "
                        + "(attempt " + (attempt + 1) + "/" + maxRetries + ")");
                Thread.sleep(waitSeconds[attempt] * 1000);
            } else {
                break;
            }
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("provider HTTP " + response.statusCode()
                    + ": " + response.body().substring(0,
                    Math.min(300, response.body().length())));
        }

        String text = ProviderUtils.extractJsonString(response.body(), "\"content\":");

        // Strip markdown code fences if present
        if (text != null && text.startsWith("```")) {
            text = text.replaceAll("^```[a-zA-Z]*\\s*", "")
                       .replaceAll("```\\s*$", "")
                       .trim();
        }

        return new ProviderOutput(providerName, model,
                text != null ? text : response.body(),
                Map.of("status", Integer.toString(response.statusCode())));
    }

    /** Factory method -- called by {@link ProviderRegistry} via reflection. */
    public static KernelPorts.GenerativeProvider create(ProviderConfig config) {
        return new ChatCompletionsProvider(config);
    }
}
