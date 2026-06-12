package gl.adapter.jason;

import gl.adapter.DirectAdapter;
import gl.adapter.ResourceActions;

/**
 * Jason platform adapter for the GL v2 framework.
 *
 * <p>Thread-safe singleton that provides {@link DirectAdapter} access
 * to Jason Internal Actions (in {@code gl.adapter.jason.actions}).
 *
 * <p>GL v2 usage from Jason Internal Actions:
 * <pre>
 *   ResourceActions gl = JasonAdapter.instance();
 *   String bid = gl.bind("agent1", "gemini", "gemini-2.5-flash", "");
 *   String rid = gl.call(bid, "classify", "llm.answer", "ANSWER", "Classify: apple", "label", "");
 * </pre>
 */
public final class JasonAdapter {

    private static final DirectAdapter INSTANCE = new DirectAdapter();

    private JasonAdapter() {}

    /** Get the shared adapter instance for all Jason agents. */
    public static ResourceActions instance() { return INSTANCE; }
}
