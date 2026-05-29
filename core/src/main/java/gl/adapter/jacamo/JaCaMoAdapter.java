package gl.adapter.jacamo;

import cartago.*;
import gl.adapter.DirectAdapter;

/**
 * JaCaMo/CArtAgO adapter for the Generative Layers framework.
 *
 * <p>Extends {@link cartago.Artifact} and delegates all GL commands to
 * {@link DirectAdapter} — the same pattern used by
 * {@link gl.adapter.AstraAdapter} (ASTRA module) and
 * {@link gl.adapter.jason.JasonAdapter} (Jason internal actions).
 *
 * <p>All three adapters implement the same 16 commands from
 * {@link gl.adapter.ResourceActions}, ensuring platform parity:
 *
 * <table>
 *   <tr><th>Platform</th><th>Adapter</th><th>Agent syntax</th></tr>
 *   <tr><td>ASTRA</td><td>{@code AstraAdapter} (module)</td><td>{@code gl.invoke(...)}</td></tr>
 *   <tr><td>Jason</td><td>{@code JasonAdapter} (internal actions)</td><td>{@code gl.actions.invoke(...)}</td></tr>
 *   <tr><td>JaCaMo</td><td>{@code JaCaMoAdapter} (CArtAgO artifact)</td><td>{@code invoke(...)} on focused artifact</td></tr>
 * </table>
 *
 * <h3>Key difference from Jason/ASTRA adapters</h3>
 * <p>In CArtAgO, results are exposed as <b>observable properties</b> that
 * automatically become agent beliefs when the agent {@code focus}es on the
 * artifact. This is the CArtAgO "sensing" pattern — agents communicate
 * through the shared artifact environment rather than ACL messages.
 *
 * <h3>Usage in .mas2j</h3>
 * <pre>
 * MAS my_app {
 *     environment: jaca.CartagoEnvironment
 *     agents:
 *         myAgent agentArchClass jaca.CAgentArch;
 * }
 * </pre>
 *
 * <h3>Usage in .asl</h3>
 * <pre>
 * !start.
 * +!start &lt;-
 *     makeArtifact("gl", "gl.adapter.jacamo.JaCaMoAdapter", [], Id);
 *     focus(Id);
 *     use_provider;
 *     invoke("a", "g", "llm.answer", "ANSWER", "prompt", "f1,f2", Rid).
 *
 * +gl_result(Rid, Outcome, Valid) &lt;- .print("Result: ", Rid).
 * +gl_accepted(Cid, Fields)      &lt;- .print("Accepted: ", Fields).
 * </pre>
 */
public class JaCaMoAdapter extends Artifact {

    private DirectAdapter adapter;

    // ── Lifecycle ───────────────────────────────────────────────

    /**
     * Initialise the artifact with a fresh {@link DirectAdapter}.
     */
    @OPERATION
    public void init() {
        adapter = new DirectAdapter();
        defineObsProperty("gl_ready", true);
    }

    // ── Provider lifecycle ──────────────────────────────────────

    /** Auto-detect provider from GL_PROVIDER / GL_MODEL env vars (default: "fake"). */
    @OPERATION
    public void use_provider() {
        adapter.use_provider();
        String provider = System.getenv("GL_PROVIDER");
        if (provider == null || provider.isBlank()) provider = "fake";
        defineObsProperty("gl_provider", provider);
    }

    /** Set provider by name. */
    @OPERATION
    public void use_provider(String providerName) {
        adapter.use_provider(providerName);
        defineObsProperty("gl_provider", providerName);
    }

    /** Set provider by name and model. */
    @OPERATION
    public void use_provider(String providerName, String model) {
        adapter.use_provider(providerName, model);
        defineObsProperty("gl_provider", providerName);
    }

    /** Set a configuration key before activating a provider. */
    @OPERATION
    public void configure(String key, String value) {
        adapter.configure(key, value);
    }

    /** Return comma-separated list of available provider names. */
    @OPERATION
    public void providers(OpFeedbackParam<String> result) {
        result.set(adapter.providers());
    }

    // ── Generative body invocation ──────────────────────────────

    /**
     * Invoke a generative body. Returns result ID via {@link OpFeedbackParam}.
     * Creates observable property: {@code gl_result(resultId, outcome, valid)}.
     */
    @OPERATION
    public void invoke(String agentId, String goalId, String bodyId,
                       String affordance, String prompt, String requiredCsv,
                       OpFeedbackParam<String> resultId) {
        String rid = adapter.invoke(agentId, goalId, bodyId, affordance, prompt, requiredCsv);
        resultId.set(rid);
        defineObsProperty("gl_result", rid, adapter.outcome(rid), adapter.valid(rid));
    }

    /** Invoke with belief-RAG context. */
    @OPERATION
    public void invoke_with_beliefs(String agentId, String goalId, String bodyId,
                                     String affordance, String prompt,
                                     String requiredCsv, String beliefsCsv,
                                     OpFeedbackParam<String> resultId) {
        String rid = adapter.invoke_with_beliefs(agentId, goalId, bodyId, affordance,
                prompt, requiredCsv, beliefsCsv);
        resultId.set(rid);
        defineObsProperty("gl_result", rid, adapter.outcome(rid), adapter.valid(rid));
    }

    /** Shorthand: invoke llm.answer with ANSWER affordance. */
    @OPERATION
    public void ask(String agentId, String goalId, String prompt,
                    OpFeedbackParam<String> resultId) {
        String rid = adapter.ask(agentId, goalId, prompt);
        resultId.set(rid);
        defineObsProperty("gl_result", rid, adapter.outcome(rid), adapter.valid(rid));
    }

    // ── Result inspection ──────────────────────────────────────

    /** Check whether a result passed schema validation. */
    @OPERATION
    public void valid(String resultId, OpFeedbackParam<Boolean> result) {
        result.set(adapter.valid(resultId));
    }

    /** Extract a named field from a validated result. */
    @OPERATION
    public void field(String resultId, String fieldName, OpFeedbackParam<String> result) {
        result.set(adapter.field(resultId, fieldName));
    }

    /** Get the candidate ID associated with a result. */
    @OPERATION
    public void candidate(String resultId, OpFeedbackParam<String> result) {
        result.set(adapter.candidate(resultId));
    }

    /** Get the trace ID for auditability. */
    @OPERATION
    public void trace(String resultId, OpFeedbackParam<String> result) {
        result.set(adapter.trace(resultId));
    }

    /** Get the outcome name. */
    @OPERATION
    public void outcome(String resultId, OpFeedbackParam<String> result) {
        result.set(adapter.outcome(resultId));
    }

    /** Return accepted knowledge for an agent. */
    @OPERATION
    public void knowledge(String agentId, OpFeedbackParam<String> result) {
        result.set(adapter.knowledge(agentId));
    }

    // ── Candidate deliberation ─────────────────────────────────

    /** Check whether a candidate passes admissibility. */
    @OPERATION
    public void admissible(String candidateId, OpFeedbackParam<Boolean> result) {
        result.set(adapter.admissible(candidateId));
    }

    /**
     * Accept a candidate. Creates observable property:
     * {@code gl_accepted(candidateId, fieldsCsv)}.
     */
    @OPERATION
    public void accept(String candidateId) {
        adapter.accept(candidateId);
        defineObsProperty("gl_accepted", candidateId, adapter.knowledge(candidateId));
    }

    /**
     * Reject a candidate. Creates observable property:
     * {@code gl_rejected(candidateId)}.
     */
    @OPERATION
    public void reject(String candidateId) {
        adapter.reject(candidateId);
        defineObsProperty("gl_rejected", candidateId);
    }

    /** Record an assessment from a peer agent. */
    @OPERATION
    public void assess(String assessorId, String candidateId,
                       String verdict, double confidence, String explanation) {
        adapter.assess(assessorId, candidateId, verdict, confidence, explanation);
    }
}
