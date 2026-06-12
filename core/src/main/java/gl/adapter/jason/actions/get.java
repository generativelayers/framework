package gl.adapter.jason.actions;
import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/** gl.adapter.jason.actions.get -- project a field from candidate material. */
public class get extends DefaultInternalAction {
    @Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        String candidateId = ((StringTerm) args[0]).getString();
        String fieldName = ((StringTerm) args[1]).getString();
        return un.unifies(new StringTermImpl(JasonAdapter.instance().get(candidateId, fieldName)), args[2]);
    }
}
