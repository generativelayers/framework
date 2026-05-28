package gl.adapters.jason;

import gl.adapters.PlatformBridge;
import gl.adapters.ResourceActions;

/**
 * Jason platform adapter for the Generative Layers framework.
 *
 * <p>Thread-safe singleton that exposes the shared {@link PlatformBridge}
 * to Jason Internal Actions (in {@code gl.adapters.jason.actions}).
 * All command logic lives in {@code PlatformBridge} — this class only
 * provides singleton access.
 *
 * <p>Symmetric counterpart: {@link gl.adapters.astra.Adapter}.
 *
 * <p>Usage from Jason Internal Actions:
 * <pre>
 *   ResourceActions bridge = Adapter.instance();
 *   bridge.use_provider("gemini");
 *   String resultId = bridge.invoke("agent", "goal", "llm.answer",
 *                                    "ANSWER", "prompt", "field1,field2");
 * </pre>
 */
public final class Adapter {

    private static final PlatformBridge BRIDGE = new PlatformBridge("jason");

    private Adapter() {}

    /**
     * Get the shared {@link ResourceActions} instance for all Jason agents.
     *
     * <p>Jason Internal Actions are JVM singletons, so all agents share
     * this bridge. This mirrors the per-agent {@code Adapter}
     * pattern but uses a static holder instead of per-agent instantiation.
     */
    public static ResourceActions instance() {
        return BRIDGE;
    }
}
