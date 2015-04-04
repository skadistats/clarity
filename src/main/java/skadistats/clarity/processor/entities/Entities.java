package skadistats.clarity.processor.entities;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.google.protobuf.ByteString;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.*;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnReset;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.wire.proto.Demo;
import skadistats.clarity.wire.proto.Netmessages;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Provides({UsesEntities.class})
@UsesDTClasses
public class Entities {

    public static final int MAX_PROPERTIES = 0x3fff;

    private final Entity[] entities = new Entity[1 << Handle.INDEX_BITS];
    private final Map<Integer, BaselineEntry> baselineEntries = new HashMap<>();

    private class BaselineEntry {
        private ByteString rawBaseline;
        private Object[] baseline;
        public BaselineEntry(ByteString rawBaseline) {
            this.rawBaseline = rawBaseline;
            this.baseline = null;
        }
    }

    private final int[] indices = new int[MAX_PROPERTIES];

    @OnReset
    public void onReset(Context ctx, Demo.CDemoFullPacket packet, ResetPhase phase) {
        if (phase == ResetPhase.CLEAR) {
            baselineEntries.clear();
            for (int entityIndex = 0; entityIndex < entities.length; entityIndex++) {
                entities[entityIndex] = null;
            }
        }
    }

    @OnStringTableEntry("instancebaseline")
    public void onBaseline(Context ctx, StringTable table, StringTableEntry oldEntry, StringTableEntry newEntry) {
        int key = Integer.valueOf(newEntry.getKey());
        baselineEntries.put(key, new BaselineEntry(newEntry.getValue()));
    }

    @OnMessage(Netmessages.CSVCMsg_PacketEntities.class)
    public void onPacketEntities(Context ctx, Netmessages.CSVCMsg_PacketEntities message) {
        BitStream stream = new BitStream(message.getEntityData());
        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
        int updateCount = message.getUpdatedEntries();
        int entityIndex = -1;

        int pvs;
        DTClass cls;
        int serial;
        Object[] base;
        Object[] state;
        Entity entity;
        int cIndices;
        ReceiveProp[] receiveProps;

        while (updateCount-- != 0) {
            entityIndex = stream.readEntityIndex(entityIndex);
            pvs = stream.readNumericBits(2);
            if ((pvs & 1) == 0) {
                if ((pvs & 2) != 0) {
                    cls = dtClasses.forClassId(stream.readNumericBits(dtClasses.getClassBits()));
                    serial = stream.readNumericBits(10);
                    base = getBaseline(ctx, dtClasses, cls.getClassId());
                    state = Arrays.copyOf(base, base.length);
                    cIndices = stream.readEntityPropList(indices);
                    receiveProps = cls.getReceiveProps();
                    for (int ci = 0; ci < cIndices; ci++) {
                        int o = indices[ci];
                        state[o] = receiveProps[o].decode(stream);
                    }
                    entities[entityIndex] = new Entity(entityIndex, serial, cls, PVS.values()[pvs], state);
                } else {
                    entity = entities[entityIndex];
                    cls = entity.getDtClass();
                    entity.setPvs(PVS.values()[pvs]);
                    state = entity.getState();
                    cIndices = stream.readEntityPropList(indices);
                    receiveProps = cls.getReceiveProps();
                    for (int ci = 0; ci < cIndices; ci++) {
                        int o = indices[ci];
                        state[o] = receiveProps[o].decode(stream);
                    }
                }
            } else if ((pvs & 2) != 0) {
                entities[entityIndex] = null;
            }
        }
        if (message.getIsDelta()) {
            while (stream.readNumericBits(1) == 1) {
                entityIndex = stream.readNumericBits(11); // max is 2^11-1, or 2047
                entities[entityIndex] = null;
            }
        }
    }

    private Object[] getBaseline(Context ctx, DTClasses dtClasses, int clsId) {
        BaselineEntry be = baselineEntries.get(clsId);
        if (be == null) {
            StringTable st = ctx.getProcessor(StringTables.class).forName("instancebaseline");
            ByteString valueByName = st.getValueByName(String.valueOf(clsId));
            if (valueByName == null) {
                System.out.println("no value for " + clsId + ", " + dtClasses.forClassId(clsId).getDtName());
            }
            be = new BaselineEntry(valueByName);
            baselineEntries.put(clsId, be);
        }
        if (be.baseline == null) {
            DTClass cls = dtClasses.forClassId(clsId);
            ReceiveProp[] receiveProps = cls.getReceiveProps();
            BitStream stream = new BitStream(be.rawBaseline);
            be.baseline = new Object[receiveProps.length];
            int cIndices = stream.readEntityPropList(indices);
            for (int ci = 0; ci < cIndices; ci++) {
                int o = indices[ci];
                be.baseline[o] = receiveProps[o].decode(stream);
            }
        }
        return be.baseline;
    }

    public Entity getByIndex(int index) {
        return entities[index];
    }

    public Entity getByHandle(int handle) {
        Entity e = entities[Handle.indexForHandle(handle)];
        return e == null || e.getSerial() != Handle.serialForHandle(handle) ? null : e;
    }

    public Iterator<Entity> getAllByPredicate(Predicate<Entity> predicate) {
        return Iterators.filter(
            Iterators.forArray(entities),
            Predicates.and(
                Predicates.notNull(),
                predicate
            ));
    }

    public Entity getByPredicate(Predicate<Entity> predicate) {
        Iterator<Entity> iter = getAllByPredicate(predicate);
        return iter.hasNext() ? iter.next() : null;
    }

    public Iterator<Entity> getAllByDtName(final String dtClassName) {
        return getAllByPredicate(
            new Predicate<Entity>() {
                @Override
                public boolean apply(Entity e) {
                    return dtClassName.equals(e.getDtClass().getDtName());
                }
            });
    }

    public Entity getByDtName(final String dtClassName) {
        Iterator<Entity> iter = getAllByDtName(dtClassName);
        return iter.hasNext() ? iter.next() : null;
    }

}
