package skadistats.clarity.match;

import skadistats.clarity.model.StringTable;

import java.util.Arrays;
import java.util.List;

public class StringTableCollection extends BaseCollection<StringTableCollection, StringTable> {

    private Index<String, StringTable> byNameIdx;

    @Override
    protected List<Index> initialIndices() {
        byNameIdx = new Index<String, StringTable>() {
            @Override
            String getKey(StringTable value) {
                return value.getName();
            }
        };
        return Arrays.<Index>asList(byNameIdx);
    }

    public StringTable forName(String name) {
        return byNameIdx.get(name);
    }

    public StringTable forId(int id) {
        return values.get(id);
    }
    

}
