package skadistats.clarity.model;

import java.util.List;

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
