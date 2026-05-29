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
        public Candidate put(Candidate candidate) { values.put(candidate.candidateId(), candidate); return candidate; }
        public Optional<Candidate> get(String candidateId) { return Optional.ofNullable(values.get(candidateId)); }
        public Candidate update(Candidate candidate) { values.put(candidate.candidateId(), candidate); return candidate; }
        public List<Candidate> all() { return List.copyOf(values.values()); }
    }

    public static final class Assessments implements KernelPorts.AssessmentStore {
        private final ConcurrentHashMap<String, Assessment> values = new ConcurrentHashMap<>();
        public Assessment put(Assessment assessment) { values.put(assessment.assessmentId(), assessment); return assessment; }
        public Optional<Assessment> get(String assessmentId) { return Optional.ofNullable(values.get(assessmentId)); }
        public List<Assessment> forTarget(String targetRef) { return values.values().stream().filter(a -> a.targetRef().equals(targetRef)).toList(); }
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
        private final List<String> events = new ArrayList<>();
        public void increment(String metricName) { events.add("increment:" + metricName); }
        public void observe(String metricName, double value) { events.add("observe:" + metricName + "=" + value); }
        public List<String> events() { return List.copyOf(events); }
    }
}
