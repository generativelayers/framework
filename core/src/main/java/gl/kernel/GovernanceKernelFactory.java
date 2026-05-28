package gl.kernel;

/**
 * Factory for {@link GovernanceKernel} instances.
 *
 * <p>This factory is <b>abstract</b> — it only knows about the
 * {@link KernelPorts.GenerativeProvider} interface, never about
 * concrete providers (Gemini, OpenAI, etc.). Concrete provider
 * selection is the responsibility of the application layer
 * (via {@link gl.provider.ProviderResolver}).
 */
public final class GovernanceKernelFactory {
    private GovernanceKernelFactory() {}

    /**
     * Create a GovernanceKernel backed by the deterministic fake provider.
     * For reproducible tests and offline development.
     */
    public static GovernanceKernel deterministicInMemory() {
        return withProvider(new KernelDefaults.DeterministicFakeProvider());
    }

    /**
     * Create a GovernanceKernel backed by the given provider.
     * This is the primary entry point — the caller decides
     * which concrete provider to inject.
     */
    public static GovernanceKernel withProvider(KernelPorts.GenerativeProvider provider) {
        return new GovernanceKernel(
                provider,
                new KernelDefaults.KeyValueResponseValidator(),
                new KernelDefaults.SimpleGovernancePolicy(),
                new KernelDefaults.SimpleAdmissibilityChecker(),
                new InMemoryKernelStores.Blobs(),
                new InMemoryKernelStores.Candidates(),
                new InMemoryKernelStores.Assessments(),
                new InMemoryKernelStores.Results(),
                new InMemoryKernelStores.Traces(),
                new InMemoryKernelStores.Metrics()
        );
    }
}
