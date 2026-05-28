package gl.adapters.jason.actions;

import gl.adapters.jason.Adapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapters.jason.actions.configure(Key, Value)}.
 *
 * <p>Delegates to {@link gl.adapters.ResourceActions#configure(String, String)}.
 */
public class configure extends DefaultInternalAction {

    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String key = ((StringTerm) args[0]).getString();
        String value = ((StringTerm) args[1]).getString();
        return Adapter.instance().configure(key, value);
    }
}
