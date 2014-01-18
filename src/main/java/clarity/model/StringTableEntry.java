package clarity.model;

import com.google.protobuf.ByteString;

public class StringTableEntry {
    
    private final int index;
    private final String key;
    private final ByteString value;
    
    public StringTableEntry(int index, String key, ByteString value) {
        this.index = index;
        this.key = key;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public String getKey() {
        return key;
    }

    public ByteString getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + index;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StringTableEntry other = (StringTableEntry) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        if (index != other.index)
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }
    
}
