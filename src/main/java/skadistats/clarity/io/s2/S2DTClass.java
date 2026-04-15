package skadistats.clarity.io.s2;

import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.EntityStateFactory;

public class S2DTClass implements DTClass {

    private final SerializerField field;
    private int classId = -1;
    private EntityStateFactory entityStateFactory;

    public S2DTClass(SerializerField field) {
        this.field = field;
    }

    @Override
    public int getClassId() {
        return classId;
    }

    @Override
    public void setClassId(int classId) {
        this.classId = classId;
    }

    @Override
    public String getDtName() {
        return field.getSerializer().getId().getName();
    }

    public Serializer getSerializer() {
        return field.getSerializer();
    }

    public SerializerField getField() {
        return field;
    }

    @Override
    public void setEntityStateFactory(EntityStateFactory factory) {
        this.entityStateFactory = factory;
    }

    @Override
    public EntityState getEmptyState() {
        return entityStateFactory.forS2(field);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", field.getSerializer().getId(), classId);
    }

}
