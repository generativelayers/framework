package gl;

import gl.model.*;

import java.util.List;
import java.util.Optional;

/**
 * Service Provider Interfaces (SPI) for the {@link GovernanceKernel}.
 *
 * <p>All kernel behaviour is defined through these pluggable ports:
 * <ul>
 *   <li>{@code GenerativeProvider} -- LLM backend</li>
 *   <li>{@code ResponseValidator} -- post-generation schema validation</li>
 *   <li>{@code GovernancePolicy} -- pre-generation policy gate</li>
 *   <li>{@code AdmissibilityChecker} -- pre-acceptance admissibility gate</li>
 *   <li>{@code BlobStore} -- content-addressed storage</li>
 *   <li>{@code CandidateStore} -- governed candidate lifecycle</li>
 *   <li>{@code AssessmentStore} -- peer assessment records</li>
 *   <li>{@code ResultStore} -- generation result records</li>
 *   <li>{@code TraceSink} -- append-only audit trail</li>
 *   <li>{@code MetricsSink} -- lightweight metrics collector</li>
 * </ul>
 */
public final class KernelPorts {
    private KernelPorts() {}

    /** LLM backend that produces raw text from a prompt and request context. */
    public interface GenerativeProvider {
        ProviderOutput generate(ResourceRequest request, Blob promptBlob) throws Exception;
    }

    /** Post-generation schema validator: checks raw output against a {@link ResponseSchema}. */
    public interface ResponseValidator {
        ValidationResult validate(String rawOutput, ResponseSchema schema);
    }

    /** Pre-generation policy gate: decides whether a generation request is allowed, denied, or escalated. */
    public interface GovernancePolicy {
        PolicyDecision evaluate(ResourceRequest request);
    }

    /** Pre-acceptance admissibility check: evaluates a candidate against its assessments. */
    public interface AdmissibilityChecker {
        AdmissibilityDecision check(Candidate candidate, List<Assessment> assessments);
    }

    /** Content-addressed storage for prompts, outputs, and evidence blobs. */
    public interface BlobStore {
        Blob put(Blob blob);
        Optional<Blob> get(String blobId);
        List<Blob> all();
    }

    /** CRUD store for governed candidates throughout their lifecycle. */
    public interface CandidateStore {
        Candidate put(Candidate candidate);
        Optional<Candidate> get(String candidateId);
        /** Reverse lookup: find candidate by the result ID that created it. O(1). */
        Optional<Candidate> byResultId(String resultId);
        Candidate update(Candidate candidate);
        List<Candidate> all();
    }

    /** Store for peer assessment records linked to candidates. */
    public interface AssessmentStore {
        Assessment put(Assessment assessment);
        Optional<Assessment> get(String assessmentId);
        List<Assessment> forTarget(String targetRef);
        List<Assessment> all();
    }

    /** Store for generation result records linking requests to outcomes. */
    public interface ResultStore {
        ResourceResult put(ResourceResult result);
        Optional<ResourceResult> get(String resultId);
        List<ResourceResult> all();
    }

    /** Append-only audit trail for generation invocations. */
    public interface TraceSink {
        TraceRecord record(TraceRecord trace);
        Optional<TraceRecord> get(String traceId);
        List<TraceRecord> all();
    }

    /** Lightweight metrics collector for invocation counters and observations. */
    public interface MetricsSink {
        void increment(String metricName);
        void observe(String metricName, double value);
        List<String> events();
    }

    /** Store for accept/reject decision records linked to candidates. */
    public interface DecisionStore {
        Decision put(Decision decision);
        Optional<Decision> get(String decisionId);
        List<Decision> forCandidate(String candidateId);
        List<Decision> all();
    }
}
