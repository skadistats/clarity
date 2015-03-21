package skadistats.clarity.model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class Entity {

    private final int index;
    private final int serial;
    private final DTClass dtClass;
    private PVS pvs;
    private final Object[] state;

    public Entity(int index, int serial, DTClass dtClass, PVS pvs, Object[] state) {
        this.index = index;
        this.serial = serial;
        this.dtClass = dtClass;
        this.pvs = pvs;
        this.state = state;
    }

    public int getIndex() {
        return index;
    }

    public int getSerial() {
        return serial;
    }
    
    public int getHandle() {
        return Handle.forIndexAndSerial(index, serial);
    }

    public DTClass getDtClass() {
        return dtClass;
    }

    public PVS getPvs() {
        return pvs;
    }

    public void setPvs(PVS pvs) {
        this.pvs = pvs;
    }

    public Object[] getState() {
        return state;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String property) {
        Integer index = dtClass.getPropertyIndex(property);
        if (index == null) {
            throw new IllegalArgumentException(String.format("property %s not found on entity of class %s", property, getDtClass().getDtName()));
        }
        return (T) state[index.intValue()];
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getArrayProperty(Class<T> clazz, String property) {
        List<T> result = new ArrayList<T>();
        int i = 0;
        while (true) {
            Integer idx = dtClass.getPropertyIndex(property + String.format(".%04d", i));
            if (idx == null) {
                break;
            }
            result.add((T) state[idx.intValue()]);
            i++;
        }
        return (T[]) result.toArray((T[]) Array.newInstance(clazz, 0));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n\n-- Entity [index=");
        builder.append(index);
        builder.append(", serial=");
        builder.append(serial);
        builder.append(", dtClass=");
        builder.append(dtClass.getDtName());
        builder.append("]");
        for (int i = 0; i < state.length; i++) {
            builder.append("\n");
            builder.append(dtClass.getReceiveProps()[i].getVarName());
            builder.append(" = ");
            builder.append(state[i]);
        }

        return builder.toString();
    }

}
