package layer.kernel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exports Generative Layer kernel state (traces, candidates, assessments, blobs, results)
 * as structured Map objects suitable for JSON/CSV serialisation.
 *
 * This utility is essential for experiment reproducibility: it converts the
 * in-memory kernel state into portable data that can be written to files,
 * compared across ASTRA/Jason runs, and included in paper results.
 *
 * No external JSON library dependency — produces Map/List structures that
 * can be serialised by any JSON library the caller prefers.
 */
public final class TraceExporter {
    private TraceExporter() {}

    /**
     * Export a single trace record as a map.
     */
    public static Map<String, Object> exportTrace(TraceRecord trace) {
        if (trace == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("traceId", trace.traceId());
        map.put("requestId", trace.requestId());
        map.put("agentId", trace.agentId());
        map.put("goalId", trace.goalId());
        map.put("resourceId", trace.resourceId());
        map.put("taskType", trace.taskType());
        map.put("promptBlobId", trace.promptBlobId());
        map.put("outputBlobId", trace.outputBlobId());
        map.put("candidateId", trace.candidateId());
        map.put("assessmentId", trace.assessmentId());
        map.put("outcome", trace.outcome().name());
        if (trace.policyDecision() != null) {
            map.put("policyOutcome", trace.policyDecision().outcome().name());
            map.put("policyReason", trace.policyDecision().reason());
        }
        if (trace.validationResult() != null) {
            map.put("validationValid", trace.validationResult().valid());
            map.put("validationMessage", trace.validationResult().message());
            if (!trace.validationResult().errors().isEmpty()) {
                map.put("validationErrors", trace.validationResult().errors());
            }
        }
        map.put("createdAt", trace.createdAt().toString());
        return map;
    }

    /**
     * Export a single candidate as a map.
     */
    public static Map<String, Object> exportCandidate(Candidate candidate) {
        if (candidate == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("candidateId", candidate.candidateId());
        map.put("type", candidate.type().name());
        map.put("status", candidate.status().name());
        map.put("sourceResultId", candidate.sourceResultId());
        map.put("sourceBlobId", candidate.sourceBlobId());
        map.put("agentId", candidate.agentId());
        map.put("goalId", candidate.goalId());
        if (!candidate.fields().isEmpty()) {
            map.put("fields", new LinkedHashMap<>(candidate.fields()));
        }
        if (!candidate.evidenceRefs().isEmpty()) {
            map.put("evidenceRefs", candidate.evidenceRefs());
        }
        map.put("createdAt", candidate.createdAt().toString());
        return map;
    }

    /**
     * Export a single assessment as a map.
     */
    public static Map<String, Object> exportAssessment(Assessment assessment) {
        if (assessment == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("assessmentId", assessment.assessmentId());
        map.put("assessorId", assessment.assessorId());
        map.put("targetRef", assessment.targetRef());
        map.put("targetType", assessment.targetType());
        map.put("verdict", assessment.verdict().name());
        map.put("confidence", assessment.confidence());
        if (!assessment.criteria().isEmpty()) {
            map.put("criteria", assessment.criteria());
        }
        if (!assessment.evidenceRefs().isEmpty()) {
            map.put("evidenceRefs", assessment.evidenceRefs());
        }
        map.put("explanation", assessment.explanation());
        map.put("createdAt", assessment.createdAt().toString());
        return map;
    }

    /**
     * Export a single resource result as a map.
     */
    public static Map<String, Object> exportResult(ResourceResult result) {
        if (result == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("resultId", result.resultId());
        map.put("requestId", result.requestId());
        map.put("outcome", result.outcome().name());
        map.put("success", result.success());
        map.put("promptBlobId", result.promptBlobId());
        map.put("outputBlobId", result.outputBlobId());
        map.put("candidateId", result.candidateId());
        map.put("traceId", result.traceId());
        map.put("message", result.message());
        if (result.validation() != null) {
            map.put("validationValid", result.validation().valid());
        }
        if (result.policyDecision() != null) {
            map.put("policyOutcome", result.policyDecision().outcome().name());
        }
        map.put("createdAt", result.createdAt().toString());
        return map;
    }

    /**
     * Export a full experiment snapshot from the kernel: all traces, candidates,
     * assessments, results, and metrics.
     */
    public static Map<String, Object> exportAll(Kernel kernel) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("traces", kernel.traces().stream()
                .map(TraceExporter::exportTrace)
                .collect(Collectors.toList()));
        snapshot.put("metrics", kernel.metrics());
        return snapshot;
    }

    /**
     * Export all traces as a list of CSV lines (header + rows).
     * Useful for quick experiment data in spreadsheets.
     */
    public static List<String> exportTracesAsCsv(List<TraceRecord> traces) {
        String header = "traceId,requestId,agentId,goalId,resourceId,taskType,outcome,policyOutcome,validationValid,createdAt";
        List<String> rows = traces.stream().map(t -> {
            String policyOutcome = t.policyDecision() != null ? t.policyDecision().outcome().name() : "";
            String validationValid = t.validationResult() != null ? String.valueOf(t.validationResult().valid()) : "";
            return String.join(",",
                    t.traceId(), t.requestId(), t.agentId(), t.goalId(),
                    t.resourceId(), t.taskType(), t.outcome().name(),
                    policyOutcome, validationValid, t.createdAt().toString());
        }).collect(Collectors.toList());

        rows.add(0, header);
        return rows;
    }
}
