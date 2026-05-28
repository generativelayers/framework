package grl.kernel;

import java.time.Instant;
import java.util.List;

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
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        explanation = explanation == null ? "" : explanation;
        createdAt = createdAt == null ? Ids.now() : createdAt;
    }
}
