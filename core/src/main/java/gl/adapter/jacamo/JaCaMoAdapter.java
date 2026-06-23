package gl.adapter.jacamo;

import cartago.*;
import gl.adapter.DirectAdapter;

/**
 * JaCaMo/CArtAgO adapter for the GL v2 framework.
 *
 * <p>Extends {@link cartago.Artifact} and delegates all GL commands to
 * {@link DirectAdapter}. The artifact may expose lifecycle metadata as
 * observable properties for platform integration. Generated candidate
 * content is not automatically adopted as BDI belief; belief adoption
 * remains an explicit host-agent operation.
 *
 * <p>GL v2 -- 13 public commands:
 * <pre>
 *   see > bind > call > result > candidate > check > get
 *       > judge > decide > accept/reject > knowledge > explain
 * </pre>
 *
 * <p>Usage in .asl:
 * <pre>
 * !start.
 * +!start &lt;-
 *     makeArtifact("gl", "gl.adapter.jacamo.JaCaMoAdapter", [], Id);
 *     focus(Id);
 *     see(Providers);
 *     bind("agent1", "gemini", "gemini-2.5-flash", "", Bid);
 *     call(Bid, "classify", "llm.answer", "ANSWER", "Classify: apple", "label", "", Rid);
 *     candidate(Rid, Cid);
 *     judge(Cid, "agent1", "APPROVE", 0.9, "looks correct", Aid);
 *     decide(Cid, Adm);
 *     accept(Cid, "valid classification", Did).
 * </pre>
 */
public class JaCaMoAdapter extends Artifact {

    /** Shared across all agents in the same JVM/MAS (same pattern as AstraAdapter). */
    private static final DirectAdapter SHARED = new DirectAdapter();

    // -- Lifecycle -----------------------------------------------

    @OPERATION
    public void init() {
        defineObsProperty("gl_ready", true);
    }

    // -- 1. see --

    @OPERATION
    public void see(OpFeedbackParam<String> result) {
        result.set(SHARED.see());
    }

    // -- 2. bind --

    @OPERATION
    public void bind(String agentId, String provider, String model, String config,
                     OpFeedbackParam<String> result) {
        String bindingId = SHARED.bind(agentId, provider, model, config);
        result.set(bindingId);
        if (!bindingId.startsWith("ERROR:")) {
            signal("gl_bound", bindingId, agentId, provider, model);
        }
    }

    // -- 3. call --

    @OPERATION
    public void call(String bindingId, String goalId, String bodyId,
                     String affordance, String prompt, String requiredFields,
                     String context, OpFeedbackParam<String> result) {
        String rid = SHARED.call(bindingId, goalId, bodyId, affordance, prompt, requiredFields, context);
        result.set(rid);
    }

    // -- 4. result --

    @OPERATION
    public void result(String resultId, OpFeedbackParam<String> result) {
        result.set(SHARED.result(resultId));
    }

    // -- 5. candidate --

    @OPERATION
    public void candidate(String resultId, OpFeedbackParam<String> result) {
        result.set(SHARED.candidate(resultId));
    }

    // -- 6. check --

    @OPERATION
    public void check(String refId, OpFeedbackParam<String> result) {
        result.set(SHARED.check(refId));
    }

    // -- 7. get --

    @OPERATION
    public void get(String candidateId, String fieldName, OpFeedbackParam<String> result) {
        result.set(SHARED.get(candidateId, fieldName));
    }

    // -- 8. judge --

    @OPERATION
    public void judge(String candidateId, String assessorId, String verdict,
                      double confidence, String rationale, OpFeedbackParam<String> result) {
        result.set(SHARED.judge(candidateId, assessorId, verdict, confidence, rationale));
    }

    // -- 9. decide --

    @OPERATION
    public void decide(String candidateId, OpFeedbackParam<String> result) {
        result.set(SHARED.decide(candidateId));
    }

    // -- 10. accept --

    @OPERATION
    public void accept(String candidateId, String reason, OpFeedbackParam<String> result) {
        String decisionId = SHARED.accept(candidateId, reason);
        if (decisionId == null || decisionId.startsWith("ERROR:")) {
            failed(decisionId == null ? "ERROR:accept_failed" : decisionId);
            return;
        }

        result.set(decisionId);
        signal("gl_accepted", candidateId, decisionId);
    }

    // -- 11. reject --

    @OPERATION
    public void reject(String candidateId, String reason, OpFeedbackParam<String> result) {
        String decisionId = SHARED.reject(candidateId, reason);
        if (decisionId == null || decisionId.startsWith("ERROR:")) {
            failed(decisionId == null ? "ERROR:reject_failed" : decisionId);
            return;
        }

        result.set(decisionId);
        signal("gl_rejected", candidateId, decisionId);
    }

    // -- 12. knowledge --

    @OPERATION
    public void knowledge(String agentId, OpFeedbackParam<String> result) {
        result.set(SHARED.knowledge(agentId));
    }

    // -- 13. explain --

    @OPERATION
    public void explain(String refId, OpFeedbackParam<String> result) {
        result.set(SHARED.explain(refId));
    }
}
