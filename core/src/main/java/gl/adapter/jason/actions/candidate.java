package gl.adapter.jason.actions;
import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/** gl.adapter.jason.actions.candidate -- get candidateId from resultId. */
public class candidate extends DefaultInternalAction {
    @Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        String resultId = ((StringTerm) args[0]).getString();
        return un.unifies(new StringTermImpl(JasonAdapter.instance().candidate(resultId)), args[1]);
    }
}
