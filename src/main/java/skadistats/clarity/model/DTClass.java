package skadistats.clarity.model;

import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s2.S2DTClass;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.s2.S2EntityState;

/**
 * The class of an entity: its name and class id. The implementations are
 * {@code S1DTClass} (Source 1, based on send tables) and {@code S2DTClass}
 * (Source 2, based on serializers).
 */
public sealed interface DTClass permits S1DTClass, S2DTClass {

    /**
     * @return the class name
     */
    String getDtName();

    /**
     * @return the class id as assigned in the replay's class info
     */
    int getClassId();
    /** Internal to the parser. */
    void setClassId(int classId);

    /** Internal helper behind {@link Entity#getNameForFieldPath}. */
    static String getNameForFieldPath(DTClass dtClass, EntityState state, FieldPath fp) {
        return switch (dtClass) {
            case S1DTClass s1 -> s1.getNameForFieldPath((S1FieldPath) fp);
            case S2DTClass ignored -> ((S2EntityState) state).getNameForFieldPath((S2FieldPath) fp);
        };
    }

    /** Internal helper behind {@link Entity#getFieldPathForName}. */
    static FieldPath getFieldPathForName(DTClass dtClass, EntityState state, String name) {
        return switch (dtClass) {
            case S1DTClass s1 -> s1.getFieldPathForName(name);
            case S2DTClass ignored -> ((S2EntityState) state).getFieldPathForName(name);
        };
    }

}
