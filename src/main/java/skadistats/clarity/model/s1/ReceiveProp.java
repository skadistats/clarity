package skadistats.clarity.model.s1;


import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s1.prop.PropDecoder;

public class ReceiveProp {

    private final SendProp sendProp;
    private final String name;
    private final PropDecoder propDecoder;

    public ReceiveProp(SendProp sendProp, String name) {
        this.sendProp = sendProp;
        this.name = name;
        this.propDecoder = sendProp.getType().getDecoder();
    }

    public SendProp getSendProp() {
        return sendProp;
    }

    public String getVarName() {
        return name;
    }

    public Object decode(BitStream stream) {
        return propDecoder.decode(stream, sendProp);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ReceiveProp [source=");
        builder.append(sendProp.getSrc());
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(sendProp.getType());
        builder.append(", prio=");
        builder.append(sendProp.getPriority());
        builder.append("]");
        return builder.toString();
    }

}
