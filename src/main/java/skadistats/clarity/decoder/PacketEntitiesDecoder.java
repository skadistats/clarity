package skadistats.clarity.decoder;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.*;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.sendtables.DTClasses;

import java.util.List;

public class PacketEntitiesDecoder {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DTClasses dtClasses;
    private final StringTable baseline;
    private final int classBits;
    private final int numEntries;
    private final boolean isDelta;
    private final BitStream stream;

    public PacketEntitiesDecoder(byte[] data, int numEntries, boolean isDelta, DTClasses dtClasses, StringTable baseline) {
        this.dtClasses = dtClasses;
        this.baseline = baseline;
        this.stream = new BitStream(data);
        this.numEntries = numEntries;
        this.isDelta = isDelta;
        this.classBits = Util.calcBitsNeededFor(dtClasses.size() - 1);
    }

    public void decodeAndApply(Entities world) {
        int index = -1;
        int count = 0;
        while (count < numEntries) {
            index = decodeDiff(index, world);
            count++;
        }
        if (isDelta) {
            decodeDeletionDiffs(world);
        }
    }

    private int decodeDiff(int index, Entities entities) {
        index = stream.readEntityIndex(index);
        PVS pvs = stream.readEntityPVS();
        DTClass cls = null;
        Integer serial = null;
        Object[] state = null;
        List<Integer> propList = null;
        Entity entity = null;
        switch (pvs) {
        case ENTER:
            cls = dtClasses.forClassId(stream.readNumericBits(classBits));
            serial = stream.readNumericBits(10);
            propList = stream.readEntityPropList();
            state = decodeBaseProperties(cls);
            decodeProperties(state, cls, propList);
            entities.add(index, serial, cls, pvs, state);
            log.debug("          {} [index={}, serial={}, handle={}, type={}]",
                pvs,
                index,
                serial,
                Handle.forIndexAndSerial(index, serial),
                cls.getDtName()
            );
            break;
        case PRESERVE:
            entity = entities.getByIndex(index);
            entity.setPvs(pvs);
            cls = entity.getDtClass();
            serial = entity.getSerial();
            propList = stream.readEntityPropList();
            decodeProperties(entity.getState(), cls, propList);
            break;
        case LEAVE:
            entity = entities.getByIndex(index);
            entity.setPvs(pvs);
            log.debug("          {} [index={}, serial={}, handle={}, type={}]",
                pvs,
                index,
                entity.getSerial(),
                Handle.forIndexAndSerial(index, entity.getSerial()),
                entity.getDtClass().getDtName()
            );
            break;
        case LEAVE_AND_DELETE:
            entities.remove(index);
            break;
        }
        return index;
    }

    private void decodeDeletionDiffs(Entities entities) {
        while (stream.readBit()) {
            int index = stream.readNumericBits(11); // max is 2^11-1, or 2047
            entities.remove(index);
        }
    }

    private void decodeProperties(Object[] state, DTClass cls, List<Integer> propIndices) {
        for (Integer i : propIndices) {
            ReceiveProp r = cls.getReceiveProps().get(i);
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
