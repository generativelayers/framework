package gl.GovernanceKernel;

import java.time.Instant;
import java.util.Map;

public record Blob(String blobId, BlobType type, String content, String sha256, Map<String, String> metadata, Instant createdAt) {
    public Blob {
        blobId = blobId == null || blobId.isBlank() ? Ids.id("blob") : blobId;
        type = type == null ? BlobType.GENERATIVE_OUTPUT : type;
        content = content == null ? "" : content;
        sha256 = sha256 == null || sha256.isBlank() ? Ids.sha256(content) : sha256;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        createdAt = createdAt == null ? Ids.now() : createdAt;
    }
    public static Blob of(BlobType type, String content, Map<String, String> metadata) {
        return new Blob(Ids.id("blob"), type, content, null, metadata, Ids.now());
    }
}
