package skadistats.clarity.model.state;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;

public abstract sealed class S2EntityState implements EntityState
        permits S2FlatEntityState, S2NestedArrayEntityState, S2TreeMapEntityState {

    protected final SerializerField rootField;
    protected Serializer[] pointerSerializers;

    protected S2EntityState(SerializerField rootField, int pointerCount) {
        this.rootField = rootField;
        this.pointerSerializers = new Serializer[pointerCount];
    }

    protected S2EntityState(S2EntityState other) {
        this.rootField = other.rootField;
        this.pointerSerializers = other.pointerSerializers.clone();
    }

    public SerializerField getRootField() {
        return rootField;
    }

    public Serializer getPointerSerializer(int pointerId) {
        return pointerSerializers[pointerId];
    }

    public int getPointerCount() {
        return pointerSerializers.length;
    }

    public Field getFieldForFieldPath(S2FieldPath fp) {
        Field f = rootField;
        for (int i = 0; i <= fp.last(); i++) {
            f = f.getChild(this, fp.get(i));
            if (f == null) return null;
        }
        return f;
    }

    public String getNameForFieldPath(S2FieldPath fp) {
        var sb = new StringBuilder();

        Field currentField = rootField;
        var i = 0;
        var last = fp.last();
        while (true) {
            var idx = fp.get(i);
            var segment = currentField.getChildNameSegment(this, idx);
            if (segment == null) return null;
            if (i != 0) sb.append('.');
            sb.append(segment);
            if (i == last) return sb.toString();
            currentField = currentField.getChild(this, idx);
            i++;
        }
    }

    public S2FieldPath getFieldPathForName(String fieldName) {
        var fp = S2ModifiableFieldPath.newInstance();

        Field currentField = rootField;
        var search = fieldName;
        while (true) {
            var dotIdx = search.indexOf('.');
            var last = (dotIdx == -1);
            var segment = last ? search : search.substring(0, dotIdx);
            var fieldIdx = currentField.getChildIndex(this, segment);
            if (fieldIdx == null) return null;
            fp.cur(fieldIdx);
            if (last) return fp.unmodifiable();
            fp.down();
            currentField = currentField.getChild(this, fieldIdx);
            search = search.substring(segment.length() + 1);
        }
    }

    public FieldType getTypeForFieldPath(S2FieldPath fp) {
        var f = getFieldForFieldPath(fp);
        return f != null ? f.getType() : null;
    }

    public abstract <T> T getValueForFieldPath(S2FieldPath fp);

    public abstract boolean write(S2FieldPath fp, Object decoded);

    public abstract boolean decodeInto(S2FieldPath fp, Decoder decoder, BitStream bs);

    public abstract boolean applyMutation(S2FieldPath fp, StateMutation mutation);

}
