package clarity.model;
import java.io.UnsupportedEncodingException;

import com.google.protobuf.ByteString;


public class StringTableEntry {

	private final int index;
	private final String name;
	private final ByteString value;
	
	public StringTableEntry(int index, String name, ByteString value) {
		this.index = index;
		this.name = name;
		this.value = value;
	}

	public int getIndex() {
		return index;
	}

	public String getName() {
		return name;
	}

	public ByteString getValue() {
		return value;
	}

	@Override
	public String toString() {
		try {
			StringBuilder builder = new StringBuilder();
			builder.append("StringTableEntry [index=");
			builder.append(index);
			builder.append(", name=");
			builder.append(name);
			builder.append(", value=");
			builder.append(value == null ? "NULL" : value.toString("ISO-8859-1"));
			builder.append("]");
			return builder.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	
}
