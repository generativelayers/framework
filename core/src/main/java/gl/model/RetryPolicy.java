package gl.model;

/**
 * Controls retry behaviour when generation or validation fails.
 *
 * <p>By default, no retries are attempted. Use {@link #withRetries(int)} to
 * enable automatic retry with error feedback appended to the prompt.
 *
 * @param maxAttempts          total attempts (1 = no retry). Must be >= 1.
 * @param includeErrorFeedback if true, append validation errors to the re-prompt
 */
public record RetryPolicy(int maxAttempts, boolean includeErrorFeedback) {

    public RetryPolicy {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
    }

    /** No retries -- fail on first invalid output or provider error. */
    public static RetryPolicy none() { return new RetryPolicy(1, false); }

    /** Retry up to {@code max} times, appending error context to each re-prompt. */
    public static RetryPolicy withRetries(int max) { return new RetryPolicy(max, true); }
}
