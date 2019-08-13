package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

public class Entity {

    private final EntityStateSupplier stateSupplier;

    public Entity(EntityStateSupplier stateSupplier) {
        this.stateSupplier = stateSupplier;
    }

    public int getIndex() {
        return stateSupplier.getIndex();
    }

    public EntityState getState() {
        return stateSupplier.getState();
    }

    public DTClass getDtClass() {
        return stateSupplier.getDTClass();
    }

    public int getSerial() {
        return stateSupplier.getSerial();
    }

    public boolean isActive() {
        return stateSupplier.isActive();
    }

    public int getHandle() {
        return stateSupplier.getHandle();
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