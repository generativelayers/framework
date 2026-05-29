package gl.adapter.jason.actions;

import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapter.jason.actions.admissible(CandidateId, IsAdmissible)}.
 *
 * <p>Delegates to {@link gl.adapter.ResourceActions#admissible(String)}.
 */
public class admissible extends DefaultInternalAction {

    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String candidateId = ((StringTerm) args[0]).getString();
        boolean isAdmissible = JasonAdapter.instance().admissible(candidateId);
        return un.unifies(isAdmissible ? Literal.LTrue : Literal.LFalse, args[1]);
    }
}
