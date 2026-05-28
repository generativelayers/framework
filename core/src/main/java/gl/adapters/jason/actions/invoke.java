package gl.adapters.jason.actions;

import gl.adapters.jason.GLAdapterSingleton;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapters.jason.actions.invoke(AgentId, GoalId, BodyId, Affordance, Prompt, RequiredCsv, ResultId)}.
 *
 * <p>Delegates to {@link gl.adapters.ResourceActions#invoke(String, String, String, String, String, String)}.
 */
public class invoke extends DefaultInternalAction {

    @Override public int getMinArgs() { return 7; }
    @Override public int getMaxArgs() { return 7; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String agentId    = ((StringTerm) args[0]).getString();
        String goalId     = ((StringTerm) args[1]).getString();
        String bodyId     = ((StringTerm) args[2]).getString();
        String affordance = ((StringTerm) args[3]).getString();
        String prompt     = ((StringTerm) args[4]).getString();
        String required   = ((StringTerm) args[5]).getString();

        String resultId = GLAdapterSingleton.instance()
                .invoke(agentId, goalId, bodyId, affordance, prompt, required);

        return un.unifies(new StringTermImpl(resultId), args[6]);
    }
}
