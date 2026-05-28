package gl.adapters.jason.actions;

import gl.adapters.jason.GLAdapterSingleton;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapters.jason.actions.field(ResultId, FieldName, Value)}.
 *
 * <p>Delegates to {@link gl.adapters.ResourceActions#field(String, String)}.
 */
public class field extends DefaultInternalAction {

    @Override public int getMinArgs() { return 3; }
    @Override public int getMaxArgs() { return 3; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String resultId  = ((StringTerm) args[0]).getString();
        String fieldName = ((StringTerm) args[1]).getString();
        String value = GLAdapterSingleton.instance().field(resultId, fieldName);
        return un.unifies(new StringTermImpl(value), args[2]);
    }
}
