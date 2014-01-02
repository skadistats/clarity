package clarity.decoder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.DTClassCollection;
import clarity.match.EntityCollection;
import clarity.model.DTClass;
import clarity.model.PropFlag;
import clarity.model.ReceiveProp;
import clarity.model.StringTable;

import com.google.protobuf.ByteString;

public class TempEntitiesDecoder {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DTClassCollection dtClasses;
    private final StringTable baseline;
    private final int classBits;
    private final int numEntries;
    private final EntityBitStream stream;

    public TempEntitiesDecoder(byte[] data, int numEntries, DTClassCollection dtClasses, StringTable baseline) {
        this.dtClasses = dtClasses;
        this.baseline = baseline;
        this.stream = new EntityBitStream(data);
        this.numEntries = numEntries;
        this.classBits = Util.calcBitsNeededFor(dtClasses.size() - 1);
    }

    public void decodeAndApply(EntityCollection world) {
        log.trace("\n\n------------------------------------------------------------------------------------------------------------\nreading {} temp entities", numEntries);
        log.trace("begin: {}", stream);
        DTClass cls = null;
        int count = 0;
        while (count < numEntries) {
            stream.readBit(); // seems to be always 0
            if (stream.readBit()) {
                cls = dtClasses.forClassId(stream.readNumericBits(classBits) - 1);
            }
            Object[] state = null;
            List<Integer> propList = null;
            log.trace("before propList: {}", stream);
            propList = stream.readEntityPropList();
            log.trace("before props: {}", stream);
            state = new Object[cls.getReceiveProps().size()];
            decodeProperties(state, cls, propList);
            log.trace("after props: {}", stream);
            log.debug("          [type={}, propList={}, state={}]",
                cls.getDtName(),
                propList,
                state
            );
            
            count++;
        }
    }

    private void decodeProperties(Object[] state, DTClass cls, List<Integer> propIndices) {
        for (Integer i : propIndices) {
            ReceiveProp r = cls.getReceiveProps().get(i);
            log.trace("decode {} with flags {}", r.getType(), PropFlag.dump(r.getFlags()));
            Object dec = r.getType().getDecoder().decode(stream, r);
            state[i] = dec;
        }
    }

    private Object[] decodeBaseProperties(DTClass cls) {
        ByteString s = baseline.getValueByName(String.valueOf(cls.getClassId()));
        return BaseInstanceDecoder.decode(
            s.toByteArray(),
            cls.getReceiveProps()
            );
    }

}
