package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

public class Entity {

    private final int index;
    private final int serial;
    private final int handle;
    private final DTClass dtClass;

    private boolean existent;
    private boolean active;
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

    public EntityState getState() {
        return state;
    }

    public void setState(EntityState state) {
        this.state = state;
    }

    /**
     * Check if this entity contains the given property.
     *
     * @param property Name of the property
     * @return True, if and only if the given property is present in this entity
     */
    public boolean hasProperty(String property) {
        return getDtClass().getFieldPathForName(property) != null;
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
        var fp = getDtClass().getFieldPathForName(property);
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
        return getState().dump(title, getDtClass()::getNameForFieldPath);
    }

    public long getUid() {
        return uid(dtClass.getClassId(), handle);
    }

    public static long uid(int dtClassId, int handle) {
        return (long) dtClassId << 32 | handle;
    }

}
