package generativelayers.kernel;

import java.util.List;
import java.util.Optional;

public final class KernelPorts {
    private KernelPorts() {}

    public interface GenerativeProvider {
        ProviderOutput generate(ResourceRequest request, Blob promptBlob) throws Exception;
    }

    public interface ResponseValidator {
        ValidationResult validate(String rawOutput, ResponseSchema schema);
    }

    public interface GovernancePolicy {
        PolicyDecision evaluate(ResourceRequest request);
    }

    public interface AdmissibilityChecker {
        AdmissibilityDecision check(Candidate candidate, List<Assessment> assessments);
    }

    public interface BlobStore {
        Blob put(Blob blob);
        Optional<Blob> get(String blobId);
        List<Blob> all();
    }

    public interface CandidateStore {
        Candidate put(Candidate candidate);
        Optional<Candidate> get(String candidateId);
        Candidate update(Candidate candidate);
        List<Candidate> all();
    }

    public interface AssessmentStore {
        Assessment put(Assessment assessment);
        Optional<Assessment> get(String assessmentId);
        List<Assessment> forTarget(String targetRef);
        List<Assessment> all();
    }

    public interface ResultStore {
        ResourceResult put(ResourceResult result);
        Optional<ResourceResult> get(String resultId);
        List<ResourceResult> all();
    }

    public interface TraceSink {
        TraceRecord record(TraceRecord trace);
        Optional<TraceRecord> get(String traceId);
        List<TraceRecord> all();
    }

    public interface MetricsSink {
        void increment(String metricName);
        void observe(String metricName, double value);
        List<String> events();
    }
}
