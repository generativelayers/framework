package gl.body;

import gl.model.*;

/** Status of a body invocation: CREATED, CANDIDATE_READY, DENIED,
 *  ESCALATED, PROVIDER_ERROR, or INVALID_OUTPUT. */
public enum InvocationStatus {
    CREATED,
    DENIED,
    ESCALATED,
    PROVIDER_ERROR,
    INVALID_OUTPUT,
    CANDIDATE_READY
}
