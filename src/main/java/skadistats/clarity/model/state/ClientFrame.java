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

        private final long[] uid;
        private final boolean[] active;
        private EntityState[] state;

        private Capsule(ClientFrame frame) {
            int size = frame.getSize();
            uid = new long[size];
            active = new boolean[size];
            state = new EntityState[size];
            for (int i = 0; i < size; i++) {
                Entity e = frame.entity[i];
                uid[i] = e != null ? e.getUid() : -1L;
                active[i] = e != null && e.isActive();
                state[i] = e != null ? e.getState().copy() : null;
            }
        }

        public boolean isExistent(int eIdx) {
            return uid[eIdx] != -1;
        }

        public boolean isActive(int eIdx) {
            return active[eIdx];
        }

        public long getUid(int eIdx) {
            return uid[eIdx];
        }

        public EntityState getState(int eIdx) {
            return state[eIdx];
        }

    }

}
