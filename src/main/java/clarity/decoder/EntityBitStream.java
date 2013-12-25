package clarity.decoder;

import java.util.ArrayList;
import java.util.List;

import clarity.model.PVS;

public class EntityBitStream extends BitStream {

	public EntityBitStream(byte[] data) {
		super(data);
	}
	
	public int readEntityIndex(int baseIndex) {
        int encodedIndex = readNumericBits(6);
        if ((encodedIndex & 0x30) != 0) {
            int a = (encodedIndex >> 4) & 3;
            int b = a == 3 ? 16 : 0;
            int i = readNumericBits(4 * a + b) << 4;
            encodedIndex = i | (encodedIndex & 0x0f);
        }
        return baseIndex + encodedIndex + 1;
	}
	
	
    public PVS readEntityPVS() {
    	return PVS.values()[(readNumericBits(1) << 1) | readNumericBits(1)];
    }
    
    public List<Integer> readEntityPropList() {
        List<Integer> prop_list = new ArrayList<Integer>();
        int cursor = -1;

        while(true) {
            if (readBit()) {
                cursor += 1;
            } else {
                int offset = readVarInt();
                if (offset == 0x3fff) {
                    return prop_list;
                } else {
                    cursor += offset + 1;
                }
            }
            prop_list.add(cursor);
        }
    }
    
}
