package skadistats.clarity.model.state;

public interface EntityState {

    int length();

    boolean has(int idx);

    Object get(int idx);
    void set(int idx, Object value);

    default EntityState sub(int idx) {
        return (EntityState) get(idx);
    }

    EntityState clone();

}
