package clarity.decoder;

import java.util.List;

import clarity.match.DTClassCollection;
import clarity.match.EntityCollection;
import clarity.model.DTClass;
import clarity.model.Entity;
import clarity.model.PVS;
import clarity.model.ReceiveProp;
import clarity.model.StringTable;

import com.google.protobuf.ByteString;

public class PacketEntitiesDecoder {

    private final DTClassCollection dtClasses;
    private final StringTable baseline;
    private final int classBits;
    private final int numEntries;
    private final boolean isDelta;
    private final EntityBitStream stream;

    public PacketEntitiesDecoder(byte[] data, int numEntries, boolean isDelta, DTClassCollection dtClasses, StringTable baseline) {
        this.dtClasses = dtClasses;
        this.baseline = baseline;
        this.stream = new EntityBitStream(data);
        this.numEntries = numEntries;
        this.isDelta = isDelta;
        this.classBits = Util.calcBitsNeededFor(dtClasses.size() - 1);
    }

    public void decodeAndApply(EntityCollection world) {
        int index = -1;
        //System.out.println("------ decoding packet entities, num " + numEntries);
        int count = 0;
        while (count < numEntries) {
            index = decodeDiff(index, world);
            count++;
        }
        if (isDelta) {
            decodeDeletionDiffs(world);
        }
    }

    private int decodeDiff(int index, EntityCollection entities) {
        index = stream.readEntityIndex(index);
        PVS pvs = stream.readEntityPVS();
        DTClass cls = null;
        Integer serial = null;
        Object[] state = null;
        List<Integer> propList = null;
        switch (pvs) {
        case ENTER:
            cls = dtClasses.forClassId(stream.readNumericBits(classBits));
            serial = stream.readNumericBits(10);
            propList = stream.readEntityPropList();
            state = decodeBaseProperties(cls);
            decodeProperties(state, cls, propList);
            entities.put(index, new Entity(index, serial, cls, state));
            break;
        case PRESERVE:
            Entity entity = entities.get(index);
            cls = entity.getDtClass();
            serial = entity.getSerial();
            propList = stream.readEntityPropList();
            decodeProperties(entity.getState(), cls, propList);
            break;
        case LEAVE:
            break;
        case LEAVE_AND_DELETE:
            entities.put(index, null);
            break;
        }
        return index;
    }

    private void decodeDeletionDiffs(EntityCollection entities) {
        while (stream.readBit()) {
            int index = stream.readNumericBits(11); // max is 2^11-1, or 2047
            entities.put(index, null);
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
