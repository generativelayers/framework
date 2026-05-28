package gl.adapters.jason.actions;

import gl.adapters.jason.Adapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapters.jason.actions.admissible(CandidateId, IsAdmissible)}.
 *
 * <p>Delegates to {@link gl.adapters.ResourceActions#admissible(String)}.
 */
public class admissible extends DefaultInternalAction {

    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String candidateId = ((StringTerm) args[0]).getString();
        boolean isAdmissible = Adapter.instance().admissible(candidateId);
        return un.unifies(isAdmissible ? Literal.LTrue : Literal.LFalse, args[1]);
    }
}
