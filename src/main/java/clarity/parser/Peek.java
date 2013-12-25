package clarity.parser;

import clarity.parser.packethandler.PacketHandler;

import com.google.protobuf.GeneratedMessage;

public class Peek {
	
	private final PacketHandler<?> packetHandler;
	private final int tick;
	private final GeneratedMessage message;

	public Peek(PacketHandler<?> packetHandler, int tick, GeneratedMessage message) {
		this.packetHandler = packetHandler;
		this.tick = tick;
		this.message = message;
	}
	
	public PacketHandler getPacketHandler() {
		return packetHandler;
	}

	public int getTick() {
		return tick;
	}

	public GeneratedMessage getMessage() {
		return message;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Peek [tick=");
		builder.append(tick);
		builder.append(", type=");
		builder.append(message.getDescriptorForType().getName());
		builder.append(", size=");
		builder.append(message.getSerializedSize());
		builder.append("]");
		return builder.toString();
	}




}
