package layer.provider.openai;

import layer.kernel.Blob;
import layer.kernel.KernelPorts;
import layer.kernel.ProviderOutput;
import layer.kernel.ResourceRequest;
import layer.provider.ProviderConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * OpenAI-compatible REST API provider.
 *
 * <p>Works with OpenAI, Azure OpenAI, Ollama, LM Studio, and any
 * provider that implements the OpenAI Chat Completions API.
 *
 * <p>Configuration (via {@link ProviderConfig}):
 * <ul>
 *   <li>{@code model} — model name (default: gpt-4o-mini)</li>
 *   <li>{@code temperature} — sampling temperature (default: 0.0)</li>
 *   <li>{@code endpoint} — API URL (default: https://api.openai.com/v1/chat/completions)</li>
 *   <li>{@code apiKeyEnv} — env var holding the API key (default: OPENAI_API_KEY)</li>
 * </ul>
 */
public final class OpenAiCompatibleProvider implements KernelPorts.GenerativeProvider {
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final HttpClient client;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final double temperature;

    public OpenAiCompatibleProvider(ProviderConfig config) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.model = config.model().isEmpty() ? DEFAULT_MODEL : config.model();
        this.temperature = config.temperature();
        this.apiKey = config.apiKey();
        String ep = config.endpoint().isEmpty() ? DEFAULT_ENDPOINT : config.endpoint();
        this.endpoint = URI.create(ep);
    }

    /** Legacy constructor for backward compatibility. */
    public OpenAiCompatibleProvider(URI endpoint, String apiKey, String model) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
        this.temperature = 0.0;
    }

    /**
     * Factory method — called by {@link layer.provider.ProviderRegistry} via reflection.
     */
    public static KernelPorts.GenerativeProvider create(ProviderConfig config) {
        return new OpenAiCompatibleProvider(config);
    }

    public static OpenAiCompatibleProvider fromEnvironment() {
        return new OpenAiCompatibleProvider(
                new ProviderConfig.Builder()
                        .set("provider", "openai")
                        .set("apiKeyEnv", "OPENAI_API_KEY")
                        .build()
        );
    }

    @Override
    public ProviderOutput generate(ResourceRequest request, Blob promptBlob) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OpenAI API key is not set");
        String body = "{\"model\":\"" + escape(model) + "\",\"messages\":[{\"role\":\"system\",\"content\":\"Return concise key=value lines where possible.\"},{\"role\":\"user\",\"content\":\"" + escape(request.prompt()) + "\"}],\"temperature\":" + temperature + "}";
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("provider HTTP " + response.statusCode() + ": " + response.body());
        return new ProviderOutput("openai-compatible", model, extractContent(response.body()), Map.of("status", Integer.toString(response.statusCode())));
    }

    private static String extractContent(String json) {
        String marker = "\"content\":";
        int idx = json.indexOf(marker);
        if (idx < 0) return json;
        int start = json.indexOf('"', idx + marker.length());
        if (start < 0) return json;
        StringBuilder out = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                out.append(switch (c) { case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t'; default -> c; });
                escape = false;
            } else if (c == '\\') { escape = true; }
            else if (c == '"') { break; }
            else { out.append(c); }
        }
        return out.toString();
    }

    private static String escape(String text) {
        return (text == null ? "" : text).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
