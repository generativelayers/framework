package gl.adapter.jason.actions;

import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapter.jason.actions.providers(Result)}.
 *
 * <p>Delegates to {@link gl.adapter.ResourceActions#providers()}.
 */
public class providers extends DefaultInternalAction {

    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 1; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String result = JasonAdapter.instance().providers();
        return un.unifies(new StringTermImpl(result), args[0]);
    }
}
