package skadistats.clarity.model.s2.field;

import skadistats.clarity.model.s2.Field;
import skadistats.clarity.model.s2.FieldType;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.state.s2.S2EntityState;

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
    public Field getChild(S2EntityState state, int idx) {
        return serializer.getField(idx);
    }

    @Override
    public Integer getChildIndex(S2EntityState state, String nameSegment) {
        return serializer.getFieldIndex(nameSegment);
    }

    @Override
    public String getChildNameSegment(S2EntityState state, int idx) {
        return serializer.getFieldName(idx);
    }

}
