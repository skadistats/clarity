package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

public interface EntityStateSupplier<F extends FieldPath> {

    int getIndex();
    DTClass<F> getDTClass();
    int getSerial();
    boolean isActive();
    int getHandle();
    EntityState<F> getState();

}
