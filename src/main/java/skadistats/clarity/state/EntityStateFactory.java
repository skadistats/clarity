package skadistats.clarity.state;

import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.model.s2.S2FieldPathType;
import skadistats.clarity.model.s2.field.SerializerField;
import skadistats.clarity.state.s1.S1EntityStateType;
import skadistats.clarity.state.s2.FieldLayoutBuilder;
import skadistats.clarity.state.s2.S2EntityStateType;

public class EntityStateFactory {

    private final S1EntityStateType s1Type;
    private final S2EntityStateType s2Type;
    private final S2FieldPathType s2FieldPathType;
    private final FieldLayoutBuilder layoutBuilder = new FieldLayoutBuilder();
    private int pointerCount;

    public EntityStateFactory(S1EntityStateType s1Type, S2EntityStateType s2Type, S2FieldPathType s2FieldPathType) {
        this.s1Type = s1Type;
        this.s2Type = s2Type;
        this.s2FieldPathType = s2FieldPathType;
    }

    public S2FieldPathType getS2FieldPathType() {
        return s2FieldPathType;
    }

    public void setPointerCount(int pointerCount) {
        this.pointerCount = pointerCount;
    }

    public EntityState forS1(S1DTClass dtClass) {
        return s1Type.createState(dtClass);
    }

    public EntityState forS2(SerializerField field) {
        return s2Type.createState(field, pointerCount, layoutBuilder);
    }

}
