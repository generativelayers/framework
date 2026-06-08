package gl.jason;

/**
 * Short public Jason internal-action alias for Generative Layers ask.
 *
 * <p>Normal Jason usage:
 * <pre>
 * gl.jason.ask("agent", "goal", "Classify: apple", ResultId);
 * </pre>
 *
 * <p>This is the public, concise form of the full implementation action:
 * <pre>
 * gl.adapter.jason.actions.ask("agent", "goal", "Classify: apple", ResultId);
 * </pre>
 */
public class ask extends gl.adapter.jason.actions.ask {
}
