package gl.adapters.jason.actions;

import gl.adapters.jason.Adapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapters.jason.actions.reject(CandidateId)}.
 *
 * <p>Delegates to {@link gl.adapters.ResourceActions#reject(String)}.
 */
public class reject extends DefaultInternalAction {

    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 1; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String candidateId = ((StringTerm) args[0]).getString();
        return Adapter.instance().reject(candidateId);
    }
}
