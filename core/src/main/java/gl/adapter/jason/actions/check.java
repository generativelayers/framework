package gl.adapter.jason.actions;
import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/** gl.adapter.jason.actions.check -- validation/status for result or candidate. */
public class check extends DefaultInternalAction {
    @Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        String refId = ((StringTerm) args[0]).getString();
        return un.unifies(new StringTermImpl(JasonAdapter.instance().check(refId)), args[1]);
    }
}
