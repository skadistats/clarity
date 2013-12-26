package clarity.model;

import org.javatuples.Pair;

import com.dota2.proto.Netmessages.CSVCMsg_SendTable.sendprop_t;

public class SendProp implements Prop {

    private final SendTable table;
    private final sendprop_t sp;
    private final SendProp template;
    private final Pair<String, String> excludeIdentifier;

    public SendProp(SendTable table, sendprop_t sp, SendProp template) {
        this.table = table;
        this.sp = sp;
        this.template = template;
        this.excludeIdentifier = new Pair<String, String>(sp.getDtName(), sp.getVarName());
    }

    public boolean isFlagSet(PropFlag flag) {
        return (sp.getFlags() & flag.getFlag()) != 0;
    }

    public Pair<String, String> getExcludeIdentifier() {
        return excludeIdentifier;
    }

    public PropType getType() {
        return PropType.values()[sp.getType()];
    }

    public String getSrc() {
        return table.getMessage().getNetTableName();
    }

    public String getDtName() {
        return sp.getDtName();
    }

    public String getVarName() {
        return sp.getVarName();
    }

    public int getPriority() {
        return sp.getPriority();
    }

    public float getLowValue() {
        return sp.getLowValue();
    }

    public float getHighValue() {
        return sp.getHighValue();
    }

    public int getNumBits() {
        return sp.getNumBits();
    }

    public SendProp getTemplate() {
        return template;
    }

    public int getNumElements() {
        return sp.getNumElements();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SendProp [name=");
        builder.append(sp.getVarName());
        builder.append(", type=");
        builder.append(getType());
        builder.append(", prio=");
        builder.append(getPriority());
        builder.append("]");
        return builder.toString();
    }

}