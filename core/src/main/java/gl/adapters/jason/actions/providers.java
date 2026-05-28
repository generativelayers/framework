package gl.adapters.jason.actions;

import gl.adapters.jason.Adapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapters.jason.actions.providers(Result)}.
 *
 * <p>Delegates to {@link gl.adapters.ResourceActions#providers()}.
 */
public class providers extends DefaultInternalAction {

    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 1; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String result = Adapter.instance().providers();
        return un.unifies(new StringTermImpl(result), args[0]);
    }
}
