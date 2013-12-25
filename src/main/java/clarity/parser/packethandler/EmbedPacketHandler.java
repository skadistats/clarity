package clarity.parser.packethandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import clarity.parser.Peek;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;

public abstract class EmbedPacketHandler<T extends GeneratedMessage> extends DemoPacketHandler<T> {

	public EmbedPacketHandler(Class<? extends GeneratedMessage> packetClass) {
		super(packetClass);
	}
	
	protected abstract ByteString getData(GeneratedMessage message);

	public Collection<Peek> scan(int tick, byte[] completeMessage) throws IOException {
		Peek me = super.scan(tick, completeMessage).iterator().next();
		CodedInputStream s = CodedInputStream.newInstance(getData(me.getMessage()).toByteArray());
		List<Peek> result = new ArrayList<Peek>();
		while (!s.isAtEnd()) {
			int kind = s.readRawVarint32();
			int size = s.readRawVarint32();
			byte[] message = s.readRawBytes(size);
			PacketHandler<?> h = PacketHandlerRegistry.forEmbed(kind);
			if (h != null) {
				result.addAll(h.scan(tick, message));
			}
		}
		return result;
	}

}
