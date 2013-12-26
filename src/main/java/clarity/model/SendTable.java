package clarity.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.javatuples.Pair;

import com.dota2.proto.Netmessages.CSVCMsg_SendTable;
import com.dota2.proto.Netmessages.CSVCMsg_SendTable.sendprop_t;

public class SendTable {

    private final CSVCMsg_SendTable message;
    private final LinkedList<SendProp> props;

    public SendTable(CSVCMsg_SendTable message) {
        this.message = message;
        this.props = new LinkedList<SendProp>();

        for (sendprop_t sp : message.getPropsList()) {
            SendProp tpl = sp.getType() == PropType.ARRAY.ordinal() ? props.peekLast() : null;
            props.add(new SendProp(this, sp, tpl));
        }
    }

    public CSVCMsg_SendTable getMessage() {
        return message;
    }

    public Set<Pair<String, String>> getAllExclusions() {
        Set<Pair<String, String>> result = new HashSet<Pair<String, String>>();
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

    public SendProp getBaseClass() {
        for (SendProp sp : props) {
            if ("baseclass".equals(sp.getVarName())) {
                return sp;
            }
        }
        return null;
    }

}
