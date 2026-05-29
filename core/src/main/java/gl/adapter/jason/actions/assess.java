package gl.adapter.jason.actions;

import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapter.jason.actions.assess(AssessorId, CandidateId, Verdict, Confidence, Explanation)}.
 *
 * <p>Delegates to {@link gl.adapter.ResourceActions#assess(String, String, String, double, String)}.
 */
public class assess extends DefaultInternalAction {

    @Override public int getMinArgs() { return 5; }
    @Override public int getMaxArgs() { return 5; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String assessorId  = ((StringTerm) args[0]).getString();
        String candidateId = ((StringTerm) args[1]).getString();
        String verdict     = ((StringTerm) args[2]).getString();
        double confidence  = ((NumberTerm) args[3]).solve();
        String explanation = ((StringTerm) args[4]).getString();

        return JasonAdapter.instance()
                .assess(assessorId, candidateId, verdict, confidence, explanation);
    }
}
