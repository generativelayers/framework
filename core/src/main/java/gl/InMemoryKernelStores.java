package gl;

import gl.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory implementations of the kernel's storage ports.
 *  Provides thread-safe ConcurrentHashMap-backed stores for
 *  candidates, assessments, results, blobs, and traces.
 *  Suitable for single-JVM deployments and testing. */
public final class InMemoryKernelStores {
    private InMemoryKernelStores() {}

    public static final class Blobs implements KernelPorts.BlobStore {
        private final ConcurrentHashMap<String, Blob> values = new ConcurrentHashMap<>();
        public Blob put(Blob blob) { values.put(blob.blobId(), blob); return blob; }
        public Optional<Blob> get(String blobId) { return Optional.ofNullable(values.get(blobId)); }
        public List<Blob> all() { return List.copyOf(values.values()); }
    }

    public static final class Candidates implements KernelPorts.CandidateStore {
        private final ConcurrentHashMap<String, Candidate> values = new ConcurrentHashMap<>();
        /** Reverse index: sourceResultId → candidateId for O(1) lookup. */
        private final ConcurrentHashMap<String, String> resultIndex = new ConcurrentHashMap<>();
        public Candidate put(Candidate candidate) {
            values.put(candidate.candidateId(), candidate);
            if (!candidate.sourceResultId().isBlank()) {
                resultIndex.put(candidate.sourceResultId(), candidate.candidateId());
            }
            return candidate;
        }
        public Optional<Candidate> get(String candidateId) { return Optional.ofNullable(values.get(candidateId)); }
        public Optional<Candidate> byResultId(String resultId) {
            if (resultId == null || resultId.isBlank()) return Optional.empty();
            String cid = resultIndex.get(resultId);
            return cid == null ? Optional.empty() : get(cid);
        }
        public Candidate update(Candidate candidate) { values.put(candidate.candidateId(), candidate); return candidate; }
        public List<Candidate> all() { return List.copyOf(values.values()); }
    }

    public static final class Assessments implements KernelPorts.AssessmentStore {
        private final ConcurrentHashMap<String, Assessment> values = new ConcurrentHashMap<>();
        /** Reverse index: targetRef → list of assessmentIds for O(1) forTarget(). */
        private final ConcurrentHashMap<String, List<String>> targetIndex = new ConcurrentHashMap<>();
        public Assessment put(Assessment assessment) {
            values.put(assessment.assessmentId(), assessment);
            targetIndex.computeIfAbsent(assessment.targetRef(),
                    k -> java.util.Collections.synchronizedList(new ArrayList<>()))
                    .add(assessment.assessmentId());
            return assessment;
        }
        public Optional<Assessment> get(String assessmentId) { return Optional.ofNullable(values.get(assessmentId)); }
        public List<Assessment> forTarget(String targetRef) {
            List<String> ids = targetIndex.get(targetRef);
            if (ids == null) return List.of();
            synchronized (ids) {
                return ids.stream().map(values::get).filter(java.util.Objects::nonNull).toList();
            }
        }
        public List<Assessment> all() { return List.copyOf(values.values()); }
    }

    public static final class Results implements KernelPorts.ResultStore {
        private final ConcurrentHashMap<String, ResourceResult> values = new ConcurrentHashMap<>();
        public ResourceResult put(ResourceResult result) { values.put(result.resultId(), result); return result; }
        public Optional<ResourceResult> get(String resultId) { return Optional.ofNullable(values.get(resultId)); }
        public List<ResourceResult> all() { return List.copyOf(values.values()); }
    }

    public static final class Traces implements KernelPorts.TraceSink {
        private final ConcurrentHashMap<String, TraceRecord> values = new ConcurrentHashMap<>();
        public TraceRecord record(TraceRecord trace) { values.put(trace.traceId(), trace); return trace; }
        public Optional<TraceRecord> get(String traceId) { return Optional.ofNullable(values.get(traceId)); }
        public List<TraceRecord> all() { return List.copyOf(values.values()); }
    }

    public static final class Metrics implements KernelPorts.MetricsSink {
        private final List<String> events = java.util.Collections.synchronizedList(new ArrayList<>());
        public void increment(String metricName) { events.add("increment:" + metricName); }
        public void observe(String metricName, double value) { events.add("observe:" + metricName + "=" + value); }
        public List<String> events() { return List.copyOf(events); }
    }

    public static final class Decisions implements KernelPorts.DecisionStore {
        private final ConcurrentHashMap<String, Decision> values = new ConcurrentHashMap<>();
        /** Reverse index: candidateId → list of decisionIds for O(1) forCandidate(). */
        private final ConcurrentHashMap<String, List<String>> candidateIndex = new ConcurrentHashMap<>();
        public Decision put(Decision decision) {
            values.put(decision.decisionId(), decision);
            candidateIndex.computeIfAbsent(decision.candidateId(),
                    k -> java.util.Collections.synchronizedList(new ArrayList<>()))
                    .add(decision.decisionId());
            return decision;
        }
        public Optional<Decision> get(String decisionId) { return Optional.ofNullable(values.get(decisionId)); }
        public List<Decision> forCandidate(String candidateId) {
            List<String> ids = candidateIndex.get(candidateId);
            if (ids == null) return List.of();
            synchronized (ids) {
                return ids.stream().map(values::get).filter(java.util.Objects::nonNull).toList();
            }
        }
        public List<Decision> all() { return List.copyOf(values.values()); }
    }
}
