package skadistats.clarity.decoder.s1;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.util.TextTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class S1DTClass implements DTClass {

    private final String dtName;
    private final SendTable sendTable;
    private int classId = -1;
    private ReceiveProp[] receiveProps;
    private Map<String, Integer> propsByName;
    private S1DTClass superClass;

    public S1DTClass(String dtName, SendTable sendTable) {
        this.dtName = dtName;
        this.sendTable = sendTable;
    }

    @Override
    public int getClassId() {
        return classId;
    }

    @Override
    public void setClassId(int classId) {
        this.classId = classId;
    }

    @Override
    public Object[] getEmptyStateArray() {
        return new Object[receiveProps.length];
    }

    @Override
    public String getNameForFieldPath(FieldPath fp) {
        return this.receiveProps[fp.path[0]].getVarName();
    }

    @Override
    public FieldPath getFieldPathForName(String name){
        Integer idx = this.propsByName.get(name);
        return idx != null ? new FieldPath(idx.intValue()) : null;
    }

    @Override
    public <T> T getValueForFieldPath(FieldPath fp, Object[] state) {
        return (T) state[fp.path[0]];
    }

    public S1DTClass getSuperClass() {
        return superClass;
    }

    public void setSuperClass(S1DTClass superClass) {
        this.superClass = superClass;
    }

    public String getDtName() {
        return dtName;
    }
    
    public boolean instanceOf(String dtName) {
        S1DTClass s = this;
        while (s != null) {
            if (s.getDtName().equals(dtName)) {
                return true;
            }
            s = s.getSuperClass();
        }
        return false;
    }
    
    public SendTable getSendTable() {
        return sendTable;
    }

    public ReceiveProp[] getReceiveProps() {
        return receiveProps;
    }

    public void setReceiveProps(ReceiveProp[] receiveProps) {
        this.receiveProps = receiveProps;
        this.propsByName = new HashMap<>();
        for(int i = 0; i < receiveProps.length; ++i) {
            this.propsByName.put(receiveProps[i].getVarName(), i);
        }
    }


    private static final ReentrantLock DEBUG_LOCK = new ReentrantLock();
    private static final TextTable DEBUG_DUMPER = new TextTable.Builder()
        .setFrame(TextTable.FRAME_COMPAT)
        .addColumn("Idx", TextTable.Alignment.RIGHT)
        .addColumn("Property")
        .addColumn("Value")
        .build();

    @Override
    public String dumpState(String title, Object[] state) {
        DEBUG_LOCK.lock();
        try {
            DEBUG_DUMPER.clear();
            DEBUG_DUMPER.setTitle(title);
            for (int i = 0; i < state.length; i++) {
                DEBUG_DUMPER.setData(i, 0, i);
                DEBUG_DUMPER.setData(i, 1, receiveProps[i].getVarName());
                DEBUG_DUMPER.setData(i, 2, state[i]);
            }
            return DEBUG_DUMPER.toString();
        } finally {
            DEBUG_LOCK.unlock();
        }
    }

    @Override
    public List<FieldPath> collectFieldPaths(Object[] state) {
        ArrayList<FieldPath> result = new ArrayList<>(state.length);
        for (int i = 0; i < state.length; i++) {
            result.add(new FieldPath(i));
        }
        return result;
    }

}
