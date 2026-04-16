package skadistats.clarity.bench.trace;

import skadistats.clarity.io.MutationListener;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.StateMutation;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Captures every {@link EntityState} construction and mutation observed during a parser run.
 * Owns the {@link EntityState}-to-stateId mapping (identity-based) so the parser doesn't need
 * bench-only fields.
 *
 * <p>Setup mutations are folded into the {@link BirthRecipe} of the most recently born state
 * (parser flow always emits setup mutations in a tight burst right after their birth).
 * Update mutations are appended to a flat list — the measured workload.
 */
public class MutationRecorder implements MutationListener {

    private final IdentityHashMap<EntityState, Integer> stateIds = new IdentityHashMap<>();
    private int nextStateId = 0;

    private final List<BirthRecipe> births = new ArrayList<>();
    private final List<Mutation> updateMutations = new ArrayList<>();

    /** Setup mutations for the most recent birth, accumulated until the next birth (or finish). */
    private List<Mutation> openSetup = new ArrayList<>();
    private int openBirthIdx = -1;

    @Override
    public void onBirthEmpty(EntityState newState, DTClass cls) {
        closeOpenBirth();
        var id = nextStateId++;
        stateIds.put(newState, id);
        openBirthIdx = births.size();
        births.add(new BirthRecipe(id, BirthKind.EMPTY, cls, -1, null));
    }

    @Override
    public void onBirthCopy(EntityState newState, EntityState srcState) {
        closeOpenBirth();
        var id = nextStateId++;
        stateIds.put(newState, id);
        var srcId = stateIds.get(srcState);
        if (srcId == null) {
            throw new IllegalStateException("source state for copy was never observed as born");
        }
        openBirthIdx = births.size();
        births.add(new BirthRecipe(id, BirthKind.COPY_OF, null, srcId, null));
    }

    @Override
    public void onSetupMutation(EntityState target, FieldPath fp, StateMutation mutation) {
        openSetup.add(new Mutation(stateIdOf(target), fp, mutation));
    }

    @Override
    public void onUpdateMutation(EntityState target, FieldPath fp, StateMutation mutation) {
        updateMutations.add(new Mutation(stateIdOf(target), fp, mutation));
    }

    private int stateIdOf(EntityState state) {
        var id = stateIds.get(state);
        if (id == null) {
            throw new IllegalStateException("mutation target was never observed as born");
        }
        return id;
    }

    private void closeOpenBirth() {
        if (openBirthIdx < 0) return;
        var b = births.get(openBirthIdx);
        var setup = openSetup.isEmpty()
                ? BirthRecipe.NO_SETUP
                : openSetup.toArray(new Mutation[0]);
        births.set(openBirthIdx, new BirthRecipe(b.stateId(), b.kind(), b.cls(), b.srcStateId(), setup));
        openSetup.clear();
        openBirthIdx = -1;
    }

    /**
     * Finalize the trace. Closes any in-progress birth, drops the identity map (its contents
     * are no longer needed for replay and they keep many large {@link EntityState} objects
     * alive), and returns the captured trace.
     */
    public CapturedTrace finish(int pointerCount) {
        closeOpenBirth();
        var trace = new CapturedTrace(
                List.copyOf(births),
                updateMutations.toArray(new Mutation[0]),
                pointerCount
        );
        stateIds.clear();
        births.clear();
        updateMutations.clear();
        openSetup.clear();
        return trace;
    }
}
