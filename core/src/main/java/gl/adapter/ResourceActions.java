package gl.adapter;

/**
 * Canonical contract for all Generative Layer commands.
 *
 * <p>Both the ASTRA adapter ({@link gl.adapter.AstraAdapter}) and the Jason adapter
 * ({@link gl.adapter.jason.Adapter} + Internal Actions) delegate to this interface.
 * This guarantees that every command has identical name, parameter count,
 * parameter types, and return type -- regardless of the host platform.
 *
 * <p>Platform-specific differences (ASTRA's {@code @TERM} return vs. Jason's
 * unification) are handled in the thin adapter layer on top of this contract.
 *
 * <h3>Commands (16 total)</h3>
 * <table>
 *   <tr><th>Command</th><th>Returns</th><th>Description</th></tr>
 *   <tr><td>{@code configure}</td><td>boolean</td><td>Set a config key/value</td></tr>
 *   <tr><td>{@code use_provider}</td><td>boolean</td><td>Activate provider</td></tr>
 *   <tr><td>{@code providers}</td><td>String</td><td>CSV of available providers</td></tr>
 *   <tr><td>{@code invoke}</td><td>String</td><td>Invoke generative body</td></tr>
 *   <tr><td>{@code invoke_with_beliefs}</td><td>String</td><td>Invoke with belief-RAG context</td></tr>
 *   <tr><td>{@code ask}</td><td>String</td><td>Shorthand LLM ask</td></tr>
 *   <tr><td>{@code valid}</td><td>boolean</td><td>Schema validation check</td></tr>
 *   <tr><td>{@code field}</td><td>String</td><td>Extract result field</td></tr>
 *   <tr><td>{@code candidate}</td><td>String</td><td>Get candidate ID</td></tr>
 *   <tr><td>{@code trace}</td><td>String</td><td>Get trace ID</td></tr>
 *   <tr><td>{@code outcome}</td><td>String</td><td>Get outcome name</td></tr>
 *   <tr><td>{@code knowledge}</td><td>String</td><td>CSV of accepted knowledge</td></tr>
 *   <tr><td>{@code admissible}</td><td>boolean</td><td>Admissibility check</td></tr>
 *   <tr><td>{@code accept}</td><td>boolean</td><td>Accept candidate</td></tr>
 *   <tr><td>{@code reject}</td><td>boolean</td><td>Reject candidate</td></tr>
 *   <tr><td>{@code assess}</td><td>boolean</td><td>Record assessment</td></tr>
 * </table>
 */
public interface ResourceActions {

    // ── Provider lifecycle ──────────────────────────────────────

    /** Set a configuration key before activating a provider. */
    boolean configure(String key, String value);

    /** Activate the provider with accumulated configuration. */
    boolean use_provider();

    /** Activate by provider name (shorthand). */
    boolean use_provider(String providerName);

    /** Activate by provider name + model (shorthand). */
    boolean use_provider(String providerName, String model);

    /** Return comma-separated list of available provider names. */
    String providers();

    // ── Generative body invocation ──────────────────────────────

    /** Invoke a generative body. Returns the result ID. */
    String invoke(String agentId, String goalId,
                  String bodyId, String affordance,
                  String prompt, String requiredCsv);

    /** Invoke a generative body with belief context prepended to the prompt.
     *  @param beliefsCsv comma-separated belief strings to inject as RAG context */
    String invoke_with_beliefs(String agentId, String goalId,
                               String bodyId, String affordance,
                               String prompt, String requiredCsv,
                               String beliefsCsv);

    /** Shorthand: invoke llm.answer with ANSWER affordance. Returns the result ID. */
    String ask(String agentId, String goalId, String prompt);

    /** Shorthand: invoke llm.answer with ANSWER affordance and a conversation context. Returns the result ID. */
    String ask(String agentId, String goalId, String prompt, String conversationId);


    // ── Result inspection ──────────────────────────────────────

    /** Check whether a result passed schema validation. */
    boolean valid(String resultId);

    /** Extract a named field from a validated result. */
    String field(String resultId, String fieldName);

    /** Get the candidate ID associated with a result. */
    String candidate(String resultId);

    /** Get the trace ID for auditability. */
    String trace(String resultId);

    /** Get the outcome name (e.g. VALIDATED, INVALID, GOVERNANCE_DENIED). */
    String outcome(String resultId);

    /** Return accepted knowledge for an agent as semicolon-separated "key=value" entries.
     *  Each candidate's fields are joined, candidates separated by semicolons.
     *  Returns "" if no accepted candidates exist. */
    String knowledge(String agentId);

    // ── Candidate deliberation ─────────────────────────────────

    /** Check whether a candidate passes admissibility. */
    boolean admissible(String candidateId);

    /** Accept a candidate -- explicit agent adoption. */
    boolean accept(String candidateId);

    /** Reject a candidate -- agent refuses to adopt. */
    boolean reject(String candidateId);

    /** Record an assessment. Returns true on success. */
    boolean assess(String assessorId, String candidateId,
                   String verdict, double confidence,
                   String explanation);
}
