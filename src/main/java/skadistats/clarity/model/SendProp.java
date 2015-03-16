package skadistats.clarity.model;


public class SendProp implements Prop {

    private final SendTable table;
    private final SendProp template;
    
    private final int type;
    private final String varName;
    private final int flags;
    private final int priority;
    private final String dtName;
    private final int numElements;
    private final float lowValue;
    private final float highValue;
    private final int numBits;

    public SendProp(SendTable table, SendProp template, int type, String varName, int flags, int priority, String dtName, int numElements, float lowValue, float highValue, int numBits) {
        this.table = table;
        this.template = template;
        this.type = type;
        this.varName = varName;
        this.flags = flags;
        this.priority = priority;
        this.dtName = dtName;
        this.numElements = numElements;
        this.lowValue = lowValue;
        this.highValue = highValue;
        this.numBits = numBits;
    }

    public SendTableExclusion getExcludeIdentifier() {
        return new SendTableExclusion(dtName, varName);
    }

    public PropType getType() {
        return PropType.values()[type];
    }

    public String getSrc() {
        return table.getNetTableName();
    }

    public SendProp getTemplate() {
        return template;
    }

    public String getVarName() {
        return varName;
    }

    public int getPriority() {
        return priority;
    }

    public String getDtName() {
        return dtName;
    }

    public int getNumElements() {
        return numElements;
    }

    public float getLowValue() {
        return lowValue;
    }

    public float getHighValue() {
        return highValue;
    }

    public int getNumBits() {
        return numBits;
    }

    public int getFlags() {
        return flags;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SendProp [name=");
        builder.append(getVarName());
        builder.append(", type=");
        builder.append(getType());
        builder.append(", prio=");
        builder.append(getPriority());
        builder.append("]");
        return builder.toString();
    }


}