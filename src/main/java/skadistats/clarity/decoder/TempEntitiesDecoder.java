package skadistats.clarity.decoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.ReceiveProp;
import skadistats.clarity.processor.sendtables.DTClasses;

import java.util.ArrayList;
import java.util.List;

public class TempEntitiesDecoder {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DTClasses dtClasses;
    private final int classBits;
    private final int numEntries;
    private final EntityBitStream stream;

    public TempEntitiesDecoder(byte[] data, int numEntries, DTClasses dtClasses) {
        this.dtClasses = dtClasses;
        this.stream = new EntityBitStream(data);
        this.numEntries = numEntries;
        this.classBits = Util.calcBitsNeededFor(dtClasses.size() - 1);
    }

    public List<Entity> decode() {
        ArrayList<Entity> result = new ArrayList<>();
        DTClass cls = null;
        int count = 0;
        while (count < numEntries) {
            stream.readBit(); // seems to be always 0
            if (stream.readBit()) {
                cls = dtClasses.forClassId(stream.readNumericBits(classBits) - 1);
            }
            Object[] state = null;
            List<Integer> propList = null;
            propList = stream.readEntityPropList();
            state = new Object[cls.getReceiveProps().size()];
            decodeProperties(state, cls, propList);
            result.add(new Entity(0, 0, cls, null, state));
            count++;
        }
        return result;
    }

    private void decodeProperties(Object[] state, DTClass cls, List<Integer> propIndices) {
        for (Integer i : propIndices) {
            ReceiveProp r = cls.getReceiveProps().get(i);
            Object dec = r.getType().getDecoder().decode(stream, r);
            state[i] = dec;
        }
    }

}
