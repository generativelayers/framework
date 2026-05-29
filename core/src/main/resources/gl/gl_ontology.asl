/**
 * gl_ontology.asl — Shared Generative Layer beliefs for Jason MAS.
 *
 * All GL-aware Jason agents should include this file:
 *   { include("gl_ontology.asl") }
 *
 * This provides a shared vocabulary for candidates, assessments,
 * and verdicts — the Jason equivalent of the ASTRA GLOntology agent.
 *
 * These are initial beliefs that describe the GL type system.
 * Agents can query these to validate message content at runtime.
 */

// ── GL Type Definitions (as initial beliefs) ────────────────────

// Candidate status values
gl_status(proposed).
gl_status(validated).
gl_status(assessed).
gl_status(accepted_by_agent).
gl_status(rejected_by_agent).
gl_status(invalid).

// Candidate types
gl_candidate_type(candidate_answer).
gl_candidate_type(candidate_belief).
gl_candidate_type(candidate_plan).
gl_candidate_type(task_decomposition).
gl_candidate_type(action_proposal).
gl_candidate_type(tool_call_proposal).
gl_candidate_type(grounded_fact).
gl_candidate_type(memory_use).
gl_candidate_type(reflection_note).
gl_candidate_type(explanation).

// Assessment verdicts
gl_verdict_type(accept).
gl_verdict_type(reject).
gl_verdict_type(uncertain).
gl_verdict_type(needs_evidence).
gl_verdict_type(needs_human).
gl_verdict_type(retry).

// Affordance types
gl_affordance(answer).
gl_affordance(classify).
gl_affordance(summarise).
gl_affordance(ground_fact).
gl_affordance(decompose_goal).
gl_affordance(propose_tool_call).
gl_affordance(propose_action).
gl_affordance(retrieve_memory).
gl_affordance(reflect).
gl_affordance(critique).
gl_affordance(assess).
gl_affordance(explain).
gl_affordance(escalate).

// Outcome types
gl_outcome_type(success).
gl_outcome_type(governance_denied).
gl_outcome_type(governance_escalated).
gl_outcome_type(provider_failed).
gl_outcome_type(invalid_output).

// ── GL Validation Rules ─────────────────────────────────────────

// Check if a verdict is a valid GL verdict
gl_valid_verdict(V) :- gl_verdict_type(V).

// Check if a status is a valid GL candidate status
gl_valid_status(S) :- gl_status(S).

// Check if an affordance is a valid GL affordance
gl_valid_affordance(A) :- gl_affordance(A).
