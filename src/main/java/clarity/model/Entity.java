package clarity.model;

import com.rits.cloning.Cloner;



public class Entity implements Cloneable {
    
    private static final Cloner CLONER = new Cloner();
    
    private final int index;
    private final int serial;
    private final DTClass dtClass;
    private PVS pvs;
    private final Object[] state;

    public Entity(int index, int serial, DTClass dtClass, PVS pvs, Object[] state) {
        this.index = index;
        this.serial = serial;
        this.dtClass = dtClass;
        this.pvs = pvs;
        this.state = state;
    }
    
    public int getIndex() {
        return index;
    }

    public int getSerial() {
        return serial;
    }

    public DTClass getDtClass() {
        return dtClass;
    }
    
    public PVS getPvs() {
        return pvs;
    }

    public void setPvs(PVS pvs) {
        this.pvs = pvs;
    }

    public Object[] getState() {
        return state;
    }
    
    @Override
    public Entity clone() {
       return CLONER.deepClone(this);
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
