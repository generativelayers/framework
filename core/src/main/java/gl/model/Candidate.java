package gl.model;

import gl.Ids;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A governed piece of generative output.
 *  Candidates follow a strict lifecycle:
 *  VALIDATED / INVALID -> ASSESSED -> ACCEPTED_BY_AGENT / REJECTED_BY_AGENT.
 *  INVALID candidates cannot be assessed or accepted.
 *  No generated content enters the agent's belief base without explicit acceptance. */
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
