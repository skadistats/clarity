package skadistats.clarity.model.state;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.FieldPath;

import java.util.Set;

public class ClientFrame {

    private final EngineType engineType;
    private final int tick;
    private final boolean[] valid;
    private final DTClass[] dtClass;
    private final int[] serial;
    private final boolean[] active;
    private final int[] lastChangedTick;
    private final Set[] changedFieldPaths;
    private final CloneableEntityState[] state;

    public ClientFrame(EngineType engineType, int tick) {
        this.engineType = engineType;
        this.tick = tick;
        int n = 1 << engineType.getIndexBits();
        this.valid = new boolean[n];
        this.dtClass = new DTClass[n];
        this.serial = new int[n];
        this.active = new boolean[n];
        this.lastChangedTick = new int[n];
        this.changedFieldPaths = new Set[n];
        this.state = new CloneableEntityState[n];
    }

    public void copyFromOtherFrame(ClientFrame otherFrame, int idx, int length) {
        System.arraycopy(otherFrame.valid, idx, valid, idx, length);
        System.arraycopy(otherFrame.dtClass, idx, dtClass, idx, length);
        System.arraycopy(otherFrame.serial, idx, serial, idx, length);
        System.arraycopy(otherFrame.active, idx, active, idx, length);
        System.arraycopy(otherFrame.lastChangedTick, idx, lastChangedTick, idx, length);
        // no changed field paths (leave at null)
        System.arraycopy(otherFrame.state, idx, state, idx, length);
    }

    public void createNewEntity(int eIdx, DTClass dtClass, int serial, Set<FieldPath> changedFieldPaths, CloneableEntityState state) {
        this.valid[eIdx] = true;
        this.dtClass[eIdx] = dtClass;
        this.serial[eIdx] = serial;
        this.active[eIdx] = true;
        this.lastChangedTick[eIdx] = tick;
        this.changedFieldPaths[eIdx] = changedFieldPaths;
        this.state[eIdx] = state;
    }

    public void updateExistingEntity(ClientFrame oldFrame, int eIdx, Set<FieldPath> changedFieldPaths, CloneableEntityState state) {
        this.valid[eIdx] = oldFrame.valid[eIdx];
        this.dtClass[eIdx] = oldFrame.dtClass[eIdx];
        this.serial[eIdx] = oldFrame.serial[eIdx];
        this.active[eIdx] = oldFrame.active[eIdx];
        this.lastChangedTick[eIdx] = tick;
        this.changedFieldPaths[eIdx] = changedFieldPaths;
        this.state[eIdx] = state;
    }

    public void deleteEntity(int eIdx) {
        this.valid[eIdx] = false;
        this.dtClass[eIdx] = null;
        this.serial[eIdx] = 0;
        this.active[eIdx] = false;
        this.lastChangedTick[eIdx] = tick;
        this.changedFieldPaths[eIdx] = null;
        this.state[eIdx] = null;
    }

    public void setActive(int eIdx, boolean active) {
        this.active[eIdx] = active;
    }

    public void setChangedFieldPaths(int idx, Set<FieldPath> changedFieldPaths) {
        this.changedFieldPaths[idx] = changedFieldPaths;
    }

    public int getTick() {
        return tick;
    }

    public int getSize() {
        return state.length;
    }

    public boolean isValid(int idx) {
        return valid[idx];
    }

    public DTClass getDtClass(int idx) {
        return dtClass[idx];
    }

    public int getSerial(int idx) {
        return serial[idx];
    }

    public boolean isActive(int idx) {
        return active[idx];
    }

    public int getLastChangedTick(int idx) {
        return lastChangedTick[idx];
    }

    public Set<FieldPath> getChangedFieldPaths(int idx) {
        return changedFieldPaths[idx];
    }

    public CloneableEntityState getState(int idx) {
        return state[idx];
    }

    public int getHandle(int idx) {
        return engineType.handleForIndexAndSerial(idx, getSerial(idx));
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ClientFrame{");
        sb.append("tick=").append(tick);
        sb.append('}');
        return sb.toString();
    }

}
