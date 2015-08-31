package skadistats.clarity.processor.entities;

import com.google.protobuf.ByteString;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.event.*;
import skadistats.clarity.model.*;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnReset;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.util.Predicate;
import skadistats.clarity.util.SimpleIterator;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.common.proto.NetMessages;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Provides({ UsesEntities.class, OnEntityCreated.class, OnEntityUpdated.class, OnEntityDeleted.class })
@UsesDTClasses
public class Entities {

    private final Entity[] entities = new Entity[1 << Handle.INDEX_BITS];
    private final Map<Integer, BaselineEntry> baselineEntries = new HashMap<>();
    private FieldReader fieldReader;
    private int serialBitCount;

    private Event<OnEntityCreated> evCreated;
    private Event<OnEntityUpdated> evUpdated;
    private Event<OnEntityDeleted> evDeleted;

    private class BaselineEntry {
        private ByteString rawBaseline;
        private Object[] baseline;
        public BaselineEntry(ByteString rawBaseline) {
            this.rawBaseline = rawBaseline;
            this.baseline = null;
        }
    }

    @Initializer(UsesEntities.class)
    public void initUsesEntities(final Context ctx, final UsagePoint<UsesEntities> usagePoint) {
        initFieldReader(ctx);
    }

    @Initializer(OnEntityCreated.class)
    public void initOnEntityCreated(final Context ctx, final EventListener<OnEntityCreated> eventListener) {
        initFieldReader(ctx);
        evCreated = ctx.createEvent(OnEntityCreated.class, Entity.class);
    }

    @Initializer(OnEntityUpdated.class)
    public void initOnEntityUpdated(final Context ctx, final EventListener<OnEntityUpdated> eventListener) {
        initFieldReader(ctx);
        evUpdated = ctx.createEvent(OnEntityUpdated.class, Entity.class, int[].class, int.class);
    }

    @Initializer(OnEntityDeleted.class)
    public void initOnEntityDeleted(final Context ctx, final EventListener<OnEntityDeleted> eventListener) {
        initFieldReader(ctx);
        evDeleted = ctx.createEvent(OnEntityDeleted.class, Entity.class);
    }

    private void initFieldReader(Context ctx) {
        if (fieldReader == null) {
            fieldReader = ctx.getEngineType().getNewFieldReader();
            serialBitCount = ctx.getEngineType().getSerialBitCount();
        }
    }

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
    public void onBaselineEntry(Context ctx, StringTable table, int index, String key, ByteString value) {
        baselineEntries.put(Integer.valueOf(key), new BaselineEntry(value));
    }

    @OnMessage(NetMessages.CSVCMsg_PacketEntities.class)
    public void onPacketEntities(Context ctx, NetMessages.CSVCMsg_PacketEntities message) {
        BitStream stream = new BitStream(message.getEntityData());
        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
        int updateCount = message.getUpdatedEntries();
        int entityIndex = -1;

        int pvs;
        DTClass cls;
        int serial;
        Object[] state;
        Entity entity;

        while (updateCount-- != 0) {
            entityIndex += stream.readUBitVar() + 1;
            pvs = stream.readUBitInt(2);
            if ((pvs & 1) == 0) {
                if ((pvs & 2) != 0) {
                    cls = dtClasses.forClassId(stream.readUBitInt(dtClasses.getClassBits()));
                    serial = stream.readUBitInt(serialBitCount);
                    state = Util.clone(getBaseline(dtClasses, cls.getClassId()));
                    fieldReader.readFields(stream, cls, state, false);
                    entity = new Entity(entityIndex, serial, cls, PVS.values()[pvs], state);
                    entities[entityIndex] = entity;
                    if (evCreated != null) {
                        evCreated.raise(entity);
                    }
                } else {
                    entity = entities[entityIndex];
                    cls = entity.getDtClass();
                    entity.setPvs(PVS.values()[pvs]);
                    state = entity.getState();
                    fieldReader.readFields(stream, cls, state, false);
                    if (evUpdated != null) {
                        //evUpdated.raise(entity, indices, cIndices);
                    }
                }
            } else if ((pvs & 2) != 0) {
                entity = entities[entityIndex];
                entities[entityIndex] = null;
                if (evDeleted != null) {
                    evDeleted.raise(entity);
                }
            }
        }
        if (message.getIsDelta()) {
            while (stream.readBitFlag()) {
                entityIndex = stream.readUBitInt(11); // max is 2^11-1, or 2047
                if (evDeleted != null) {
                    evDeleted.raise(entities[entityIndex]);
                }
                entities[entityIndex] = null;
            }
        }
    }

    private Object[] getBaseline(DTClasses dtClasses, int clsId) {
        BaselineEntry be = baselineEntries.get(clsId);
        if (be.baseline == null) {
            DTClass cls = dtClasses.forClassId(clsId);
            BitStream stream = new BitStream(be.rawBaseline);
            be.baseline = cls.getEmptyStateArray();
            fieldReader.readFields(stream, cls, be.baseline, false);
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

    public Iterator<Entity> getAllByPredicate(final Predicate<Entity> predicate) {
        return new SimpleIterator<Entity>() {
            int i = -1;
            @Override
            public Entity readNext() {
                while(++i < entities.length) {
                    Entity e = entities[i];
                    if (e != null && predicate.apply(e)) {
                        return e;
                    }
                }
                return null;
            }
        };
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
