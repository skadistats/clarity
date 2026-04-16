package skadistats.clarity.model.state;

import skadistats.clarity.io.s1.S1DTClass;
import skadistats.clarity.io.s2.field.SerializerField;

public class EntityStateFactory {

    private final S1EntityStateType s1Type;
    private final S2EntityStateType s2Type;
    private final FieldLayoutBuilder layoutBuilder = new FieldLayoutBuilder();
    private int pointerCount;

    public EntityStateFactory(S1EntityStateType s1Type, S2EntityStateType s2Type) {
        this.s1Type = s1Type;
        this.s2Type = s2Type;
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
