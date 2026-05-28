package generativelayers.adapters;

import generativelayers.body.GenerativeBodyRegistry;
import generativelayers.body.GenerativeBodyRuntime;
import generativelayers.kernel.Kernel;

public final class AdapterRuntime {
    private AdapterRuntime() {}
    public static Kernel kernel() { return GenerativeBodyRuntime.kernel(); }
    public static GenerativeBodyRegistry bodies() { return GenerativeBodyRuntime.registry(); }
}
