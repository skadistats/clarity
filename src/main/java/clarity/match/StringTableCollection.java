package clarity.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import clarity.model.StringTable;

import com.rits.cloning.Cloner;

public class StringTableCollection implements Cloneable {

    private static final Cloner CLONER = new Cloner();
    
    private final List<StringTable> byId = new ArrayList<StringTable>();
    private final Map<String, StringTable> byName = new TreeMap<String, StringTable>();

    public void add(StringTable table) {
        byId.add(table);
        byName.put(table.getName(), table);
    }

    public StringTable forName(String name) {
        return byName.get(name);
    }

    public StringTable forId(int id) {
        return byId.get(id);
    }
    
    @Override
    public StringTableCollection clone() {
       return CLONER.deepClone(this);
    }
    
}
