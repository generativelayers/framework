package gl.adapter.astra;

import gl.adapter.DirectAdapter;

import astra.core.Module;

/**
 * ASTRA platform adapter for the GL v2 framework.
 *
 * <p>All agents in the same MAS share a single {@link DirectAdapter}
 * instance (shared lifecycle stores). Provider bindings are per-agent
 * via {@code bind()}, but candidates, assessments, and decisions are
 * globally resolvable -- so one agent can judge another agent's candidate.
 *
 * <p>GL v2 -- 13 public commands:
 * <pre>
 *   see &gt; bind &gt; call &gt; result &gt; candidate &gt; check &gt; get
 *       &gt; judge &gt; decide &gt; accept/reject &gt; knowledge &gt; explain
 * </pre>
 *
 * <p>Usage in .astra files (via the {@link gl.astra.GL} alias):
 * <pre>
 *   module gl.astra.GL gl;
 *
 *   rule +!main(list args) {
 *       +binding(gl.bind("agent1", "groq", "llama-3.3-70b-versatile", ""));
 *       !classify("apple");
 *   }
 *
 *   rule +!classify(string item) : binding(string bid) {
 *       !evaluate(gl.candidate(gl.call(bid, "classify", "llm.answer",
 *           "ANSWER", "Classify: " + item, "label,confidence", "")), item);
 *   }
 *
 *   rule +!evaluate(string cid, string item) : gl.decide(cid) == "ADMISSIBLE" {
 *       gl.accept(cid, "valid classification");
 *       +classified(item, gl.get(cid, "label"));
 *   }
 * </pre>
 */
public class AstraAdapter extends Module {

    /** Shared across all agents in the same JVM/MAS. */
    private static final DirectAdapter SHARED = new DirectAdapter();

    // -- 1. Discovery --

    @TERM public String see() { return SHARED.see(); }

    // -- 2. Binding --

    @TERM public String bind(String agentId, String provider, String model, String config) {
        return SHARED.bind(agentId, provider, model, config);
    }

    // -- 3. Invocation --

    @TERM public String call(String bindingId, String goalId, String bodyId,
                             String affordance, String prompt, String requiredFields,
                             String context) {
        return SHARED.call(bindingId, goalId, bodyId, affordance, prompt, requiredFields, context);
    }

    // -- 4. Result inspection --

    @TERM public String result(String resultId) { return SHARED.result(resultId); }

    // -- 5. Candidate boundary --

    @TERM public String candidate(String resultId) { return SHARED.candidate(resultId); }

    // -- 6. Check --

    @TERM public String check(String refId) { return SHARED.check(refId); }

    // -- 7. Field projection --

    @TERM public String get(String candidateId, String fieldName) {
        return SHARED.get(candidateId, fieldName);
    }

    // -- 8. Judge --

    @TERM public String judge(String candidateId, String assessorId, String verdict,
                              double confidence, String rationale) {
        return SHARED.judge(candidateId, assessorId, verdict, confidence, rationale);
    }

    // -- 9. Decide --

    @TERM public String decide(String candidateId) { return SHARED.decide(candidateId); }

    // -- 10. Accept --

    @ACTION public boolean accept(String candidateId, String reason) {
        String result = SHARED.accept(candidateId, reason);
        return result != null && !result.startsWith("ERROR:");
    }

    // -- 11. Reject --

    @ACTION public boolean reject(String candidateId, String reason) {
        String result = SHARED.reject(candidateId, reason);
        return result != null && !result.startsWith("ERROR:");
    }

    // -- 12. Knowledge --

    @TERM public String knowledge(String agentId) { return SHARED.knowledge(agentId); }

    // -- 13. Explain --

    @TERM public String explain(String refId) { return SHARED.explain(refId); }
}
