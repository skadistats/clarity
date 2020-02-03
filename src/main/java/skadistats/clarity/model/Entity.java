package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

public class Entity {

    private final EngineType engineType;
    private final int handle;
    private final DTClass dtClass;

    private boolean active;
    private EntityState state;

    public Entity(EngineType engineType, int handle, DTClass dtClass) {
        this.engineType = engineType;
        this.handle = handle;
        this.dtClass = dtClass;
    }

    public void bindState(boolean active, EntityState state) {
        this.active = active;
        this.state = state;

    }

    public int getIndex() {
        return engineType.indexForHandle(handle);
    }

    public int getSerial() {
        return engineType.serialForHandle(handle);
    }

    public int getHandle() {
        return handle;
    }

    public DTClass getDtClass() {
        return dtClass;
    }

    public EntityState getState() {
        return state;
    }

    public boolean isActive() {
        return active;
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
        for (String property : properties) {
            if (!hasProperty(property)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String property) {
        FieldPath fp = getDtClass().getFieldPathForName(property);
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
        String title = "idx: " + getIndex() + ", serial: " + getSerial() + ", class: " + getDtClass().getDtName();
        return getState().dump(title, getDtClass()::getNameForFieldPath);
    }

}