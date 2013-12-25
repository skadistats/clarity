package clarity.model;

import com.dota2.proto.Netmessages.CSVCMsg_CreateStringTable;
import com.google.protobuf.ByteString;

public class StringTable {

	private final CSVCMsg_CreateStringTable createMessage;
	private final String[] names;
	private final ByteString[] values;
	
	public StringTable(CSVCMsg_CreateStringTable createMessage) {
		this.createMessage = createMessage;
		this.names = new String[createMessage.getMaxEntries()];
		this.values = new ByteString[createMessage.getMaxEntries()];
	}
	
	public void set(int index, String name, ByteString value) {
		if (index < names.length) { 
			this.names[index] = name;
			this.values[index] = value;
		} else {
			throw new RuntimeException("out of index (" + index + "/" + names.length + ")");
		}
	}

	public int getMaxEntries() {
		return createMessage.getMaxEntries();
	}

	public boolean getUserDataFixedSize() {
		return createMessage.getUserDataFixedSize();
	}

	public int getUserDataSize() {
		return createMessage.getUserDataSize();
	}

	public int getUserDataSizeBits() {
		return createMessage.getUserDataSizeBits();
	}

	public String getName() {
		return createMessage.getName();
	}
	
}
