package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

public interface EntityStateSupplier<E extends EntityState> {

    int getIndex();
    DTClass getDTClass();
    int getSerial();
    boolean isActive();
    int getHandle();
    E getState();

}
