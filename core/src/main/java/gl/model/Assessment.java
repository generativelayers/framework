package gl.model;

import gl.Outcomes;
import gl.Ids;

import java.time.Instant;
import java.util.List;

/** Peer assessment of a candidate by an assessor agent.
 *  Contains a verdict (APPROVE/REJECT_VERDICT/WARN/UNCERTAIN), confidence score,
 *  evaluation criteria, evidence references, and free-text explanation.
 *  Multiple assessments may target the same candidate. */
public record Assessment(
        String assessmentId,
        String assessorId,
        String targetRef,
        String targetType,
        Outcomes.AssessmentVerdict verdict,
        double confidence,
        List<String> criteria,
        List<String> evidenceRefs,
        String explanation,
        Instant createdAt
) {
    public Assessment {
        assessmentId = assessmentId == null || assessmentId.isBlank() ? Ids.id("assess") : assessmentId;
        assessorId = assessorId == null ? "" : assessorId;
        targetRef = targetRef == null ? "" : targetRef;
        targetType = targetType == null ? "" : targetType;
        verdict = verdict == null ? Outcomes.AssessmentVerdict.UNCERTAIN : verdict;
        confidence = Double.isNaN(confidence) || Double.isInfinite(confidence)
                ? 0.0 : Math.max(0.0, Math.min(1.0, confidence));
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        explanation = explanation == null ? "" : explanation;
        createdAt = createdAt == null ? Ids.now() : createdAt;
    }
}
