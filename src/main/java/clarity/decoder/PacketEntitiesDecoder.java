package clarity.decoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;

import clarity.model.DTClassCollection;
import clarity.model.Entity;
import clarity.model.EntityCollection;
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

    public List<Pair<PVS, Entity>> decode(EntityCollection world) {
        List<Pair<PVS, Entity>> patch = new LinkedList<Pair<PVS, Entity>>();
        int index = -1;
        // System.out.println("------ decoding packet entities, num " +
        // message.getUpdatedEntries());
        while (patch.size() < numEntries) {
            Pair<PVS, Entity> diff = decodeDiff(index, world);
            index = diff.getValue1().getIndex();
            patch.add(diff);
        }
        if (isDelta) {
            patch.addAll(decodeDeletionDiffs());
        }
        return patch;
    }

    private Pair<PVS, Entity> decodeDiff(int index, EntityCollection entities) {
        index = stream.readEntityIndex(index);
        PVS pvs = stream.readEntityPVS();
        Integer cls = null;
        Integer serial = null;
        Map<Integer, Object> state = null;
        List<Integer> propList = null;
        // System.out.println(pvs + " at " + index);
        switch (pvs) {
        case ENTER:
            cls = stream.readNumericBits(classBits);
            serial = stream.readNumericBits(10);
            propList = stream.readEntityPropList();
            // System.out.println("class: " + cls + ", serial: " + serial +
            // ", props: " + propList);
            state = decodeBaseProperties(cls);
            state.putAll(decodeProperties(cls, propList));
            break;
        case PRESERVE:
            Entity entity = entities.get(index);
            cls = entity.getCls();
            serial = entity.getSerial();
            propList = stream.readEntityPropList();
            state = decodeProperties(cls, propList);
            break;
        case LEAVE:
        case LEAVE_AND_DELETE:
            state = new HashMap<Integer, Object>();
            break;
        }
        return new Pair<PVS, Entity>(pvs, new Entity(index, serial, cls, state));
    }

    private List<Pair<PVS, Entity>> decodeDeletionDiffs() {
        List<Pair<PVS, Entity>> deletions = new ArrayList<Pair<PVS, Entity>>();

        while (stream.readBit()) {
            int index = stream.readNumericBits(11); // max is 2^11-1, or 2047
            deletions.add(new Pair<PVS, Entity>(PVS.LEAVE_AND_DELETE, new Entity(index, null, null, null)));
        }
        return deletions;
    }

    private Map<Integer, Object> decodeProperties(int cls, List<Integer> propIndices) {
        Map<Integer, Object> decodedProps = new HashMap<Integer, Object>();
        for (Integer i : propIndices) {
            ReceiveProp r = dtClasses.forClassId(cls).getReceiveProps().get(i);
            // System.out.print(c + ": " + r);
            Object dec = r.getType().getDecoder().decode(stream, r);
            decodedProps.put(i, dec);
            // System.out.println(" := " + dec.toString());
        }
        return decodedProps;
    }

    private Map<Integer, Object> decodeBaseProperties(int cls) {
        Map<Integer, Object> decodedProps = new HashMap<Integer, Object>();
        ByteString s = baseline.getByName(String.valueOf(cls));
        BaseInstanceDecoder.decode(
            s.toByteArray(),
            dtClasses.forClassId(cls).getReceiveProps()
            );
        return decodedProps;
    }

}
