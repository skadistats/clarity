package skadistats.clarity.decoder;

import skadistats.clarity.model.PVS;

import java.util.ArrayList;
import java.util.List;

public class EntityBitStream extends BitStream {

    public EntityBitStream(byte[] data) {
        super(data);
    }

    public int readEntityIndex(int baseIndex) {
        // Thanks to Robin Dietrich for providing a clean version of this code :-)

        // The header looks like this: [XY00001111222233333333333333333333] where everything > 0 is optional.
        // The first 2 bits (X and Y) tell us how much (if any) to read other than the 6 initial bits:
        // Y set -> read 4
        // X set -> read 8
        // X + Y set -> read 28

        int offset = readNumericBits(6);
        switch (offset & 48) {
            case 16:
                offset = (offset & 15) | (readNumericBits(4) << 4);
                break;
            case 32:
                offset = (offset & 15) | (readNumericBits(8) << 4);
                break;
            case 48:
                offset = (offset & 15) | (readNumericBits(28) << 4);
                break;
        }
        return baseIndex + offset + 1;
    }

    public PVS readEntityPVS() {
        return PVS.values()[(readNumericBits(1) << 1) | readNumericBits(1)];
    }

    public List<Integer> readEntityPropList() {
        List<Integer> propList = new ArrayList<Integer>();
        int cursor = -1;

        while (true) {
            if (readNumericBits(1) == 1) {
                cursor += 1;
            } else {
                int offset = readVarInt();
                if (offset == 0x3fff) {
                    return propList;
                } else {
                    cursor += offset + 1;
                }
            }
            propList.add(cursor);
        }
    }

}
