package gl.adapter.jason.actions;
import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/** gl.adapter.jason.actions.judge -- record evaluative evidence about a candidate. */
public class judge extends DefaultInternalAction {
    @Override public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        String candidateId = ((StringTerm) args[0]).getString();
        String assessorId = ((StringTerm) args[1]).getString();
        String verdict = ((StringTerm) args[2]).getString();
        double confidence = ((NumberTerm) args[3]).solve();
        String rationale = ((StringTerm) args[4]).getString();
        return un.unifies(new StringTermImpl(
                JasonAdapter.instance().judge(candidateId, assessorId, verdict, confidence, rationale)),
                args[5]);
    }
}
