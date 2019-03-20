package skadistats.clarity.model.state;

public class EntityStateFactory {

    public static CloneableEntityState withLength(int length) {
        return new ArrayEntityState(length);
    }

}
