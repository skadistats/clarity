package clarity.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import clarity.model.EntityCollection;
import clarity.model.ReceiveProp;
import clarity.model.SendTable;
import clarity.model.StringTable;

public class Match {

	private final Map<String, Integer> classByDT = new TreeMap<String, Integer>(); 
	private final Map<String, SendTable> sendTableByDT = new TreeMap<String, SendTable>();
	private final Map<Integer, List<ReceiveProp>> receivePropsByClass = new TreeMap<Integer, List<ReceiveProp>>();

	private final List<StringTable> stringTables = new ArrayList<StringTable>();
	private final EntityCollection entityCollection = new EntityCollection();

	public Map<String, Integer> getClassByDT() {
		return classByDT;
	}

	public Map<String, SendTable> getSendTableByDT() {
		return sendTableByDT;
	}
	
	public List<StringTable> getStringTables() {
		return stringTables;
	}
	
	public StringTable getStringTableByName(String name) {
		for (StringTable t : stringTables) {
			if (t.getName().equals(name)) {
				return t;
			}
		}
		return null;
	}

	public Map<Integer, List<ReceiveProp>> getReceivePropsByClass() {
		return receivePropsByClass;
	}

	public EntityCollection getEntityCollection() {
		return entityCollection;
	}
	
	
	
}
