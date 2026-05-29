package gl;

import gl.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory and builder for {@link GovernanceKernel} instances.
 *
 * <p>Quick usage:
 * <pre>
 *   // Deterministic (tests)
 *   GovernanceKernel kernel = GovernanceKernelFactory.deterministicInMemory();
 *
 *   // With a real provider
 *   GovernanceKernel kernel = GovernanceKernelFactory.withProvider(myProvider);
 *
 *   // With retry, listeners, custom policy
 *   GovernanceKernel kernel = GovernanceKernelFactory.builder(myProvider)
 *       .withRetryPolicy(RetryPolicy.withRetries(3))
 *       .withListener(myListener)
 *       .withPolicy(myPolicy)
 *       .build();
 * </pre>
 */
public final class GovernanceKernelFactory {
    private GovernanceKernelFactory() {}

    /** Create a kernel backed by the deterministic fake provider. */
    public static GovernanceKernel deterministicInMemory() {
        return withProvider(new KernelDefaults.DeterministicFakeProvider());
    }

    /** Create a kernel backed by the given provider with all defaults. */
    public static GovernanceKernel withProvider(KernelPorts.GenerativeProvider provider) {
        return builder(provider).build();
    }

    /** Create a builder for fine-grained kernel configuration. */
    public static Builder builder(KernelPorts.GenerativeProvider provider) {
        return new Builder(provider);
    }

    /** Fluent builder for configuring a {@link GovernanceKernel}. */
    public static final class Builder {
        private final KernelPorts.GenerativeProvider provider;
        private KernelPorts.ResponseValidator validator = new KernelDefaults.KeyValueResponseValidator();
        private KernelPorts.GovernancePolicy policy = new KernelDefaults.SimpleGovernancePolicy();
        private KernelPorts.AdmissibilityChecker admissibility = new KernelDefaults.SimpleAdmissibilityChecker();
        private RetryPolicy retryPolicy = RetryPolicy.none();
        private final List<KernelListener> listeners = new ArrayList<>();

        private Builder(KernelPorts.GenerativeProvider provider) {
            this.provider = provider;
        }

        public Builder withValidator(KernelPorts.ResponseValidator v) { this.validator = v; return this; }
        public Builder withPolicy(KernelPorts.GovernancePolicy p) { this.policy = p; return this; }
        public Builder withAdmissibility(KernelPorts.AdmissibilityChecker a) { this.admissibility = a; return this; }
        public Builder withRetryPolicy(RetryPolicy rp) { this.retryPolicy = rp; return this; }
        public Builder withListener(KernelListener l) { this.listeners.add(l); return this; }

        public GovernanceKernel build() {
            return new GovernanceKernel(
                    provider, validator, policy, admissibility,
                    new InMemoryKernelStores.Blobs(),
                    new InMemoryKernelStores.Candidates(),
                    new InMemoryKernelStores.Assessments(),
                    new InMemoryKernelStores.Results(),
                    new InMemoryKernelStores.Traces(),
                    new InMemoryKernelStores.Metrics(),
                    retryPolicy, List.copyOf(listeners)
            );
        }
    }
}
