package gl.tests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves that explain(candidateId) returns full lifecycle evidence
 * after the complete GL v2 lifecycle:
 *
 *   bind -> call -> candidate -> judge -> decide -> accept -> explain
 *
 * This test uses only the 13 public GL v2 commands via DirectAdapter.
 */
class ExplainLifecycleTest {

    @Test
    void explainCandidateShowsFullLifecycleEvidence() {
        gl.adapter.DirectAdapter gl = new gl.adapter.DirectAdapter();

        // -- bind --
        String bid = gl.bind("agent1", "fake", "", "");
        assertFalse(bid.startsWith("ERROR"), "bind must succeed: " + bid);

        // -- call --
        String rid = gl.call(bid, "classify", "llm.answer", "ANSWER",
                "Classify: apple", "label,confidence", "");
        assertFalse(rid.startsWith("ERROR"), "call must succeed: " + rid);

        // -- candidate --
        String cid = gl.candidate(rid);
        assertFalse(cid.startsWith("ERROR"), "candidate must succeed: " + cid);

        // -- judge --
        String aid = gl.judge(cid, "reviewer", "APPROVE", 0.9, "looks correct");
        assertFalse(aid.startsWith("ERROR"), "judge must succeed: " + aid);

        // -- decide --
        String admResult = gl.decide(cid);
        assertTrue(admResult.startsWith("ADMISSIBLE"), "decide must return ADMISSIBLE: " + admResult);

        // -- accept --
        String did = gl.accept(cid, "valid classification");
        assertFalse(did.startsWith("ERROR"), "accept must succeed: " + did);

        // -- explain(candidateId) -- the key test --
        String explanation = gl.explain(cid);
        assertFalse(explanation.startsWith("ERROR"), "explain must succeed: " + explanation);

        // Core candidate fields
        assertTrue(explanation.contains("type=candidate"), "must contain type");
        assertTrue(explanation.contains("id=" + cid), "must contain candidate id");
        assertTrue(explanation.contains("agent=agent1"), "must contain agent");
        assertTrue(explanation.contains("status=ACCEPTED_BY_AGENT"), "must contain accepted status");

        // Assessment evidence
        assertTrue(explanation.contains("assessment_count=1"), "must count assessments");
        assertTrue(explanation.contains("assessment_verdict=APPROVE"), "must contain verdict");
        assertTrue(explanation.contains("assessment_confidence=0.9"), "must contain confidence");
        assertTrue(explanation.contains("assessment_rationale=looks correct"), "must contain rationale");

        // Admissibility
        assertTrue(explanation.contains("admissibility=FINAL:ACCEPTED_BY_AGENT"), "must contain final admissibility for decided candidate");

        // Decision evidence
        assertTrue(explanation.contains("decision_count=1"), "must count decisions");
        assertTrue(explanation.contains("decision=ACCEPTED"), "must contain decision type");
        assertTrue(explanation.contains("decision_reason=valid classification"), "must contain decision reason");

        // Print for demo/thesis screenshot
        System.out.println("=== explain(candidateId) -- full lifecycle ===");
        for (String part : explanation.split(";")) {
            System.out.println("  " + part);
        }
    }

    @Test
    void explainCandidateShowsRejectionEvidence() {
        gl.adapter.DirectAdapter gl = new gl.adapter.DirectAdapter();
        String bid = gl.bind("agent1", "fake", "", "");
        String rid = gl.call(bid, "classify", "llm.answer", "ANSWER",
                "Classify: brick", "label", "");
        String cid = gl.candidate(rid);

        // Judge negatively
        gl.judge(cid, "reviewer", "REJECT_VERDICT", 0.8, "wrong category");

        // Reject
        String did = gl.reject(cid, "material is incorrect");
        assertFalse(did.startsWith("ERROR"), "reject must succeed: " + did);

        String explanation = gl.explain(cid);

        assertTrue(explanation.contains("status=REJECTED_BY_AGENT"), "must show rejected status");
        assertTrue(explanation.contains("assessment_verdict=REJECT_VERDICT"), "must show reject verdict");
        assertTrue(explanation.contains("decision=REJECTED"), "must show decision type");
        assertTrue(explanation.contains("decision_reason=material is incorrect"), "must show rejection reason");

        System.out.println("=== explain(candidateId) -- rejection ===");
        for (String part : explanation.split(";")) {
            System.out.println("  " + part);
        }
    }

    @Test
    void explainCandidateBeforeDecisionShowsPartialEvidence() {
        gl.adapter.DirectAdapter gl = new gl.adapter.DirectAdapter();
        String bid = gl.bind("agent1", "fake", "", "");
        String rid = gl.call(bid, "goal1", "llm.answer", "ANSWER",
                "Some prompt", "label,confidence", "");
        String cid = gl.candidate(rid);

        // Only judge, no decision yet
        gl.judge(cid, "peer", "APPROVE", 0.7, "partial review");

        String explanation = gl.explain(cid);

        assertTrue(explanation.contains("assessment_count=1"), "must have 1 assessment");
        assertTrue(explanation.contains("assessment_verdict=APPROVE"), "must show verdict");
        assertTrue(explanation.contains("decision_count=0"), "must have 0 decisions");
        assertFalse(explanation.contains("decision="), "must not have decision detail");
        assertTrue(explanation.contains("admissibility=ADMISSIBLE"), "must show admissibility");
    }

    @Test
    void explainBindingShowsProviderAndBodies() {
        gl.adapter.DirectAdapter gl = new gl.adapter.DirectAdapter();
        String bid = gl.bind("agent1", "fake", "", "");

        String explanation = gl.explain(bid);
        assertFalse(explanation.startsWith("ERROR"), "explain(bindingId) must succeed: " + explanation);

        assertTrue(explanation.contains("type=binding"), "must contain type");
        assertTrue(explanation.contains("id=" + bid), "must contain binding id");
        assertTrue(explanation.contains("agent=agent1"), "must contain agent");
        assertTrue(explanation.contains("provider=fake"), "must contain provider");
        assertTrue(explanation.contains("config_keys="), "must list config keys");

        // Standard bodies must be registered
        assertTrue(explanation.contains("llm.answer"), "must list llm.answer body");
        assertTrue(explanation.contains("rag.ground"), "must list rag.ground body");
        assertTrue(explanation.contains("planner.decompose"), "must list planner.decompose body");

        assertTrue(explanation.contains("created="), "must contain timestamp");

        System.out.println("=== explain(bindingId) ===");
        for (String part : explanation.split(";")) {
            System.out.println("  " + part);
        }
    }

    @Test
    void lifecycleFinalityPreventsRegressionAfterAccept() {
        gl.adapter.DirectAdapter gl = new gl.adapter.DirectAdapter();
        String bid = gl.bind("agent1", "fake", "", "");
        String rid = gl.call(bid, "goal1", "llm.answer", "ANSWER", "test", "label,confidence", "");
        String cid = gl.candidate(rid);
        gl.judge(cid, "reviewer", "APPROVE", 0.9, "good");
        gl.accept(cid, "approved");

        // After acceptance, ALL lifecycle-advancing commands must be blocked
        String judgeResult = gl.judge(cid, "reviewer2", "APPROVE", 0.8, "late review");
        assertTrue(judgeResult.startsWith("ERROR:already_decided:ACCEPTED_BY_AGENT"),
                "judge after accept must fail: " + judgeResult);

        String acceptResult = gl.accept(cid, "double accept");
        assertTrue(acceptResult.startsWith("ERROR:already_decided:ACCEPTED_BY_AGENT"),
                "accept after accept must fail: " + acceptResult);

        String rejectResult = gl.reject(cid, "flip decision");
        assertTrue(rejectResult.startsWith("ERROR:already_decided:ACCEPTED_BY_AGENT"),
                "reject after accept must fail: " + rejectResult);
    }

    @Test
    void lifecycleFinalityPreventsRegressionAfterReject() {
        gl.adapter.DirectAdapter gl = new gl.adapter.DirectAdapter();
        String bid = gl.bind("agent1", "fake", "", "");
        String rid = gl.call(bid, "goal1", "llm.answer", "ANSWER", "test", "label,confidence", "");
        String cid = gl.candidate(rid);
        gl.judge(cid, "reviewer", "REJECT_VERDICT", 0.9, "bad");
        gl.reject(cid, "rejected");

        String judgeResult = gl.judge(cid, "reviewer2", "APPROVE", 0.8, "reconsider");
        assertTrue(judgeResult.startsWith("ERROR:already_decided:REJECTED_BY_AGENT"),
                "judge after reject must fail: " + judgeResult);

        String acceptResult = gl.accept(cid, "flip decision");
        assertTrue(acceptResult.startsWith("ERROR:already_decided:REJECTED_BY_AGENT"),
                "accept after reject must fail: " + acceptResult);

        String rejectResult = gl.reject(cid, "double reject");
        assertTrue(rejectResult.startsWith("ERROR:already_decided:REJECTED_BY_AGENT"),
                "reject after reject must fail: " + rejectResult);
    }
}
