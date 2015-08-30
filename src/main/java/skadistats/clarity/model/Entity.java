package skadistats.clarity.model;

import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.util.TextTable;

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

    /**
     * Check if this entity contains the given property.
     *
     * @param property Name of the property
     * @return True, if and only if the given property is present in this entity
     */
    public boolean hasProperty(String property) {
        return dtClass.getPropertyIndex(property) != null;
    }

    /**
     * Check if this entity contains all of the given properties.
     *
     * @param properties Names of the properties
     * @return True, if and only if the given properties are present in this entity
     */
    public boolean hasProperties(String... properties) {
        for (String property : properties) {
            if (!hasProperty(property)) {
                return false;
            }
        }
        return true;
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
        TextTable t = new TextTable.Builder()
            .setTitle("idx: " + index + ", serial: " + serial + ", class: " + dtClass.getDtName())
            .setFrame(TextTable.FRAME_COMPAT)
            .addColumn("Index", TextTable.Alignment.RIGHT)
            .addColumn("Property")
            .addColumn("Value")
            .build();
        for (int i = 0; i < state.length; i++) {
            t.setData(i, 0, i);
            if (dtClass instanceof S1DTClass) {
                t.setData(i, 1, ((S1DTClass)dtClass).getReceiveProps()[i].getVarName());
            } else {
                t.setData(i, 1, "TODO");
            }
            t.setData(i, 2, state[i]);
        }
        return t.toString();
    }

}
