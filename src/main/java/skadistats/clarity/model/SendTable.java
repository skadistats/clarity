package skadistats.clarity.model;

import java.util.*;

public class SendTable {

    private final String netTableName;
    private final boolean decoderNeeded;
    private final List<SendProp> props;

    public SendTable(String netTableName, boolean decoderNeeded, List<SendProp> props) {
        this.netTableName = netTableName;
        this.decoderNeeded = decoderNeeded;
        this.props = props;
    }
    
    public String getNetTableName() {
        return netTableName;
    }

    public boolean isDecoderNeeded() {
        return decoderNeeded;
    }

    public Set<SendTableExclusion> getAllExclusions() {
        Set<SendTableExclusion> result = new HashSet<SendTableExclusion>();
        for (SendProp sp : props) {
            if ((sp.getFlags() & PropFlag.EXCLUDE) != 0) {
                result.add(sp.getExcludeIdentifier());
            }
        }
        return result;
    }

    public List<SendProp> getAllNonExclusions() {
        List<SendProp> result = new LinkedList<SendProp>(props);
        for (Iterator<SendProp> iter = result.iterator(); iter.hasNext();) {
            if ((iter.next().getFlags() & PropFlag.EXCLUDE) != 0) {
                iter.remove();
            }
        }
        return result;
    }

    public List<SendProp> getAllRelations() {
        List<SendProp> result = new LinkedList<SendProp>(props);
        for (Iterator<SendProp> iter = result.iterator(); iter.hasNext();) {
            SendProp sp = iter.next();
            if ((sp.getFlags() & PropFlag.EXCLUDE) != 0 || sp.getType() != PropType.DATATABLE) {
                iter.remove();
            }
        }
        return result;
    }
    
    public String getBaseClass() {
        for (SendProp sp : props) {
            if ("baseclass".equals(sp.getVarName())) {
                return sp.getDtName();
            }
        }
        return null;
    }
    

}
