package skadistats.clarity.io.decoder;

import java.util.HashMap;
import java.util.Map;

public abstract class Decoder {

    private static final Map<Class<?>, Integer> IDS = new HashMap<>();

    static {
        try {
            Class.forName("skadistats.clarity.io.decoder.DecoderIds");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("DecoderIds not found — annotation processing did not run", e);
        }
    }

    static void register(Class<?> clazz, int id) {
        IDS.put(clazz, id);
    }

    protected final int id;

    protected Decoder() {
        var assignedId = IDS.get(getClass());
        if (assignedId == null) {
            throw new IllegalStateException("No decoder ID registered for " + getClass().getName());
        }
        this.id = assignedId;
    }
}
