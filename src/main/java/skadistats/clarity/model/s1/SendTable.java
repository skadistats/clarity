package skadistats.clarity.model.s1;

import java.util.List;

public class SendTable {

    private final String netTableName;
    private final List<SendProp> props;

    public SendTable(String netTableName, List<SendProp> props) {
        this.netTableName = netTableName;
        this.props = props;
    }
    
    public String getNetTableName() {
        return netTableName;
    }

    public List<SendProp> getSendProps() {
        return props;
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
