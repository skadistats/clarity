package clarity.decoder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.DTClassCollection;
import clarity.match.TempEntityCollection;
import clarity.model.DTClass;
import clarity.model.ReceiveProp;

public class TempEntitiesDecoder {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DTClassCollection dtClasses;
    private final int classBits;
    private final int numEntries;
    private final EntityBitStream stream;

    public TempEntitiesDecoder(byte[] data, int numEntries, DTClassCollection dtClasses) {
        this.dtClasses = dtClasses;
        this.stream = new EntityBitStream(data);
        this.numEntries = numEntries;
        this.classBits = Util.calcBitsNeededFor(dtClasses.size() - 1);
    }

    public void decodeAndApply(TempEntityCollection world) {
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
            world.add(cls, state);
            log.debug("          {} [state={}]",
                cls.getDtName(),
                state
            );
            count++;
        }
    }

    private void decodeProperties(Object[] state, DTClass cls, List<Integer> propIndices) {
        for (Integer i : propIndices) {
            ReceiveProp r = cls.getReceiveProps().get(i);
            Object dec = r.getType().getDecoder().decode(stream, r);
            state[i] = dec;
        }
    }

}
