package gl.adapter.jason.actions;
import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/** gl.adapter.jason.actions.accept -- record positive decision. */
public class accept extends DefaultInternalAction {
    @Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        String candidateId = ((StringTerm) args[0]).getString();
        String reason = ((StringTerm) args[1]).getString();
        return un.unifies(new StringTermImpl(JasonAdapter.instance().accept(candidateId, reason)), args[2]);
    }
}
