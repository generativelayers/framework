/**
 * Generative Body — affordance model and invocation pipeline.
 *
 * <p>The generative body is the agent's effector for LLM interaction.
 * It defines what the agent can ask the LLM to do (affordances) and
 * governs the output through the {@link gl.GovernanceKernel}.
 *
 * <h2>Affordances</h2>
 * <ul>
 *   <li>{@link gl.body.BodyAffordance} — 13 affordance types:
 *       ANSWER, CLASSIFY, SUMMARISE, REFLECT, CRITIQUE, etc.</li>
 *   <li>{@link gl.body.GenerativeBody} — interface for a body</li>
 *   <li>{@link gl.body.DefaultGenerativeBody} — default: maps
 *       affordance to candidate type and calls the kernel</li>
 * </ul>
 *
 * <h2>Registry and Runtime</h2>
 * <ul>
 *   <li>{@link gl.body.GenerativeBodyRegistry} — maps body IDs to instances</li>
 *   <li>{@link gl.body.GenerativeBodyRuntime} — factory wiring</li>
 *   <li>{@link gl.body.BodyDescriptor} — body metadata</li>
 * </ul>
 *
 * <h2>Invocation</h2>
 * <ul>
 *   <li>{@link gl.body.BodyInvocation} — invocation request</li>
 *   <li>{@link gl.body.InvocationResult} — invocation result</li>
 *   <li>{@link gl.body.InvocationStatus} — status enum</li>
 *   <li>{@link gl.body.BodyKind} — LLM, RAG, PLANNER, etc.</li>
 * </ul>
 */
package gl.body;
