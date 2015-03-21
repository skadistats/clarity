package skadistats.clarity.model;


import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.prop.PropDecoder;

public class ReceiveProp implements Prop {

    private final SendProp sendProp;
    private final String source;
    private final String name;
    private final PropDecoder propDecoder;

    public ReceiveProp(SendProp sendProp, String source, String name) {
        this.sendProp = sendProp;
        this.source = source;
        this.name = name;
        this.propDecoder = sendProp.getType().getDecoder();
    }

    public SendTableExclusion getExcludeIdentifier() {
        return sendProp.getExcludeIdentifier();
    }

    public PropType getType() {
        return sendProp.getType();
    }

    public String getSrc() {
        return source;
    }

    public String getDtName() {
        return sendProp.getDtName();
    }

    public String getVarName() {
        return name;
    }

    public int getPriority() {
        return sendProp.getPriority();
    }

    public float getLowValue() {
        return sendProp.getLowValue();
    }

    public float getHighValue() {
        return sendProp.getHighValue();
    }

    public int getNumBits() {
        return sendProp.getNumBits();
    }

    public SendProp getTemplate() {
        return sendProp.getTemplate();
    }

    public int getNumElements() {
        return sendProp.getNumElements();
    }
    
    public int getFlags() {
        return sendProp.getFlags();
    }

    public Object decode(BitStream stream) {
        return propDecoder.decode(stream, this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ReceiveProp [source=");
        builder.append(source);
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(getType());
        builder.append(", prio=");
        builder.append(getPriority());
        builder.append("]");
        return builder.toString();
    }

}
