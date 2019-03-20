package skadistats.clarity.model.state;

import skadistats.clarity.decoder.s2.Serializer;

public interface EntityState {

    int length();

    boolean has(int idx);

    Object get(int idx);
    void set(int idx, Object value);

    EntityState createSub(int idx, int length);

    EntityState clone();

    default EntityState sub(int idx) {
        return (EntityState) get(idx);
    }

    default EntityState capacity(int i, int wantedSize, boolean shrinkIfNeeded, Serializer subSerializer) {
        EntityState subState = sub(i);
        if (wantedSize < 0) {
            // TODO: sometimes negative - figure out what this means
            return subState;
        }
        int growth = 0;
        int curSize = subState == null ? 0 : subState.length();
        if (subState == null && wantedSize > 0) {
            createSub(i, wantedSize);
            growth = wantedSize;
        } else if (shrinkIfNeeded && wantedSize == 0 && curSize != 0) {
            createSub(i, -1);
        } else if (wantedSize != curSize) {
            if (shrinkIfNeeded || wantedSize > curSize) {
                EntityState subStateNew = createSub(i, wantedSize);
                curSize = wantedSize;
                int n = Math.min(subState.length(), curSize);
                for (int j = 0; j < n; j++) {
                    subStateNew.set(j, subState.get(j));
                }
            }
            growth = Math.max(0, curSize - subState.length());
        }
        if (growth > 0 && subSerializer != null) {
            EntityState subStateNew = sub(i);
            int j = subStateNew.length();
            while (growth-- > 0) {
                subStateNew.set(--j, subSerializer.getInitialState());
            }
        }
        return sub(i);
    }

}
