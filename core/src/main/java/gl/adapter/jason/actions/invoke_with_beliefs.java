package gl.adapter.jason.actions;

import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapter.jason.actions.invoke_with_beliefs(AgentId, GoalId, BodyId, Affordance, Prompt, RequiredCsv, BeliefsCsv, ResultId)}.
 *
 * <p>Delegates to {@link gl.adapter.ResourceActions#invoke_with_beliefs}.
 */
public class invoke_with_beliefs extends DefaultInternalAction {

    @Override public int getMinArgs() { return 8; }
    @Override public int getMaxArgs() { return 8; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String agentId    = ((StringTerm) args[0]).getString();
        String goalId     = ((StringTerm) args[1]).getString();
        String bodyId     = ((StringTerm) args[2]).getString();
        String affordance = ((StringTerm) args[3]).getString();
        String prompt     = ((StringTerm) args[4]).getString();
        String required   = ((StringTerm) args[5]).getString();
        String beliefs    = ((StringTerm) args[6]).getString();

        String resultId = JasonAdapter.instance()
                .invoke_with_beliefs(agentId, goalId, bodyId, affordance, prompt, required, beliefs);

        return un.unifies(new StringTermImpl(resultId), args[7]);
    }
}
