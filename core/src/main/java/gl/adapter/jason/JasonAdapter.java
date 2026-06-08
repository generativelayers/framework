package gl.adapter.jason;

import gl.adapter.DirectAdapter;
import gl.adapter.ResourceActions;

/**
 * Jason platform adapter for the Generative Layers framework.
 *
 * <p>Thread-safe singleton that provides {@link DirectAdapter} access
 * to Jason Internal Actions (in {@code gl.adapter.jason.actions}).
 *
 * <p>Normal Jason usage is through the existing internal actions:
 * <pre>
 *   gl.adapter.jason.actions.use_provider("gemini");
 *   gl.adapter.jason.actions.ask("agent", "goal", "Classify: apple", ResultId);
 * </pre>
 *
 * <p>Usage from Jason Internal Actions:
 * <pre>
 *   ResourceActions gl = JasonAdapter.instance();
 *   gl.use_provider("gemini");
 *   String resultId = gl.ask("agent", "goal", "Classify: apple");
 * </pre>
 */
public final class JasonAdapter {

    private static final DirectAdapter INSTANCE = new DirectAdapter();

    private JasonAdapter() {}

    /** Get the shared adapter instance for all Jason agents. */
    public static ResourceActions instance() { return INSTANCE; }
}
