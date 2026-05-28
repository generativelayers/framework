package gl.GovernanceKernel;

import java.time.Instant;
import java.util.List;

public record BeliefSnapshot(String snapshotId, String agentId, List<String> beliefTerms, List<String> exposedPredicates, Instant createdAt) {
    public BeliefSnapshot {
        snapshotId = snapshotId == null || snapshotId.isBlank() ? Ids.id("belief_snapshot") : snapshotId;
        agentId = agentId == null ? "" : agentId;
        beliefTerms = beliefTerms == null ? List.of() : List.copyOf(beliefTerms);
        exposedPredicates = exposedPredicates == null ? List.of() : List.copyOf(exposedPredicates);
        createdAt = createdAt == null ? Ids.now() : createdAt;
    }
    public static BeliefSnapshot empty(String agentId) { return new BeliefSnapshot("belief_snapshot_empty", agentId, List.of(), List.of(), Ids.now()); }
}
