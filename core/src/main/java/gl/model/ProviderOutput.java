package gl.model;

import java.util.Map;

/** Raw output from a generative provider (LLM).
 *  Contains the provider name, model used, raw text response,
 *  and provider-specific metadata (e.g. temperature, status code). */
public record ProviderOutput(String providerName, String model, String rawText, Map<String, String> metadata) {
    public ProviderOutput {
        providerName = providerName == null || providerName.isBlank() ? "unknown" : providerName;
        model = model == null ? "" : model;
        rawText = rawText == null ? "" : rawText;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
