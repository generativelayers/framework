package gl.adapters.jason.actions;

import gl.adapters.jason.GLAdapterSingleton;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

/**
 * Jason Internal Action: {@code .gl.adapters.jason.actions.use_provider(ProviderName)}
 * or {@code .gl.adapters.jason.actions.use_provider(ProviderName, Model)}.
 *
 * <p>Delegates to {@link gl.adapters.ResourceActions#use_provider(String)}.
 */
public class use_provider extends DefaultInternalAction {

    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        String providerName = ((StringTerm) args[0]).getString();
        if (args.length == 2) {
            String model = ((StringTerm) args[1]).getString();
            return GLAdapterSingleton.instance().use_provider(providerName, model);
        } else {
            return GLAdapterSingleton.instance().use_provider(providerName);
        }
    }
}
