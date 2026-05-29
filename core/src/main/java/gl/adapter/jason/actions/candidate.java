package gl.adapter.jason.actions;

import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapter.jason.actions.candidate(ResultId, CandidateId)}.
 *
 * <p>Delegates to {@link gl.adapter.ResourceActions#candidate(String)}.
 */
public class candidate extends DefaultInternalAction {

    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String resultId = ((StringTerm) args[0]).getString();
        String candidateId = JasonAdapter.instance().candidate(resultId);
        return un.unifies(new StringTermImpl(candidateId), args[1]);
    }
}
