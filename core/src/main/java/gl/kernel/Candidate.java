package gl.GovernanceKernel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record Candidate(
        String candidateId,
        CandidateType type,
        CandidateStatus status,
        String sourceResultId,
        String sourceBlobId,
        String agentId,
        String goalId,
        Map<String, String> fields,
        List<String> evidenceRefs,
        Instant createdAt
) {
    public Candidate {
        candidateId = candidateId == null || candidateId.isBlank() ? Ids.id("cand") : candidateId;
        type = type == null ? CandidateType.CANDIDATE_ANSWER : type;
        status = status == null ? CandidateStatus.PROPOSED : status;
        sourceResultId = sourceResultId == null ? "" : sourceResultId;
        sourceBlobId = sourceBlobId == null ? "" : sourceBlobId;
        agentId = agentId == null ? "" : agentId;
        goalId = goalId == null ? "" : goalId;
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        createdAt = createdAt == null ? Ids.now() : createdAt;
    }

    public Candidate withStatus(CandidateStatus newStatus) {
        return new Candidate(candidateId, type, newStatus, sourceResultId, sourceBlobId, agentId, goalId, fields, evidenceRefs, createdAt);
    }
}
