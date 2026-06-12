package gl;

import gl.model.*;

/**
 * Listener for {@link GovernanceKernel} lifecycle events.
 *
 * <p>All methods have default no-op implementations, so listeners
 * only need to override the events they care about.
 *
 * <p>Register via {@link GovernanceKernelFactory#withListener(KernelListener)}.
 *
 * <p>Example:
 * <pre>
 *   GovernanceKernelFactory.builder(provider)
 *       .withListener(new KernelListener() {
 *           @Override public void onCandidateAccepted(Candidate c) {
 *               logger.info("Candidate accepted: " + c.candidateId());
 *           }
 *       })
 *       .build();
 * </pre>
 */
public interface KernelListener {

    /** Called when the governance policy denies a request. */
    default void onPolicyDenied(ResourceRequest request, PolicyDecision decision) {}

    /** Called when the LLM provider throws an exception. */
    default void onProviderFailed(ResourceRequest request, Exception error) {}

    /** Called when output validation fails. */
    default void onValidationFailed(ResourceRequest request, ValidationResult result) {}

    /** Called when a candidate is created after validation. The candidate may be VALIDATED or INVALID. */
    default void onCandidateCreated(Candidate candidate) {}

    /** Called when an agent accepts a candidate. */
    default void onCandidateAccepted(Candidate candidate) {}

    /** Called when an agent rejects a candidate. */
    default void onCandidateRejected(Candidate candidate) {}

    /** Called before a retry attempt after validation failure or provider error. */
    default void onRetry(ResourceRequest request, int attempt, String reason) {}
}
