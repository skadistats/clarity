package clarity.model;

public class Handle {
    
    public static final int INDEX_BITS = 11;
    public static final int INDEX_MASK = 0x7FF;

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
