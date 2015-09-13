package skadistats.clarity.model;

public class Entity {

    private final EngineType engineType;
    private final int index;
    private final int serial;
    private final DTClass dtClass;
    private boolean active;
    private final Object[] state;

    public Entity(EngineType engineType, int index, int serial, DTClass dtClass, boolean active, Object[] state) {
        this.engineType = engineType;
        this.index = index;
        this.serial = serial;
        this.dtClass = dtClass;
        this.active = active;
        this.state = state;
    }

    public int getIndex() {
        return index;
    }

    public int getSerial() {
        return serial;
    }
    
    public int getHandle() {
        return engineType.handleForIndexAndSerial(index, serial);
    }

    public DTClass getDtClass() {
        return dtClass;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
        return dtClass.getFieldPathForName(property) != null;
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
        FieldPath fp = dtClass.getFieldPathForName(property);
        if (fp == null) {
            throw new IllegalArgumentException(String.format("property %s not found on entity of class %s", property, getDtClass().getDtName()));
        }
        return getPropertyForFieldPath(fp);
    }

    public <T> T getPropertyForFieldPath(FieldPath fp) {
        return (T) dtClass.getValueForFieldPath(fp, state);
    }

    @Override
    public String toString() {
        String title = "idx: " + index + ", serial: " + serial + ", class: " + dtClass.getDtName();
        return dtClass.dumpState(title, state);
    }

}
