package clarity.parser.packethandler;

import java.io.IOException;
import java.util.Collection;

import clarity.match.Match;
import clarity.parser.Peek;

public interface PacketHandler<T> {

	Collection<Peek> scan(int tick, byte[] message) throws IOException;
	void apply(T message, Match m);
	
}
