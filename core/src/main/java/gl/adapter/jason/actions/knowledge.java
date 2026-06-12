package gl.adapter.jason.actions;
import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/** gl.adapter.jason.actions.knowledge -- accepted GL-side material for an agent. */
public class knowledge extends DefaultInternalAction {
    @Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        String agentId = ((StringTerm) args[0]).getString();
        return un.unifies(new StringTermImpl(JasonAdapter.instance().knowledge(agentId)), args[1]);
    }
}
