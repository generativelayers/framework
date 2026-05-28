package gl.adapters.astra;

import astra.core.Module;
import gl.adapters.PlatformBridge;

/**
 * ASTRA platform adapter for the Generative Layers framework.
 *
 * <p>Thin wrapper that exposes the shared {@link PlatformBridge}
 * as ASTRA {@code @ACTION}/{@code @TERM} methods. All command logic
 * lives in {@code PlatformBridge} — this class only adds ASTRA annotations.
 *
 * <p>Symmetric counterpart: {@link gl.adapters.jason.Adapter}.
 *
 * <p>Usage in .astra files:
 * <pre>
 *   module gl.adapters.astra.Adapter gl;
 *
 *   rule +!main(list args) {
 *       gl.use_provider("gemini");
 *       string resultId = gl.invoke("agent", "goal", "llm.answer",
 *                                     "ANSWER", "prompt", "field1,field2");
 *       boolean isValid = gl.valid(resultId);
 *       if (isValid) { gl.accept(gl.candidate(resultId)); }
 *   }
 * </pre>
 */
public class Adapter extends Module {

    private final PlatformBridge bridge = new PlatformBridge("astra");

    // ── Provider lifecycle ──────────────────────────────────────

    @ACTION
    public boolean configure(String key, String value) {
        return bridge.configure(key, value);
    }

    @ACTION
    public boolean use_provider() {
        return bridge.use_provider();
    }

    @ACTION
    public boolean use_provider(String providerName) {
        return bridge.use_provider(providerName);
    }

    @ACTION
    public boolean use_provider(String providerName, String model) {
        return bridge.use_provider(providerName, model);
    }

    @TERM
    public String providers() {
        return bridge.providers();
    }

    // ── Generative body invocation ─────────────────────────────

    @TERM
    public String invoke(String agentId, String goalId,
                         String bodyId, String affordance,
                         String prompt, String requiredCsv) {
        return bridge.invoke(agentId, goalId, bodyId, affordance, prompt, requiredCsv);
    }

    @TERM
    public String ask(String agentId, String goalId, String prompt) {
        return bridge.ask(agentId, goalId, prompt);
    }

    // ── Result inspection ──────────────────────────────────────

    @TERM
    public boolean valid(String resultId) {
        return bridge.valid(resultId);
    }

    @TERM
    public String field(String resultId, String fieldName) {
        return bridge.field(resultId, fieldName);
    }

    @TERM
    public String candidate(String resultId) {
        return bridge.candidate(resultId);
    }

    @TERM
    public String trace(String resultId) {
        return bridge.trace(resultId);
    }

    @TERM
    public String outcome(String resultId) {
        return bridge.outcome(resultId);
    }

    // ── Candidate deliberation ─────────────────────────────────

    @TERM
    public boolean admissible(String candidateId) {
        return bridge.admissible(candidateId);
    }

    @ACTION
    public boolean accept(String candidateId) {
        return bridge.accept(candidateId);
    }

    @ACTION
    public boolean reject(String candidateId) {
        return bridge.reject(candidateId);
    }

    @ACTION
    public boolean assess(String assessorId, String candidateId,
                          String verdict, double confidence,
                          String explanation) {
        return bridge.assess(assessorId, candidateId, verdict, confidence, explanation);
    }
}
