package gl.adapter;

/**
 * GL v2 Public API -- 13 governed candidate-material lifecycle commands.
 *
 * <pre>
 * see > bind > call > result > candidate > check > get
 *     > judge > decide > accept/reject > knowledge > explain
 * </pre>
 *
 * <p>Every command either discovers/binds an external resource,
 * performs a governed call, maps an invocation result into isolated
 * candidate material, checks or extracts candidate content, records
 * judgement evidence, computes admissibility, records an accept/reject
 * decision, exposes accepted GL-side knowledge, or explains the lifecycle.
 *
 * <p>Generated material never becomes BDI belief automatically;
 * BDI adoption remains an explicit host-agent operation.
 */
public interface ResourceActions {

    // -- Discovery --

    /** Discover available external generative resources.
     *  Returns structured availability report. */
    String see();

    // -- Binding --

    /** Bind an agent to a provider/model/config.
     *  @return bindingId or ERROR:reason */
    String bind(String agentId, String provider, String model, String config);

    // -- Invocation --

    /** Perform one governed external resource invocation.
     *  Agent identity comes from the binding.
     *  @return resultId or ERROR:reason */
    String call(String bindingId, String goalId, String bodyId,
                String affordance, String prompt, String requiredFields, String context);

    // -- Result/Candidate inspection --

    /** Inspect invocation-level outcome (SUCCESS, INVALID_OUTPUT, etc.).
     *  @return outcome name or ERROR:not_found */
    String result(String resultId);

    /** Return the candidateId created from an invocation result.
     *  This is the ontological boundary: InvocationResult > Candidate.
     *  @return candidateId or ERROR:not_found */
    String candidate(String resultId);

    /** Check governance state of a result or candidate.
     *  For results: validation (RESULT:VALID or RESULT:INVALID:reasons).
     *  For candidates: lifecycle status (CANDIDATE:STATUS=X).
     *  Does NOT compute admissibility -- that is decide().
     *  @return prefixed status string or ERROR:... */
    String check(String refId);

    /** Project a field from candidate material.
     *  Takes candidateId, not resultId.
     *  @return field value or ERROR:not_found/ERROR:missing_field */
    String get(String candidateId, String fieldName);

    // -- Assessment --

    /** Record evaluative evidence about a candidate.
     *  Verdicts: APPROVE, WARN, REJECT_VERDICT, UNCERTAIN.
     *  @return assessmentId or ERROR:... */
    String judge(String candidateId, String assessorId, String verdict,
                 double confidence, String rationale);

    // -- Decision --

    /** Compute decision-readiness under governance rules.
     *  Does NOT accept or reject -- only reports admissibility.
     *  @return ADMISSIBLE or INADMISSIBLE:reason or ERROR:not_found */
    String decide(String candidateId);

    /** Record positive decision with reason. Requires admissibility.
     *  @return decisionId or ERROR:not_found/ERROR:not_admissible */
    String accept(String candidateId, String reason);

    /** Record negative decision with reason. Always allowed if candidate exists.
     *  @return decisionId or ERROR:not_found */
    String reject(String candidateId, String reason);

    // -- Knowledge --

    /** Return accepted GL-side knowledge for an agent.
     *  This is NOT the BDI belief base -- it is accepted candidate material
     *  managed by GL, available as context for later calls.
     *  @return semicolon-separated field entries or EMPTY */
    String knowledge(String agentId);

    // -- Explanation --

    /** Return trace/audit/explanation for any lifecycle reference.
     *  Supports result (res_*), candidate (cand_*), assessment (assess_*),
     *  decision (dec_*), trace (trace_*), and binding (bind_*) IDs.
     *  @return structured audit string or ERROR:not_found */
    String explain(String refId);
}
