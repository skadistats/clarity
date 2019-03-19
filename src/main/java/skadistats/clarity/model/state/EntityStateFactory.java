package skadistats.clarity.model.state;

public class EntityStateFactory {

    public static EntityState withLength(int length) {
        return new ArrayEntityState(length);
    }
}
