package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

public interface EntityStateSupplier {

    int getIndex();
    DTClass getDTClass();
    int getSerial();
    boolean isActive();
    int getHandle();
    EntityState getState();

}
