package gl.adapters.jason.actions;

import gl.adapters.jason.GLAdapterSingleton;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapters.jason.actions.valid(ResultId, IsValid)}.
 *
 * <p>Delegates to {@link gl.adapters.ResourceActions#valid(String)}.
 */
public class valid extends DefaultInternalAction {

    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String resultId = ((StringTerm) args[0]).getString();
        boolean isValid = GLAdapterSingleton.instance().valid(resultId);
        return un.unifies(isValid ? Literal.LTrue : Literal.LFalse, args[1]);
    }
}
