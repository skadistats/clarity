package clarity.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
            if (sp.isFlagSet(PropFlag.EXCLUDE)) {
                result.add(sp.getExcludeIdentifier());
            }
        }
        return result;
    }

    public List<SendProp> getAllNonExclusions() {
        List<SendProp> result = new LinkedList<SendProp>(props);
        for (Iterator<SendProp> iter = result.iterator(); iter.hasNext();) {
            if (iter.next().isFlagSet(PropFlag.EXCLUDE)) {
                iter.remove();
            }
        }
        return result;
    }

    public List<SendProp> getAllRelations() {
        List<SendProp> result = new LinkedList<SendProp>(props);
        for (Iterator<SendProp> iter = result.iterator(); iter.hasNext();) {
            SendProp sp = iter.next();
            if (sp.isFlagSet(PropFlag.EXCLUDE) || sp.getType() != PropType.DATATABLE) {
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
