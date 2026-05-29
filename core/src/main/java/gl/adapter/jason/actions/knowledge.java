package gl.adapter.jason.actions;

import gl.adapter.jason.JasonAdapter;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapter.jason.actions.knowledge(AgentId, Knowledge)}.
 *
 * <p>Returns accepted knowledge for an agent as a semicolon-separated string.
 * Delegates to {@link gl.adapter.ResourceActions#knowledge}.
 */
public class knowledge extends DefaultInternalAction {

    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String agentId = ((StringTerm) args[0]).getString();

        String result = JasonAdapter.instance().knowledge(agentId);

        return un.unifies(new StringTermImpl(result), args[1]);
    }
}
