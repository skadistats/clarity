package clarity.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StringTableCollection {

	private final List<StringTable> byId = new ArrayList<StringTable>();
	private final Map<String, StringTable> byName = new TreeMap<String, StringTable>();
	
	public void add(StringTable table) {
		byId.add(table);
		byName.put(table.getName(), table);
	}
	
	public StringTable byName(String name) {
		return byName.get(name);
	}
	
	public StringTable byId(int id) {
		return byId.get(id);
	}
	
	
}
