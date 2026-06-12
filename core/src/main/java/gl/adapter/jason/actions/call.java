package gl.adapter.jason.actions;
import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/** gl.adapter.jason.actions.call -- governed external invocation. */
public class call extends DefaultInternalAction {
    @Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        String bindingId = ((StringTerm) args[0]).getString();
        String goalId = ((StringTerm) args[1]).getString();
        String bodyId = ((StringTerm) args[2]).getString();
        String affordance = ((StringTerm) args[3]).getString();
        String prompt = ((StringTerm) args[4]).getString();
        String requiredFields = ((StringTerm) args[5]).getString();
        String context = ((StringTerm) args[6]).getString();
        return un.unifies(new StringTermImpl(
                JasonAdapter.instance().call(bindingId, goalId, bodyId, affordance, prompt, requiredFields, context)),
                args[7]);
    }
}
