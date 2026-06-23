package gl.adapter.jason.actions;
import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/** gl.adapter.jason.actions.reject -- record negative decision. */
public class reject extends DefaultInternalAction {
    @Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        String candidateId = ((StringTerm) args[0]).getString();
        String reason = ((StringTerm) args[1]).getString();
        String decisionId = JasonAdapter.instance().reject(candidateId, reason);

        if (decisionId == null || decisionId.startsWith("ERROR:")) {
            return false;
        }

        return un.unifies(new StringTermImpl(decisionId), args[2]);
    }
}
