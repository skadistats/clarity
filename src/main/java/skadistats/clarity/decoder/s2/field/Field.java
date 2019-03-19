package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.DumpEntry;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.EntityStateFactory;

import java.util.List;

public abstract class Field {

    protected final FieldProperties properties;

    public Field(FieldProperties properties) {
        this.properties = properties;
    }

    public abstract Object getInitialState();
    public abstract void accumulateName(FieldPath fp, int pos, List<String> parts);
    public abstract Unpacker getUnpackerForFieldPath(FieldPath fp, int pos);
    public abstract Field getFieldForFieldPath(FieldPath fp, int pos);
    public abstract FieldType getTypeForFieldPath(FieldPath fp, int pos);
    public abstract Object getValueForFieldPath(FieldPath fp, int pos, EntityState state);
    public abstract void setValueForFieldPath(FieldPath fp, int pos, EntityState state, Object data);
    public abstract FieldPath getFieldPathForName(FieldPath fp, String property);
    public abstract void collectDump(FieldPath fp, String namePrefix, List<DumpEntry> entries, EntityState state);
    public abstract void collectFieldPaths(FieldPath fp, List<FieldPath> entries, EntityState state);

    protected void addBasePropertyName(List<String> parts) {
        parts.add(properties.getName());
    }

    protected String joinPropertyName(String... parts) {
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            if (b.length() != 0) {
                b.append('.');
            }
            b.append(part);
        }
        return b.toString();
    }

    public FieldProperties getProperties() {
        return properties;
    }

    protected EntityState ensureSubStateCapacity(EntityState state, int i, int wantedSize, boolean shrinkIfNeeded) {
        EntityState subState = state.sub(i);
        if (wantedSize < 0) {
            // TODO: sometimes negative - figure out what this means
            return subState;
        }
        int growth = 0;
        int curSize = subState == null ? 0 : subState.length();
        if (subState == null && wantedSize > 0) {
            state.set(i, EntityStateFactory.withLength(wantedSize));
            growth = wantedSize;
        } else if (shrinkIfNeeded && wantedSize == 0) {
            state.set(i, null);
        } else if (wantedSize != curSize) {
            if (shrinkIfNeeded || wantedSize > curSize) {
                state.set(i, EntityStateFactory.withLength(wantedSize));
                curSize = wantedSize;
            }
            int n = Math.min(subState.length(), curSize);
            EntityState subStateNew = state.sub(i);
            for (int j = 0; j < n; j++) {
                subStateNew.set(j, subState.get(j));
            }
            growth = Math.max(0, curSize - subState.length());
        }
        if (growth > 0 && properties.getSerializer() != null) {
            EntityState subStateNew = state.sub(i);
            int j = subStateNew.length();
            while (growth-- > 0) {
                subStateNew.set(--j, properties.getSerializer().getInitialState());
            }
        }
        return state.sub(i);
    }

}
