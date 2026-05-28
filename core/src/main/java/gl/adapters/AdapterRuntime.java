package gl.adapters;

import gl.body.GenerativeBodyRegistry;
import gl.body.GenerativeBodyRuntime;
import gl.GovernanceKernel.GovernanceKernel;

public final class AdapterRuntime {
    private AdapterRuntime() {}
    public static GovernanceKernel GovernanceKernel() { return GenerativeBodyRuntime.GovernanceKernel(); }
    public static GenerativeBodyRegistry bodies() { return GenerativeBodyRuntime.registry(); }
}
