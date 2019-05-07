package skadistats.clarity.model;

import skadistats.clarity.model.state.ClientFrame;
import skadistats.clarity.model.state.CloneableEntityState;

import java.util.function.Supplier;

public class Entity {

    private final int index;
    private Supplier<ClientFrame> clientFrameSupplier;

    public Entity(int index, Supplier<ClientFrame> clientFrameSupplier) {
        this.index = index;
        this.clientFrameSupplier = clientFrameSupplier;
    }

    public int getIndex() {
        return index;
    }

    public CloneableEntityState getState() {
        return isValid() ? clientFrameSupplier.get().getState(index) : null;
    }

    public DTClass getDtClass() {
        return isValid() ? clientFrameSupplier.get().getDtClass(index) : null;
    }

    public int getSerial() {
        return isValid() ? clientFrameSupplier.get().getSerial(index) : 0;
    }

    public boolean isActive() {
        return isValid() && clientFrameSupplier.get().isActive(index);
    }

    public int getHandle() {
        // TODO: maybe return empty handle?
        return isValid() ? clientFrameSupplier.get().getHandle(index) : 0;
    }

    public boolean isValid() {
        ClientFrame f = this.clientFrameSupplier.get();
        return f != null && f.isValid(index);
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
        return (T) getDtClass().getValueForFieldPath(fp, getState());
    }

    @Override
    public String toString() {
        String title = "idx: " + index + ", serial: " + getSerial() + ", class: " + getDtClass().getDtName();
        return getDtClass().dumpState(title, getState());
    }

}