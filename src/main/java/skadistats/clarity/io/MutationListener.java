package skadistats.clarity.io;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.StateMutation;

/**
 * Optional listener that observes every {@link EntityState} construction and every mutation
 * applied to one. Intended for capturing replay traces in benchmarks and diagnostic tools —
 * not used by production parsing.
 *
 * <p>The listener receives raw {@code EntityState} references; identity and id allocation
 * are the listener's concern. Setup mutations (baseline build, entity create, entity recreate)
 * are reported separately from update mutations so consumers can split them.
 */
public interface MutationListener {

    /** A fresh empty state was constructed via {@code cls.getEmptyState()}. */
    void onBirthEmpty(EntityState newState, DTClass cls);

    /** A new state was constructed by copying an existing one (e.g. {@code baseline.copy()}). */
    void onBirthCopy(EntityState newState, EntityState srcState);

    /** A mutation applied during baseline construction or entity create / recreate. */
    void onSetupMutation(EntityState target, FieldPath fp, StateMutation mutation);

    /** A mutation applied during a regular entity update. */
    void onUpdateMutation(EntityState target, FieldPath fp, StateMutation mutation);

}
