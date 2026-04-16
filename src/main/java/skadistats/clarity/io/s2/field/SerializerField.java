package skadistats.clarity.io.s2.field;

import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.model.state.S2AbstractEntityState;

public class SerializerField extends Field {

    protected Serializer serializer;

    public SerializerField(FieldType fieldType, Serializer serializer) {
        super(fieldType);
        this.serializer = serializer;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public Field getChild(S2AbstractEntityState state, int idx) {
        return serializer.getField(idx);
    }

    @Override
    public Integer getChildIndex(S2AbstractEntityState state, String nameSegment) {
        return serializer.getFieldIndex(nameSegment);
    }

    @Override
    public String getChildNameSegment(S2AbstractEntityState state, int idx) {
        return serializer.getFieldName(idx);
    }

}
