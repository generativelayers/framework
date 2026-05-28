package grl.tests;

import grl.kernel.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TraceExporter — verifies that kernel state can be exported
 * as structured data for experiment reproducibility.
 */
final class TraceExporterTest {

    @Test
    void exportTraceContainsAllFields() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of()
        ));

        TraceRecord trace = kernel.trace(result.traceId()).orElseThrow();
        Map<String, Object> exported = TraceExporter.exportTrace(trace);

        assertEquals("agent_a", exported.get("agentId"));
        assertEquals("goal_1", exported.get("goalId"));
        assertEquals("SUCCESS", exported.get("outcome"));
        assertEquals("ALLOW", exported.get("policyOutcome"));
        assertEquals(true, exported.get("validationValid"));
        assertNotNull(exported.get("createdAt"));
    }

    @Test
    void exportCandidateContainsStatusAndFields() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        ResourceResult result = kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label and confidence",
                ResponseSchema.required("schema", List.of("label", "confidence")),
                GovernanceContext.empty(), null, Map.of()
        ));

        Candidate candidate = kernel.candidate(result.candidateId()).orElseThrow();
        Map<String, Object> exported = TraceExporter.exportCandidate(candidate);

        assertEquals("VALIDATED", exported.get("status"));
        assertEquals("CANDIDATE_ANSWER", exported.get("type"));
        assertTrue(exported.containsKey("fields"));
        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) exported.get("fields");
        assertEquals("test", fields.get("label"));
    }

    @Test
    void exportAllReturnsTracesAndMetrics() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of()
        ));

        Map<String, Object> snapshot = TraceExporter.exportAll(kernel);
        assertTrue(snapshot.containsKey("traces"));
        assertTrue(snapshot.containsKey("metrics"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> traces = (List<Map<String, Object>>) snapshot.get("traces");
        assertFalse(traces.isEmpty());
    }

    @Test
    void exportTracesAsCsvHasHeaderAndData() {
        Kernel kernel = KernelFactory.deterministicInMemory();
        kernel.invoke(new ResourceRequest(
                null, "agent_a", "goal_1", "llm.answer", "answer", CandidateType.CANDIDATE_ANSWER,
                "Return label", ResponseSchema.required("schema", List.of("label")),
                GovernanceContext.empty(), null, Map.of()
        ));

        List<String> csv = TraceExporter.exportTracesAsCsv(kernel.traces());
        assertTrue(csv.get(0).startsWith("traceId,"), "First line must be CSV header");
        assertEquals(2, csv.size(), "One header + one data row");
        assertTrue(csv.get(1).contains("agent_a"));
        assertTrue(csv.get(1).contains("SUCCESS"));
    }
}
