package skadistats.clarity.io.s1;


import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.s1.PropType;

public class SendProp {

    private final SendTable table;
    private final SendProp template;

    private final PropType type;
    private final String varName;
    private final int flags;
    private final int priority;
    private final String dtName;
    private final int numElements;
    private final float lowValue;
    private final float highValue;
    private final int numBits;

    private final SendTableExclusion excludeIdentifier;
    private final Decoder decoder;


    public SendProp(SendTable table, SendProp template, int type, String varName, int flags, int priority, String dtName, int numElements, float lowValue, float highValue, int numBits) {
        this.table = table;
        this.template = template;
        this.type = PropType.values()[type];
        this.varName = varName;
        this.flags = flags;
        this.priority = priority;
        this.dtName = dtName;
        this.numElements = numElements;
        this.lowValue = lowValue;
        this.highValue = highValue;
        this.numBits = numBits;
        this.excludeIdentifier = new SendTableExclusion(dtName, varName);
        this.decoder = S1DecoderFactory.createDecoder(this);
    }

    public SendTableExclusion getExcludeIdentifier() {
        return excludeIdentifier;
    }

    public PropType getType() {
        return type;
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

    public Decoder getDecoder() {
        return decoder;
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
