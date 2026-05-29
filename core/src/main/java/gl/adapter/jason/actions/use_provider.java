package gl.adapter.jason.actions;

import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action for provider configuration.
 *
 * <p>Three forms:
 * <ul>
 *   <li>{@code gl.actions.use_provider} — auto-detect from {@code GL_PROVIDER} /
 *       {@code GL_MODEL} environment variables (default: "fake")</li>
 *   <li>{@code gl.actions.use_provider("gemini")} — explicit provider</li>
 *   <li>{@code gl.actions.use_provider("gemini", "gemini-2.5-flash")} — provider + model</li>
 * </ul>
 */
public class use_provider extends DefaultInternalAction {

    @Override public int getMinArgs() { return 0; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (args.length == 0) {
            // Auto-detect from environment
            String provider = System.getenv("GL_PROVIDER");
            if (provider == null || provider.isBlank()) provider = "fake";
            String model = System.getenv("GL_MODEL");
            if (model != null && !model.isBlank()) {
                return JasonAdapter.instance().use_provider(provider, model);
            } else {
                return JasonAdapter.instance().use_provider(provider);
            }
        } else if (args.length == 1) {
            checkArguments(args);
            return JasonAdapter.instance().use_provider(((StringTerm) args[0]).getString());
        } else {
            checkArguments(args);
            return JasonAdapter.instance().use_provider(
                    ((StringTerm) args[0]).getString(),
                    ((StringTerm) args[1]).getString());
        }
    }
}
