package gl.provider;

import gl.model.Blob;
import gl.KernelPorts;
import gl.model.ProviderOutput;
import gl.model.ResourceRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Gemini REST API provider for the Generative Resource gl.
 *
 * <p>Uses direct {@link java.net.http.HttpClient} calls to the Gemini
 * {@code generateContent} endpoint. No external LLM library dependency.
 *
 * <p>Configuration (via {@link ProviderConfig}):
 * <ul>
 *   <li>{@code model} -- Gemini model name (default: gemini-2.5-flash)</li>
 *   <li>{@code temperature} -- sampling temperature (default: 0.2)</li>
 *   <li>{@code maxTokens} -- max output tokens (default: 1024)</li>
 *   <li>{@code endpoint} -- API base URL (default: generativelanguage.googleapis.com)</li>
 *   <li>{@code apiKeyEnv} -- env var holding the API key (default: GEMINI_API_KEY)</li>
 * </ul>
 *
 * <p>Includes retry with backoff for 429 rate-limit responses.
 */
public final class GeminiProvider implements KernelPorts.GenerativeProvider {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com";

    private final HttpClient client;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final URI baseUrl;

    public GeminiProvider(ProviderConfig config) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.model = config.model().isEmpty() ? DEFAULT_MODEL : config.model();
        this.temperature = config.temperature();
        this.maxTokens = config.maxTokens();
        this.apiKey = config.apiKey();
        String endpoint = config.endpoint().isEmpty() ? DEFAULT_ENDPOINT : config.endpoint();
        this.baseUrl = URI.create(endpoint);
    }

    /**
     * Factory method -- called by {@link gl.provider.ProviderRegistry} via reflection.
     * Every built-in provider must implement this convention.
     */
    public static KernelPorts.GenerativeProvider create(ProviderConfig config) {
        return new GeminiProvider(config);
    }

    /** Convenience: create from environment using defaults. */
    public static GeminiProvider fromEnvironment() {
        return new GeminiProvider(
                new ProviderConfig.Builder()
                        .set("provider", "gemini")
                        .set("apiKeyEnv", "GEMINI_API_KEY")
                        .build()
        );
    }

    @Override
    public ProviderOutput generate(ResourceRequest request, Blob promptBlob) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not set. Export it: export GEMINI_API_KEY=\"your-key\"");
        }

        String systemInstruction =
                "You are a structured data extraction tool for a BDI agent system. "
              + "You MUST respond strictly in valid JSON format. "
              + "Do NOT include any conversational introduction, explanation, or markdown formatting (do not wrap in ```json). "
              + "Output ONLY the raw JSON string. "
              + "Example JSON format:\\n{\\n  \"label\": \"fruit\",\\n  \"confidence\": 0.95\\n}";

        String body = buildRequestBody(systemInstruction, request.prompt());

        URI url = baseUrl.resolve(
                "/v1beta/models/" + model + ":generateContent");

        HttpRequest httpRequest = HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        // Retry with backoff for rate limits (429)
        HttpResponse<String> resp = null;
        int maxRetries = 4;
        long[] waitSeconds = {15, 30, 45, 60};
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            resp = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 429 && attempt < maxRetries) {
                System.err.println("[GeminiProvider] Rate limited (429). "
                        + "Waiting " + waitSeconds[attempt] + "s "
                        + "(attempt " + (attempt + 1) + "/" + maxRetries + ")");
                Thread.sleep(waitSeconds[attempt] * 1000);
            } else {
                break;
            }
        }

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new java.io.IOException("Gemini HTTP " + resp.statusCode()
                    + ": " + resp.body().substring(0,
                    Math.min(300, resp.body().length())));
        }

        String text = ProviderUtils.extractJsonString(resp.body(), "\"text\":");
        if (text == null || text.isBlank()) {
            throw new java.io.IOException("No candidate text in Gemini response");
        }

        // Strip markdown code fences if present
        if (text.startsWith("```")) {
            text = text.replaceAll("^```[a-zA-Z]*\\s*", "")
                       .replaceAll("```\\s*$", "")
                       .trim();
        }

        return new ProviderOutput("gemini", model, text.trim(),
                Map.of("status", Integer.toString(resp.statusCode()),
                       "temperature", Double.toString(temperature)));
    }

    private String buildRequestBody(String systemInstruction, String userPrompt) {
        return "{"
             + "\"system_instruction\":{\"parts\":[{\"text\":\"" + ProviderUtils.escape(systemInstruction) + "\"}]},"
             + "\"contents\":[{\"parts\":[{\"text\":\"" + ProviderUtils.escape(userPrompt) + "\"}]}],"
             + "\"generationConfig\":{\"temperature\":" + temperature
             + ",\"maxOutputTokens\":" + maxTokens + "}"
             + "}";
    }
}
