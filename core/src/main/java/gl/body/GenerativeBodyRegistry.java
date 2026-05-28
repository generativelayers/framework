package gl.body;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class GenerativeBodyRegistry {
    private final ConcurrentHashMap<String, GenerativeBody> bodies = new ConcurrentHashMap<>();

    public GenerativeBody register(GenerativeBody body) {
        bodies.put(body.descriptor().bodyId(), body);
        return body;
    }

    public Optional<GenerativeBody> get(String bodyId) {
        return Optional.ofNullable(bodies.get(bodyId));
    }

    public GenerativeBody require(String bodyId) {
        return get(bodyId).orElseThrow(() -> new IllegalArgumentException("unknown generative body: " + bodyId));
    }

    public List<BodyDescriptor> descriptors() {
        return bodies.values().stream().map(GenerativeBody::descriptor).toList();
    }
}
