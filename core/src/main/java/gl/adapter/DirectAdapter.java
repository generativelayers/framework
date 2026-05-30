package gl.adapter;

import gl.Outcomes;
import gl.GovernanceKernel;
import gl.GovernanceKernelFactory;
import gl.body.*;
import gl.model.*;
import gl.provider.ProviderConfig;
import gl.provider.ProviderRegistry;

import java.util.*;

/**
 * Direct Generative Layer adapter — the single implementation of {@link ResourceActions}.
 *
 * <p>Contains all command logic: provider lifecycle, body invocation,
 * result inspection, and candidate deliberation. Both ASTRA and Jason
 * adapters delegate to an instance of this class.
 *
 * <p>Supports runtime provider reconfiguration via {@link #configure} +
 * {@link #use_provider()}.
 */
public final class DirectAdapter implements ResourceActions {

    private GovernanceKernel kernel;
    private GenerativeBodyRegistry bodies;
    private final ProviderConfig.Builder configBuilder = new ProviderConfig.Builder();

    /** Create with the default (auto-detected) provider. */
    public DirectAdapter() {
        this(GenerativeBodyRuntime.kernel(), GenerativeBodyRuntime.registry());
    }

    /** Create with an explicit kernel and body registry. */
    public DirectAdapter(GovernanceKernel kernel, GenerativeBodyRegistry bodies) {
        this.kernel = Objects.requireNonNull(kernel);
        this.bodies = Objects.requireNonNull(bodies);
    }

    /** Create a new adapter configured with the given provider config. */
    public static DirectAdapter withConfig(ProviderConfig config) {
        var provider = ProviderRegistry.create(config.provider(), config);
        var kernel = GovernanceKernelFactory.withProvider(provider);
        var bodies = GenerativeBodyRuntime.createStandardRegistry(kernel);
        return new DirectAdapter(kernel, bodies);
    }

    /** Direct access to the kernel (for tests and advanced usage). */
    public GovernanceKernel kernel() { return kernel; }

    // ── Provider lifecycle ──────────────────────────────────────

    @Override
    public boolean configure(String key, String value) {
        configBuilder.set(key, value);
        return true;
    }

    @Override
    public boolean use_provider() {
        // Auto-detect from environment if no provider was explicitly configured
        if (!configBuilder.has("provider")) {
            String envProvider = System.getenv("GL_PROVIDER");
            if (envProvider != null && !envProvider.isBlank()) {
                configBuilder.set("provider", envProvider);
            } else {
                configBuilder.set("provider", "fake");
            }
        }
        if (!configBuilder.has("model")) {
            String envModel = System.getenv("GL_MODEL");
            if (envModel != null && !envModel.isBlank()) {
                configBuilder.set("model", envModel);
            }
        }
        ProviderConfig config = configBuilder.build();
        var provider = ProviderRegistry.resolve(config);
        this.kernel = GovernanceKernelFactory.withProvider(provider);
        this.bodies = GenerativeBodyRuntime.createStandardRegistry(this.kernel);
        return true;
    }

    @Override
    public boolean use_provider(String providerName) {
        configure("provider", providerName);
        return use_provider();
    }

    @Override
    public boolean use_provider(String providerName, String model) {
        configure("provider", providerName);
        configure("model", model);
        return use_provider();
    }

    @Override
    public String providers() {
        return String.join(",", ProviderRegistry.available());
    }

    // ── Generative body invocation ──────────────────────────────

    @Override
    public String invoke(String agentId, String goalId,
                         String bodyId, String affordance,
                         String prompt, String requiredCsv) {
        return invoke_with_beliefs(agentId, goalId, bodyId, affordance, prompt, requiredCsv, "");
    }

    @Override
    public String invoke_with_beliefs(String agentId, String goalId,
                                      String bodyId, String affordance,
                                      String prompt, String requiredCsv,
                                      String beliefsCsv) {
        BodyAffordance aff = BodyAffordance.valueOf(affordance.toUpperCase());
        List<String> fields = (requiredCsv == null || requiredCsv.isBlank())
                ? List.of()
                : Arrays.asList(requiredCsv.split(","));
        List<String> beliefs = (beliefsCsv == null || beliefsCsv.isBlank())
                ? List.of()
                : Arrays.asList(beliefsCsv.split(","));
        BodyInvocation invocation = new BodyInvocation(
                agentId, goalId, bodyId, aff, prompt, fields, Map.of(), beliefs, "");
        GenerativeBody body = bodies.require(bodyId);
        InvocationResult result = body.invoke(invocation);
        return result.resourceResult() != null ? result.resourceResult().resultId() : "";
    }

    @Override
    public String ask(String agentId, String goalId, String prompt) {
        return ask(agentId, goalId, prompt, "");
    }

    @Override
    public String ask(String agentId, String goalId, String prompt, String conversationId) {
        BodyInvocation invocation = new BodyInvocation(
                agentId, goalId, "llm.answer", BodyAffordance.ANSWER,
                prompt, List.of(), Map.of(), List.of(), conversationId
        );
        GenerativeBody body = bodies.require("llm.answer");
        InvocationResult result = body.invoke(invocation);
        return result.resourceResult() != null ? result.resourceResult().resultId() : "";
    }


    // ── Result inspection ──────────────────────────────────────

    @Override
    public boolean valid(String resultId) {
        return kernel.result(resultId)
                .map(r -> r.validation() != null && r.validation().valid())
                .orElse(false);
    }

    @Override
    public String field(String resultId, String fieldName) {
        return kernel.result(resultId)
                .flatMap(r -> kernel.candidate(r.candidateId()))
                .map(c -> {
                    // 1. Try exact lookup
                    String val = c.fields().get(fieldName);
                    if (val != null && !val.isEmpty()) return val;

                    // 2. Try case-insensitive lookup
                    for (java.util.Map.Entry<String, String> entry : c.fields().entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(fieldName)) {
                            return entry.getValue();
                        }
                    }

                    // 3. Alias fallback: if looking for label/category/value, fall back to "answer"
                    if ("label".equalsIgnoreCase(fieldName) || "category".equalsIgnoreCase(fieldName) || "value".equalsIgnoreCase(fieldName)) {
                        String ans = c.fields().get("answer");
                        if (ans == null) {
                            for (java.util.Map.Entry<String, String> entry : c.fields().entrySet()) {
                                if (entry.getKey().equalsIgnoreCase("answer")) {
                                    ans = entry.getValue();
                                    break;
                                }
                            }
                        }
                        if (ans != null) return ans;
                    }

                    return "";
                })
                .orElse("");
    }

    @Override
    public String candidate(String resultId) {
        return kernel.result(resultId).map(ResourceResult::candidateId).orElse("");
    }

    @Override
    public String trace(String resultId) {
        return kernel.result(resultId).map(ResourceResult::traceId).orElse("");
    }

    @Override
    public String outcome(String resultId) {
        return kernel.result(resultId).map(r -> r.outcome().name()).orElse("UNKNOWN");
    }

    @Override
    public String knowledge(String agentId) {
        return kernel.acceptedKnowledge(agentId);
    }

    // ── Candidate deliberation ─────────────────────────────────

    @Override
    public boolean admissible(String candidateId) {
        return kernel.checkAdmissibility(candidateId).outcome() == Outcomes.AdmissibilityOutcome.ADMISSIBLE;
    }

    @Override
    public boolean accept(String candidateId) {
        return kernel.acceptCandidate(candidateId).isPresent();
    }

    @Override
    public boolean reject(String candidateId) {
        return kernel.rejectCandidate(candidateId).isPresent();
    }

    @Override
    public boolean assess(String assessorId, String candidateId,
                          String verdict, double confidence,
                          String explanation) {
        Outcomes.AssessmentVerdict v;
        try { v = Outcomes.AssessmentVerdict.valueOf(verdict.toUpperCase()); }
        catch (Exception e) { v = Outcomes.AssessmentVerdict.UNCERTAIN; }

        kernel.assess(assessorId, candidateId, "candidate",
                v, confidence, List.of(), List.of(), explanation);
        return true;
    }
}

