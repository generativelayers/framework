package gl.GovernanceKernel;

import java.util.Map;

public record ProviderOutput(String providerName, String model, String rawText, Map<String, String> metadata) {
    public ProviderOutput {
        providerName = providerName == null || providerName.isBlank() ? "unknown" : providerName;
        model = model == null ? "" : model;
        rawText = rawText == null ? "" : rawText;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
