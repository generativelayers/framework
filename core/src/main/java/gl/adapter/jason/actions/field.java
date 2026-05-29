package gl.adapter.jason.actions;

import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapter.jason.actions.field(ResultId, FieldName, Value)}.
 *
 * <p>Delegates to {@link gl.adapter.ResourceActions#field(String, String)}.
 */
public class field extends DefaultInternalAction {

    @Override public int getMinArgs() { return 3; }
    @Override public int getMaxArgs() { return 3; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String resultId  = ((StringTerm) args[0]).getString();
        String fieldName = ((StringTerm) args[1]).getString();
        String value = JasonAdapter.instance().field(resultId, fieldName);
        return un.unifies(new StringTermImpl(value), args[2]);
    }
}
