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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n\n-- Entity [index=");
        builder.append(index);
        builder.append(", serial=");
        builder.append(serial);
        builder.append(", dtClass=");
        builder.append(dtClass.getDtName());
        builder.append("]");
        for (int i = 0; i < state.length; i++) {
            builder.append("\n");
            builder.append(dtClass.getReceiveProps().get(i).getVarName());
            builder.append(" = ");
            builder.append(state[i]);
        }
        
        return builder.toString();
    }
    

}
