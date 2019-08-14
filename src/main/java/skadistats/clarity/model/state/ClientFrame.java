package skadistats.clarity.model.state;

import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.Set;

public class ClientFrame {

    private final int tick;
    private final boolean[] active;
    private final Entity[] entity;
    private final Set[] changedFieldPaths;
    private final EntityState[] state;

    public ClientFrame(int tick, int size) {
        this.tick = tick;
        this.active = new boolean[size];
        this.entity = new Entity[size];
        this.changedFieldPaths = new Set[size];
        this.state = new EntityState[size];
    }

    public void copyFromOtherFrame(ClientFrame otherFrame, int idx, int length) {
        System.arraycopy(otherFrame.active, idx, active, idx, length);
        System.arraycopy(otherFrame.entity, idx, entity, idx, length);
        // no changed field paths (leave at null)
        System.arraycopy(otherFrame.state, idx, state, idx, length);
    }

    public void createNewEntity(Entity entity, Set<FieldPath> changedFieldPaths, EntityState state) {
        int eIdx = entity.getIndex();
        this.active[eIdx] = true;
        this.entity[eIdx] = entity;
        this.changedFieldPaths[eIdx] = changedFieldPaths;
        this.state[eIdx] = state;
    }

    public void updateExistingEntity(ClientFrame oldFrame, int eIdx, Set<FieldPath> changedFieldPaths, EntityState state) {
        this.active[eIdx] = oldFrame.active[eIdx];
        this.entity[eIdx] = oldFrame.entity[eIdx];
        this.changedFieldPaths[eIdx] = changedFieldPaths;
        this.state[eIdx] = state;
    }

    public void deleteEntity(int eIdx) {
        this.active[eIdx] = false;
        this.entity[eIdx] = null;
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
        return entity[idx] != null;
    }

    public boolean isActive(int idx) {
        return active[idx];
    }

    public Entity getEntity(int eIdx) {
        return entity[eIdx];
    }

    public Set<FieldPath> getChangedFieldPaths(int idx) {
        return changedFieldPaths[idx];
    }

    public EntityState getState(int idx) {
        return state[idx];
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ClientFrame{");
        sb.append("tick=").append(tick);
        sb.append('}');
        return sb.toString();
    }

}
