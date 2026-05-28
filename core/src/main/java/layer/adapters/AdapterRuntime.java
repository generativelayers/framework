package layer.adapters;

import layer.body.GenerativeBodyRegistry;
import layer.body.GenerativeBodyRuntime;
import layer.kernel.Kernel;

public final class AdapterRuntime {
    private AdapterRuntime() {}
    public static Kernel kernel() { return GenerativeBodyRuntime.kernel(); }
    public static GenerativeBodyRegistry bodies() { return GenerativeBodyRuntime.registry(); }
}
