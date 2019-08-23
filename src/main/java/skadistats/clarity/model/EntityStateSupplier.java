package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

public interface EntityStateSupplier {

    boolean isActive();
    EntityState getState();

}
