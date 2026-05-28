package gl.adapters;

import gl.body.GenerativeBodyRuntime;
import gl.GovernanceKernel.GovernanceKernel;
import gl.GovernanceKernel.GovernanceKernelFactory;
import gl.provider.ProviderConfig;
import gl.provider.ProviderRegistry;

import java.util.Map;

/**
 * Direct Generative Layer adapter — synchronous, in-process access to the Generative Layer framework.
 *
 * <p>Supports runtime provider reconfiguration via {@link #reconfigure(ProviderConfig)}.
 * When reconfigured, a fresh GovernanceKernel and body registry are created with the
 * chosen provider, replacing the defaults.
 */
public final class DirectAdapter extends AdapterBase {

    /** Create with the default (auto-detected) provider. */
    public DirectAdapter() {
        super(AdapterRuntime.GovernanceKernel(), AdapterRuntime.bodies());
    }

    /** Create with an explicit GovernanceKernel and body registry. */
    public DirectAdapter(GovernanceKernel GovernanceKernel, gl.body.GenerativeBodyRegistry bodies) {
        super(GovernanceKernel, bodies);
    }

    /**
     * Create a new adapter configured with the given provider config.
     * This is how the ASTRA Module switches providers at runtime.
     */
    public static DirectAdapter withConfig(ProviderConfig config) {
        var provider = ProviderRegistry.create(config.provider(), config);
        var GovernanceKernel = GovernanceKernelFactory.withProvider(provider);
        var bodies = GenerativeBodyRuntime.createStandardRegistry(GovernanceKernel);
        return new DirectAdapter(GovernanceKernel, bodies);
    }

    public String ask(String agentId, String goalId, String prompt) {
        return invokeBody(agentId, goalId, "llm.answer", "ANSWER", prompt, "", Map.of());
    }

    public String invoke(String agentId, String goalId, String bodyId, String affordance, String prompt, String requiredCsv) {
        return invokeBody(agentId, goalId, bodyId, affordance, prompt, requiredCsv, Map.of());
    }

    public boolean validResult(String resultId) { return valid(resultId); }
    public String resultField(String resultId, String fieldName) { return field(resultId, fieldName); }
    public String candidateId(String resultId) { return candidate(resultId); }
    public String traceId(String resultId) { return trace(resultId); }
    public String outcomeName(String resultId) { return outcome(resultId); }
    public boolean admissibleCandidate(String candidateId) { return admissible(candidateId); }
    public boolean acceptCandidate(String candidateId) { return accept(candidateId); }
    public boolean rejectCandidate(String candidateId) { return reject(candidateId); }

    public String assessCandidate(String assessorId, String candidateId, String verdict, double confidence, String criteriaCsv, String evidenceCsv, String explanation) {
        return assess(assessorId, candidateId, verdict, confidence, criteriaCsv, evidenceCsv, explanation);
    }
}
