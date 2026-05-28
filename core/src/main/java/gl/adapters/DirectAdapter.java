package gl.adapters;

import gl.body.GenerativeBodyRuntime;
import gl.kernel.GovernanceKernel;
import gl.kernel.GovernanceKernelFactory;
import gl.provider.ProviderConfig;
import gl.provider.ProviderRegistry;

/**
 * Direct Generative Layer adapter — synchronous, in-process access.
 *
 * <p>Inherits all {@link ResourceActions} commands from {@link AdapterBase}.
 * Supports runtime provider reconfiguration via {@link #withConfig(ProviderConfig)}.
 */
public final class DirectAdapter extends AdapterBase {

    /** Create with the default (auto-detected) provider. */
    public DirectAdapter() {
        super(AdapterRuntime.kernel(), AdapterRuntime.bodies());
    }

    /** Create with an explicit kernel and body registry. */
    public DirectAdapter(GovernanceKernel kernel, gl.body.GenerativeBodyRegistry bodies) {
        super(kernel, bodies);
    }

    /**
     * Create a new adapter configured with the given provider config.
     */
    public static DirectAdapter withConfig(ProviderConfig config) {
        var provider = ProviderRegistry.create(config.provider(), config);
        var kernel = GovernanceKernelFactory.withProvider(provider);
        var bodies = GenerativeBodyRuntime.createStandardRegistry(kernel);
        return new DirectAdapter(kernel, bodies);
    }

    // ── Legacy method aliases (backward compatibility) ─────────

    public boolean validResult(String resultId) { return valid(resultId); }
    public String resultField(String resultId, String fieldName) { return field(resultId, fieldName); }
    public String candidateId(String resultId) { return candidate(resultId); }
    public String traceId(String resultId) { return trace(resultId); }
    public String outcomeName(String resultId) { return outcome(resultId); }
    public boolean admissibleCandidate(String candidateId) { return admissible(candidateId); }
    public boolean acceptCandidate(String candidateId) { return accept(candidateId); }
    public boolean rejectCandidate(String candidateId) { return reject(candidateId); }
    public String assessCandidate(String assessorId, String candidateId, String verdict, double confidence, String criteriaCsv, String evidenceCsv, String explanation) {
        return assessFull(assessorId, candidateId, verdict, confidence, criteriaCsv, evidenceCsv, explanation);
    }
}
