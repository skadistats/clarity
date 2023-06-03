package skadistats.clarity.util;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public abstract class StateDifferenceEvaluator {

    private final EntityState prevState;
    private final EntityState curState;
    private final Iterator<FieldPath> prevIter;
    private final Iterator<FieldPath> curIter;
    private FieldPath prev = null;
    private FieldPath cur = null;

    public StateDifferenceEvaluator(EntityState prevState, EntityState curState) {
        this.prevState = prevState;
        this.curState = curState;
        this.prevIter = prevState.fieldPathIterator();
        this.curIter = curState.fieldPathIterator();
    }

    private void advancePrev() {
        prev = prevIter.hasNext() ? prevIter.next() : null;
    }

    private void advanceCur() {
        cur = curIter.hasNext() ? curIter.next() : null;
    }

    private boolean prevHigher() {
        return cur != null && (prev == null || prev.compareTo(cur) > 0);
    }

    private boolean curHigher() {
        return prev != null && (cur == null || cur.compareTo(prev) > 0);
    }

    protected abstract void onPropertiesDeleted(List<FieldPath> fieldPaths);

    protected abstract void onPropertiesAdded(List<FieldPath> fieldPaths);

    protected abstract void onPropertyChanged(FieldPath fieldPath);

    public void work() {
        advancePrev();
        advanceCur();
        while (true) {
            if (Objects.equals(prev, cur)) {
                if (prev == null) break;
                if (!Objects.equals(
                        prevState.getValueForFieldPath(prev),
                        curState.getValueForFieldPath(prev)
                )) onPropertyChanged(prev);
                advancePrev();
                advanceCur();
            } else if (prevHigher()) {
                List<FieldPath> akku = new ArrayList<>();
                do {
                    akku.add(cur);
                    advanceCur();
                } while (prevHigher());
                onPropertiesAdded(akku);
            } else if (curHigher()) {
                List<FieldPath> akku = new ArrayList<>();
                do {
                    akku.add(prev);
                    advancePrev();
                } while (curHigher());
                onPropertiesDeleted(akku);
            } else {
                throw new UnsupportedOperationException("should not happen");
            }
        }
    }

}
