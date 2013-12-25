package clarity.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xerial.snappy.Snappy;

import clarity.parser.packethandler.PacketHandler;
import clarity.parser.packethandler.PacketHandlerRegistry;

import com.dota2.proto.Demo.EDemoCommands;
import com.google.protobuf.CodedInputStream;

public class ReplayFile {

	private final File file;
	private final int fileInfoOffset;
	private List<Peek> index;

	public ReplayFile(String fileName) throws IOException {
		file = new File(fileName);
		CodedInputStream s = CodedInputStream.newInstance(new FileInputStream(file));
		s.setSizeLimit(Integer.MAX_VALUE);
		
		String header = new String(s.readRawBytes(8));
		if (!"PBUFDEM\0".equals(header)) {
			throw new IOException("replay does not have the proper header");
		}

		fileInfoOffset = s.readFixed32();

		index = new ArrayList<Peek>();
		int tick = 0;
		int kind = 0;
		do {
			kind = s.readRawVarint32();
			boolean isCompressed = (kind & EDemoCommands.DEM_IsCompressed_VALUE) != 0;
			kind &= ~EDemoCommands.DEM_IsCompressed_VALUE;
			tick = s.readRawVarint32();
			int size = s.readRawVarint32();
			byte[] message = s.readRawBytes(size);
			if (isCompressed) {
				message = Snappy.uncompress(message);
			}
			PacketHandler<?> h = PacketHandlerRegistry.forDemo(kind);
			if (h != null) {
				index.addAll(h.scan(tick, message));
			}
		} while (kind != 0);

	}
	
	public Iterator<Peek> iterator() {
		return index.iterator();
	}
}
