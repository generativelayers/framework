package gl.adapter.jason.actions;

import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapter.jason.actions.trace(ResultId, TraceId)}.
 *
 * <p>Delegates to {@link gl.adapter.ResourceActions#trace(String)}.
 */
public class trace extends DefaultInternalAction {

    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String resultId = ((StringTerm) args[0]).getString();
        String traceId = JasonAdapter.instance().trace(resultId);
        return un.unifies(new StringTermImpl(traceId), args[1]);
    }
}
