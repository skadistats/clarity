package skadistats.clarity.model;

import skadistats.clarity.io.s1.S1DTClass;
import skadistats.clarity.io.s2.S2DTClass;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.EntityStateFactory;
import skadistats.clarity.model.state.S2EntityState;

public sealed interface DTClass permits S1DTClass, S2DTClass {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    void setEntityStateFactory(EntityStateFactory factory);
    EntityState getEmptyState();

    static String getNameForFieldPath(DTClass dtClass, EntityState state, FieldPath fp) {
        return switch (dtClass) {
            case S1DTClass s1 -> s1.getNameForFieldPath((S1FieldPath) fp);
            case S2DTClass ignored -> ((S2EntityState) state).getNameForFieldPath((S2FieldPath) fp);
        };
    }

    static FieldPath getFieldPathForName(DTClass dtClass, EntityState state, String name) {
        return switch (dtClass) {
            case S1DTClass s1 -> s1.getFieldPathForName(name);
            case S2DTClass ignored -> ((S2EntityState) state).getFieldPathForName(name);
        };
    }

}
