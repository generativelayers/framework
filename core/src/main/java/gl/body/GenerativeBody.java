package gl.body;

import gl.model.*;

/** Interface for a generative body -- the agent's effector for LLM interaction.
 *  Implementations receive a {@link BodyInvocation} and return an
 *  {@link InvocationResult} containing the governed candidate. */
public interface GenerativeBody {
    BodyDescriptor descriptor();
    InvocationResult invoke(BodyInvocation invocation);
}
