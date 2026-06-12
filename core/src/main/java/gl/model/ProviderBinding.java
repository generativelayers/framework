package gl.model;

import gl.GovernanceKernel;
import gl.Ids;
import gl.body.GenerativeBodyRegistry;
import gl.provider.ProviderConfig;

import java.time.Instant;

/**
 * A per-agent provider binding created by {@code bind()}.
 *
 * <p>Bindings isolate provider/model/config per agent.
 * Lifecycle stores (results, candidates, assessments, decisions, traces)
 * remain shared so that lifecycle objects are globally resolvable by ID.
 *
 * <p>The {@code agentId} stored here is propagated by {@code call()} into
 * every {@code ResourceRequest}, {@code Candidate}, and {@code TraceRecord},
 * so that {@code knowledge(agentId)} can later retrieve accepted material.
 */
public record ProviderBinding(
        String bindingId,
        String agentId,
        String providerName,
        String modelName,
        GovernanceKernel kernel,
        GenerativeBodyRegistry bodies,
        ProviderConfig config,
        Instant createdAt
) {
    public ProviderBinding {
        bindingId = bindingId == null || bindingId.isBlank() ? Ids.id("bind") : bindingId;
        agentId = agentId == null ? "" : agentId;
        providerName = providerName == null ? "" : providerName;
        modelName = modelName == null ? "" : modelName;
        createdAt = createdAt == null ? Ids.now() : createdAt;
    }
}
