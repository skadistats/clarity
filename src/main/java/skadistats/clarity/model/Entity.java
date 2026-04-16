package skadistats.clarity.model;

import skadistats.clarity.io.s1.S1DTClass;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.S2AbstractEntityState;

public class Entity {

    private final int index;
    private final int serial;
    private final int handle;
    private final DTClass dtClass;

    private boolean existent;
    private boolean active;
    private int spawnGroupHandle;
    private EntityState state;

    public Entity(int index, int serial, int handle, DTClass dtClass) {
        this.index = index;
        this.serial = serial;
        this.handle = handle;
        this.dtClass = dtClass;
    }

    public int getIndex() {
        return index;
    }

    public int getSerial() {
        return serial;
    }

    public int getHandle() {
        return handle;
    }

    public DTClass getDtClass() {
        return dtClass;
    }

    public boolean isExistent() {
        return existent;
    }

    public void setExistent(boolean existent) {
        this.existent = existent;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Get the spawn group handle this entity belongs to. Source 2 only;
     * always 0 for Source 1 engines.
     *
     * <p>Spawn groups are the Source 2 mechanism for batch-loading and
     * unloading entities. Handle 0 is the system spawn group containing
     * always-resident resource entities (CWorld, player controllers, team,
     * game rules, observer pawns). Higher handles correspond to map sections
     * loaded later (main map, sub-areas, mod content).
     */
    public int getSpawnGroupHandle() {
        return spawnGroupHandle;
    }

    public void setSpawnGroupHandle(int spawnGroupHandle) {
        this.spawnGroupHandle = spawnGroupHandle;
    }

    public EntityState getState() {
        return state;
    }

    public void setState(EntityState state) {
        this.state = state;
    }

    public String getNameForFieldPath(FieldPath fp) {
        if (state instanceof S2AbstractEntityState s2s) {
            return s2s.getNameForFieldPath(fp);
        }
        return ((S1DTClass) dtClass).getNameForFieldPath(fp);
    }

    public FieldPath getFieldPathForName(String property) {
        if (state instanceof S2AbstractEntityState s2s) {
            return s2s.getFieldPathForName(property);
        }
        return ((S1DTClass) dtClass).getFieldPathForName(property);
    }

    public Field getFieldForFieldPath(FieldPath fp) {
        return ((S2AbstractEntityState) state).getFieldForFieldPath((S2FieldPath) fp);
    }

    /**
     * Check if this entity contains the given property.
     *
     * @param property Name of the property
     * @return True, if and only if the given property is present in this entity
     */
    public boolean hasProperty(String property) {
        return getFieldPathForName(property) != null;
    }

    /**
     * Check if this entity contains all of the given properties.
     *
     * @param properties Names of the properties
     * @return True, if and only if the given properties are present in this entity
     */
    public boolean hasProperties(String... properties) {
        for (var property : properties) {
            if (!hasProperty(property)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String property) {
        var fp = getFieldPathForName(property);
        if (fp == null) {
            throw new IllegalArgumentException(String.format("property %s not found on entity of class %s", property, getDtClass().getDtName()));
        }
        return getPropertyForFieldPath(fp);
    }

    public <T> T getPropertyForFieldPath(FieldPath fp) {
        return (T) getState().getValueForFieldPath(fp);
    }

    @Override
    public String toString() {
        var title = "idx: " + getIndex() + ", serial: " + getSerial() + ", class: " + getDtClass().getDtName();
        return getState().dump(title, this::getNameForFieldPath);
    }

    public long getUid() {
        return uid(dtClass.getClassId(), handle);
    }

    public static long uid(int dtClassId, int handle) {
        return (long) dtClassId << 32 | handle;
    }

}
