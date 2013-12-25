package clarity.parser.packethandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import clarity.match.Match;
import clarity.parser.Peek;

import com.google.protobuf.GeneratedMessage;

public class DemoPacketHandler<T extends GeneratedMessage> implements PacketHandler<T> {
	
	protected final Class<? extends GeneratedMessage> packetClass;
	
	public DemoPacketHandler(Class<? extends GeneratedMessage> packetClass) {
		this.packetClass = packetClass;
	}

	public Collection<Peek> scan(int tick, byte[] message) throws IOException {
		return Arrays.asList(new Peek(this, tick, parseClass(packetClass, message)));
	}
	
	protected GeneratedMessage parseClass(Class<? extends GeneratedMessage> clazz, byte[] message) {
		try {
			Method m = clazz.getMethod("parseFrom", byte[].class);
			return (GeneratedMessage) m.invoke(null, message);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void apply(T message, Match match) {
	}
	
}
