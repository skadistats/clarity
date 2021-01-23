package skadistats.clarity.model.state;

import skadistats.clarity.model.Entity;

public class ClientFrame {

    private final Entity[] entity;

    public ClientFrame(int size) {
        this.entity = new Entity[size];
    }

    public void setEntity(Entity e) {
        int eIdx = e.getIndex();
        this.entity[eIdx] = e;
    }

    public void removeEntity(Entity e) {
        int eIdx = e.getIndex();
        this.entity[eIdx] = null;
    }

    public Entity getEntity(int eIdx) {
        return entity[eIdx];
    }

    public int getSize() {
        return entity.length;
    }

    public Capsule createCapsule() {
        return new Capsule(this);
    }

    public static class Capsule {

        private final int[] handle;
        private final boolean[] active;
        private EntityState[] state;

        private Capsule(ClientFrame frame) {
            int size = frame.getSize();
            handle = new int[size];
            active = new boolean[size];
            state = new EntityState[size];
            for (int i = 0; i < size; i++) {
                Entity e = frame.entity[i];
                handle[i] = e != null ? e.getHandle() : -1;
                active[i] = e != null && e.isActive();
                state[i] = e != null ? e.getState().copy() : null;
            }
        }

        public boolean isExistent(int eIdx) {
            return handle[eIdx] != -1;
        }

        public boolean isActive(int eIdx) {
            return active[eIdx];
        }

        public int getHandle(int eIdx) {
            return handle[eIdx];
        }

        public EntityState getState(int eIdx) {
            return state[eIdx];
        }

    }

}
