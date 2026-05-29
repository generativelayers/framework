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
import java.util.Map;

/**
 * OpenAI-compatible REST API provider.
 *
 * <p>Works with OpenAI, Azure OpenAI, Ollama, LM Studio, and any
 * provider that implements the OpenAI Chat Completions API.
 *
 * <p>Configuration (via {@link ProviderConfig}):
 * <ul>
 *   <li>{@code model} -- model name (default: gpt-4o-mini)</li>
 *   <li>{@code temperature} -- sampling temperature (default: 0.0)</li>
 *   <li>{@code endpoint} -- API URL (default: https://api.openai.com/v1/chat/completions)</li>
 *   <li>{@code apiKeyEnv} -- env var holding the API key (default: GL_OPENAI_API_KEY)</li>
 * </ul>
 */
public final class OpenAiProvider implements KernelPorts.GenerativeProvider {
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final HttpClient client;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final double temperature;

    public OpenAiProvider(ProviderConfig config) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.model = config.model().isEmpty() ? DEFAULT_MODEL : config.model();
        this.temperature = config.temperature();
        this.apiKey = config.apiKey();
        String ep = config.endpoint().isEmpty() ? DEFAULT_ENDPOINT : config.endpoint();
        this.endpoint = URI.create(ep);
    }


    /**
     * Factory method -- called by {@link gl.provider.ProviderRegistry} via reflection.
     */
    public static KernelPorts.GenerativeProvider create(ProviderConfig config) {
        return new OpenAiProvider(config);
    }

    public static OpenAiProvider fromEnvironment() {
        return new OpenAiProvider(
                new ProviderConfig.Builder()
                        .set("provider", "openai")
                        .set("apiKeyEnv", "GL_OPENAI_API_KEY")
                        .build()
        );
    }

    @Override
    public ProviderOutput generate(ResourceRequest request, Blob promptBlob) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OpenAI API key is not set");
        String body = "{\"model\":\"" + ProviderUtils.escape(model) + "\",\"messages\":[{\"role\":\"system\",\"content\":\"Return concise key=value lines where possible.\"},{\"role\":\"user\",\"content\":\"" + ProviderUtils.escape(request.prompt()) + "\"}],\"temperature\":" + temperature + "}";
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("provider HTTP " + response.statusCode() + ": " + response.body());
        String text = ProviderUtils.extractJsonString(response.body(), "\"content\":");
        return new ProviderOutput("openai-compatible", model, text != null ? text : response.body(), Map.of("status", Integer.toString(response.statusCode())));
    }
}
