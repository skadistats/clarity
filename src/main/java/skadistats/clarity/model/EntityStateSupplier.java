package skadistats.clarity.model;

import skadistats.clarity.model.state.CloneableEntityState;

public interface EntityStateSupplier {

    int getIndex();
    DTClass getDTClass();
    int getSerial();
    boolean isActive();
    int getHandle();
    CloneableEntityState getState();

}
