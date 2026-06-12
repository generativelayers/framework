/**
 * Domain model -- all records and enums used across the GL framework.
 *
 * <h2>Candidate Lifecycle</h2>
 * <ul>
 *   <li>{@link gl.model.Candidate} -- governed generative output</li>
 *   <li>{@link gl.model.CandidateStatus} -- lifecycle states</li>
 *   <li>{@link gl.model.CandidateType} -- semantic type</li>
 *   <li>{@link gl.model.Assessment} -- peer assessment</li>
 *   <li>{@link gl.model.AdmissibilityDecision} -- gate check result</li>
 * </ul>
 *
 * <h2>Request / Response</h2>
 * <ul>
 *   <li>{@link gl.model.ResourceRequest} -- generation request</li>
 *   <li>{@link gl.model.ResourceResult} -- generation result</li>
 *   <li>{@link gl.model.ResponseSchema} -- expected output schema</li>
 *   <li>{@link gl.model.ValidationResult} -- schema validation result</li>
 *   <li>{@link gl.model.ProviderOutput} -- raw LLM response</li>
 * </ul>
 *
 * <h2>Governance</h2>
 * <ul>
 *   <li>{@link gl.model.PolicyDecision} -- pre-generation policy check</li>
 *   <li>{@link gl.model.GovernanceContext} -- contextual metadata</li>
 * </ul>
 *
 * <h2>Storage and Audit</h2>
 * <ul>
 *   <li>{@link gl.model.Blob} -- content-addressed storage unit</li>
 *   <li>{@link gl.model.BlobType} -- blob type enum</li>
 *   <li>{@link gl.model.TraceRecord} -- full audit trail</li>
 * </ul>
 */
package gl.model;
