package gl.adapters.jason.actions;

import gl.adapters.jason.Adapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapters.jason.actions.ask(AgentId, GoalId, Prompt, ResultId)}.
 *
 * <p>Delegates to {@link gl.adapters.ResourceActions#ask(String, String, String)}.
 */
public class ask extends DefaultInternalAction {

    @Override public int getMinArgs() { return 4; }
    @Override public int getMaxArgs() { return 4; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String agentId = ((StringTerm) args[0]).getString();
        String goalId  = ((StringTerm) args[1]).getString();
        String prompt  = ((StringTerm) args[2]).getString();

        String resultId = Adapter.instance().ask(agentId, goalId, prompt);

        return un.unifies(new StringTermImpl(resultId), args[3]);
    }
}
