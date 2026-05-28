package generativelayers.kernel;

/**
 * Factory for {@link Kernel} instances.
 *
 * <p>This factory is <b>abstract</b> — it only knows about the
 * {@link KernelPorts.GenerativeProvider} interface, never about
 * concrete providers (Gemini, OpenAI, etc.). Concrete provider
 * selection is the responsibility of the application layer
 * (via {@link generativelayers.provider.ProviderResolver}).
 */
public final class KernelFactory {
    private KernelFactory() {}

    /**
     * Create a kernel backed by the deterministic fake provider.
     * For reproducible tests and offline development.
     */
    public static Kernel deterministicInMemory() {
        return withProvider(new KernelDefaults.DeterministicFakeProvider());
    }

    /**
     * Create a kernel backed by the given provider.
     * This is the primary entry point — the caller decides
     * which concrete provider to inject.
     */
    public static Kernel withProvider(KernelPorts.GenerativeProvider provider) {
        return new Kernel(
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
