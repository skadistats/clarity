package skadistats.clarity.io.s1;


import skadistats.clarity.io.bitstream.BitStream;

public class ReceiveProp {

    private final SendProp sendProp;
    private final String name;

    public ReceiveProp(SendProp sendProp, String name) {
        this.sendProp = sendProp;
        this.name = name;
    }

    public SendProp getSendProp() {
        return sendProp;
    }

    public String getVarName() {
        return name;
    }

    public Object decode(BitStream stream) {
        return sendProp.getDecoder().decode(stream);
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
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
