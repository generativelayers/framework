package gl.adapter.jason.actions;
import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/** gl.adapter.jason.actions.bind -- bind agent to provider/model/config. */
public class bind extends DefaultInternalAction {
    @Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        String agentId = ((StringTerm) args[0]).getString();
        String provider = ((StringTerm) args[1]).getString();
        String model = ((StringTerm) args[2]).getString();
        String config = ((StringTerm) args[3]).getString();
        return un.unifies(new StringTermImpl(JasonAdapter.instance().bind(agentId, provider, model, config)), args[4]);
    }
}
