package gl.adapter.astra;

import gl.adapter.DirectAdapter;

import astra.core.Module;

/**
 * ASTRA platform adapter for the Generative Layers framework.
 *
 * <p>All agents in the same MAS share a single {@link DirectAdapter}
 * instance (and thus a single {@link gl.GovernanceKernel}). This
 * ensures that candidate IDs are valid across agents — so one agent
 * can assess another agent's candidate via {@link #assess}.
 *
 * <p>Usage in .astra files:
 * <pre>
 *   module gl.adapter.astra.AstraAdapter gl;
 *
 *   rule +!main(list args) {
 *       gl.use_provider("gemini");
 *       string resultId = gl.ask("agent", "goal", "Classify: apple");
 *       if (gl.valid(resultId)) { gl.accept(gl.candidate(resultId)); }
 *   }
 * </pre>
 */
public class AstraAdapter extends Module {

    /** Shared across all agents in the same JVM/MAS. */
    private static final DirectAdapter SHARED = new DirectAdapter();

    // ── Provider lifecycle ──────────────────────────────────────

    @ACTION public boolean configure(String key, String value) { return SHARED.configure(key, value); }
    @ACTION public boolean use_provider() { return SHARED.use_provider(); }
    @ACTION public boolean use_provider(String name) { return SHARED.use_provider(name); }
    @ACTION public boolean use_provider(String name, String model) { return SHARED.use_provider(name, model); }
    @TERM   public String providers() { return SHARED.providers(); }

    // ── Generative body invocation ─────────────────────────────

    @TERM public String invoke(String agentId, String goalId, String bodyId,
                               String affordance, String prompt, String csv) {
        return SHARED.invoke(agentId, goalId, bodyId, affordance, prompt, csv);
    }
    @TERM public String invoke_with_beliefs(String agentId, String goalId, String bodyId,
                                            String affordance, String prompt, String csv,
                                            String beliefsCsv) {
        return SHARED.invoke_with_beliefs(agentId, goalId, bodyId, affordance, prompt, csv, beliefsCsv);
    }
    @TERM public String ask(String agentId, String goalId, String prompt) {
        return SHARED.ask(agentId, goalId, prompt);
    }

    // ── Result inspection ──────────────────────────────────────

    @TERM public boolean valid(String resultId) { return SHARED.valid(resultId); }
    @TERM public String field(String resultId, String fieldName) { return SHARED.field(resultId, fieldName); }
    @TERM public String candidate(String resultId) { return SHARED.candidate(resultId); }
    @TERM public String trace(String resultId) { return SHARED.trace(resultId); }
    @TERM public String outcome(String resultId) { return SHARED.outcome(resultId); }
    @TERM public String knowledge(String agentId) { return SHARED.knowledge(agentId); }

    // ── Candidate deliberation ─────────────────────────────────

    @TERM   public boolean admissible(String candidateId) { return SHARED.admissible(candidateId); }
    @ACTION public boolean accept(String candidateId) { return SHARED.accept(candidateId); }
    @ACTION public boolean reject(String candidateId) { return SHARED.reject(candidateId); }
    @ACTION public boolean assess(String assessorId, String candidateId,
                                  String verdict, double confidence, String explanation) {
        return SHARED.assess(assessorId, candidateId, verdict, confidence, explanation);
    }
}
