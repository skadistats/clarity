package clarity.model;

import java.util.Map;

public class Entity {

    private final Integer index;
    private final Integer serial;
    private final DTClass dtClass;
    private final Map<Integer, Object> state;

    public Entity(Integer index, Integer serial, DTClass cls, Map<Integer, Object> state) {
        this.index = index;
        this.serial = serial;
        this.dtClass = cls;
        this.state = state;
    }

    public void updateFrom(Entity from) {
        for (Map.Entry<Integer, Object> e : from.state.entrySet()) {
            state.put(e.getKey(), e.getValue());
        }
    }

    public Integer getIndex() {
        return index;
    }

    public Integer getSerial() {
        return serial;
    }

    public DTClass getDtClass() {
        return dtClass;
    }

}
