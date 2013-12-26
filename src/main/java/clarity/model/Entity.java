package clarity.model;


public class Entity {

    private final Integer index;
    private final Integer serial;
    private final DTClass dtClass;
    private final Object[] state;

    public Entity(Integer index, Integer serial, DTClass dtClass, Object[] state) {
        this.index = index;
        this.serial = serial;
        this.dtClass = dtClass;
        this.state = state;
    }

    public Integer getIndex() {
        return index;
    }

    public Integer getSerial() {
        return serial;
    }

    public DTClass getDtClass() {
        return dtClass;
    }

    public Object[] getState() {
        return state;
    }

}
