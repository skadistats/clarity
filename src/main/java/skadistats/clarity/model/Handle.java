package skadistats.clarity.model;

public class Handle {
    
    public static final int INDEX_BITS = 11;
    public static final int SERIAL_BITS = 10;
    public static final int INDEX_MASK = (1 << INDEX_BITS) - 1;
    public static final int MAX = (1 << (INDEX_BITS + SERIAL_BITS)) - 1; 
    
    public static int indexForHandle(int handle) {
        return handle & INDEX_MASK;
    }
    
    public static int serialForHandle(int handle) {
        return handle >> INDEX_BITS;
    }
    
    public static int forIndexAndSerial(int index, int serial) {
        return serial << INDEX_BITS | index;
    }
    
}
