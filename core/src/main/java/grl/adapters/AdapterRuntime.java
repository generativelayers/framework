package grl.adapters;

import grl.body.GenerativeBodyRegistry;
import grl.body.GenerativeBodyRuntime;
import grl.kernel.Kernel;

public final class AdapterRuntime {
    private AdapterRuntime() {}
    public static Kernel kernel() { return GenerativeBodyRuntime.kernel(); }
    public static GenerativeBodyRegistry bodies() { return GenerativeBodyRuntime.registry(); }
}
