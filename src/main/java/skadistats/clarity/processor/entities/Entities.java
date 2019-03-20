package skadistats.clarity.processor.entities;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import skadistats.clarity.ClarityException;
import skadistats.clarity.LogChannel;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.state.ClientFrame;
import skadistats.clarity.model.state.CloneableEntityState;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnReset;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.processor.runner.OnInit;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.OnDTClassesComplete;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.util.Predicate;
import skadistats.clarity.util.SimpleIterator;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.common.proto.NetMessages;
import skadistats.clarity.wire.common.proto.NetworkBaseTypes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

@Provides({UsesEntities.class, OnEntityCreated.class, OnEntityUpdated.class, OnEntityDeleted.class, OnEntityEntered.class, OnEntityLeft.class, OnEntityUpdatesCompleted.class})
@UsesDTClasses
public class Entities {

    public static final String BASELINE_TABLE = "instancebaseline";

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.entities);

    private int entityCount;
    private FieldReader fieldReader;
    private int[] deletions;
    private int serverTick;
    private LinkedList<ClientFrame> clientFrames = new LinkedList<>();

    private class ClassBaseline {
        private ByteString raw;
        private CloneableEntityState state;
        private void reset() {
            raw = null;
            state = null;
        }
    }
    private ClassBaseline[] classBaselines;

    private class Baseline {
        private int dtClassId = -1;
        private CloneableEntityState state;
        private void reset() {
            dtClassId = -1;
            state = null;
        }
    }
    private Baseline[][] baselines;

    @Insert
    private EngineType engineType;
    @Insert
    private DTClasses dtClasses;

    @InsertEvent
    private Event<OnEntityCreated> evCreated;
    @InsertEvent
    private Event<OnEntityUpdated> evUpdated;
    @InsertEvent
    private Event<OnEntityDeleted> evDeleted;
    @InsertEvent
    private Event<OnEntityEntered> evEntered;
    @InsertEvent
    private Event<OnEntityLeft> evLeft;
    @InsertEvent
    private Event<OnEntityUpdatesCompleted> evUpdatesCompleted;

    @Initializer(OnEntityCreated.class)
    public void initOnEntityCreated(final EventListener<OnEntityCreated> listener) {
        listener.setInvocationPredicate(getInvocationPredicate(listener.getAnnotation().classPattern()));
    }

    @Initializer(OnEntityDeleted.class)
    public void initOnEntityDeleted(final EventListener<OnEntityDeleted> listener) {
        listener.setInvocationPredicate(getInvocationPredicate(listener.getAnnotation().classPattern()));
    }

    @Initializer(OnEntityUpdated.class)
    public void initOnEntityUpdated(final EventListener<OnEntityUpdated> listener) {
        listener.setInvocationPredicate(getInvocationPredicate(listener.getAnnotation().classPattern()));
    }

    @Initializer(OnEntityEntered.class)
    public void initOnEntityEntered(final EventListener<OnEntityEntered> listener) {
        listener.setInvocationPredicate(getInvocationPredicate(listener.getAnnotation().classPattern()));
    }

    @Initializer(OnEntityLeft.class)
    public void initOnEntityLeft(final EventListener<OnEntityLeft> listener) {
        listener.setInvocationPredicate(getInvocationPredicate(listener.getAnnotation().classPattern()));
    }

    private Predicate<Object[]> getInvocationPredicate(String classPattern) {
        if (".*".equals(classPattern)) {
            return null;
        }
        final Pattern p = Pattern.compile(classPattern);
        return new Predicate<Object[]>() {
            @Override
            public boolean apply(Object[] value) {
                Entity e = (Entity) value[0];
                return p.matcher(e.getDtClass().getDtName()).matches();
            }
        };
    }

    @OnInit
    public void onInit() {
        entityCount = 1 << engineType.getIndexBits();

        fieldReader = engineType.getNewFieldReader();

        deletions = new int[entityCount];

        baselines = new Baseline[entityCount][2];
        for (int i = 0; i < entityCount; i++) {
            baselines[i][0] = new Baseline();
            baselines[i][1] = new Baseline();
        }
    }

    @OnDTClassesComplete
    public void onDTClassesComplete() {
        classBaselines = new ClassBaseline[dtClasses.getClassCount()];
        for (int i = 0; i < classBaselines.length; i++) {
            classBaselines[i] = new ClassBaseline();
        }
    }

    @OnReset
    public void onReset(Demo.CDemoStringTables packet, ResetPhase phase) {
        if (phase == ResetPhase.CLEAR) {
            for (int i = 0; i < classBaselines.length; i++) {
                classBaselines[i].reset();
            }
            for (int i = 0; i < baselines.length; i++) {
                baselines[i][0].reset();
                baselines[i][1].reset();
            }
        }
    }

    @OnStringTableEntry(BASELINE_TABLE)
    public void onBaselineEntry(StringTable table, int index, String key, ByteString value) {
        if (classBaselines != null) {
            classBaselines[Integer.valueOf(key)].reset();
        }
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_Tick.class)
    public void onMessage(NetworkBaseTypes.CNETMsg_Tick message) {
        serverTick = message.getTick();
    }

    boolean debug = false;

    @OnMessage(NetMessages.CSVCMsg_PacketEntities.class)
    public void onPacketEntities(NetMessages.CSVCMsg_PacketEntities message) {
        if (log.isDebugEnabled()) {
            log.debug("processing packet entities: now: %6d, delta-from: %6d, update-count: %5d, baseline: %d, update-baseline: %5s", serverTick, message.getDeltaFrom(), message.getUpdatedEntries(), message.getBaseline(), message.getUpdateBaseline());
        }

        ClientFrame newFrame = new ClientFrame(engineType, serverTick);
        ClientFrame oldFrame = null;

        if (message.getIsDelta()) {
            if (serverTick == message.getDeltaFrom()) {
                throw new ClarityException("received self-referential delta update for tick %d", serverTick);
            }
            oldFrame = getClientFrame(message.getDeltaFrom(), false);
            if (oldFrame == null) {
                throw new ClarityException("missing client frame for delta update from tick %d", message.getDeltaFrom());
            }
            log.debug("performing delta update, using old frame from tick %d", oldFrame.getTick());
        } else {
            log.debug("performing full update");
        }

        if (message.getUpdateBaseline()) {
            for (Baseline[] baseline : baselines) {
                // TODO: check if this is correct
                baseline[1 - message.getBaseline()] = baseline[message.getBaseline()];
            }
        }

        BitStream stream = BitStream.createBitStream(message.getEntityData());

        int updateCount = message.getUpdatedEntries();
        int updateIndex;
        int updateType;
        int eIdx = 0;

        while (true) {
            if (updateCount > 0) {
                updateIndex = eIdx + stream.readUBitVar();
                updateCount--;
            } else {
                updateIndex = entityCount;
            }
            if (eIdx < updateIndex) {
                if (oldFrame != null) {
                    newFrame.copyFromOtherFrame(oldFrame, eIdx, updateIndex - eIdx);
                }
            }
            eIdx = updateIndex;
            if (eIdx == entityCount) {
                break;
            }

            updateType = stream.readUBitInt(2);
            switch (updateType) {
                case 2:
                    // CREATE ENTITY
                    int dtClassId = stream.readUBitInt(dtClasses.getClassBits());
                    DTClass dtClass = dtClasses.forClassId(dtClassId);
                    if (dtClass == null) {
                        throw new ClarityException("class for new entity %d is %d, but no dtClass found!.", eIdx, dtClassId);
                    }
                    int serial = stream.readUBitInt(engineType.getSerialBits());
                    if (engineType.getId() == EngineId.SOURCE2) {
                        // TODO: there is an extra VarInt encoded here for S2, figure out what it is
                        stream.readVarUInt();
                    }
                    if (oldFrame != null && oldFrame.isValid(eIdx) && oldFrame.getSerial(eIdx) == serial) {
                        // same entity, only enter
                        newFrame.updateExistingEntity(oldFrame, eIdx);
                        fieldReader.readFields(stream, dtClass, newFrame.getState(eIdx), debug);
                        newFrame.setActive(eIdx, true);
                        logModification("ENTER", newFrame, eIdx);
                        // TODO: raise evEntered
                    } else {
                        // new entity
                        CloneableEntityState newState = getBaseline(eIdx, dtClassId, message.getBaseline()).clone();
                        fieldReader.readFields(stream, dtClass, newState, debug);
                        newFrame.createNewEntity(eIdx, dtClass, serial, newState);
                        logModification("CREATE", newFrame, eIdx);
                        // TODO: raise evCreated
                        // TODO: raise evEntered
                    }
                    if (message.getUpdateBaseline()) {
                        // TODO: properly update baseline
                        //throw new UnsupportedOperationException("update baseline");
                    }
                    break;

                case 0:
                    // UPDATE ENTITY
                    checkOldFrameValid("update", oldFrame, eIdx);
                    newFrame.updateExistingEntity(oldFrame, eIdx);
                    logModification("UPDATE", newFrame, eIdx);
                    fieldReader.readFields(stream, newFrame.getDtClass(eIdx), newFrame.getState(eIdx), debug);
                    // TODO: raise evUpdated
                    break;

                case 1:
                    // LEAVE ENTITY
                    checkOldFrameValid("leave", oldFrame, eIdx);
                    newFrame.copyFromOtherFrame(oldFrame, eIdx, 1);
                    newFrame.setActive(eIdx, false);
                    // TODO: raise evLeft
                    break;

                case 3:
                    // DELETE ENTITY
                    checkOldFrameValid("delete", oldFrame, eIdx);
                    logModification("DELETE", oldFrame, eIdx);
                    // TODO: raise evLeft
                    // TODO: raise evDeleted
                    break;
            }

            eIdx++;
        }

        if (false && engineType.handleDeletions() && message.getIsDelta()) {
            int n = fieldReader.readDeletions(stream, engineType.getIndexBits(), deletions);
            for (int i = 0; i < n; i++) {
                eIdx = deletions[i];
                if (newFrame.isValid(eIdx)) {
                    log.debug("entity at index %d was ACTUALLY found when ordered to delete, tell the press!", eIdx);
                    if (newFrame.isActive(eIdx)) {
                        newFrame.setActive(eIdx, false);
                        // TODO: raise evLeft
                    }
                    // TODO: raise evDeleted
                    newFrame.deleteEntity(eIdx);
                } else {
                    log.debug("entity at index %d was not found when ordered to delete.", eIdx);
                }
            }
        }


        log.debug("update finished for tick %d", newFrame.getTick());

        Iterator<ClientFrame> iter = clientFrames.iterator();
        while(iter.hasNext()) {
            ClientFrame frame = iter.next();
            if (frame.getTick() >= message.getDeltaFrom()) {
                break;
            }
            log.debug("deleting client frame for tick %d", frame.getTick());
            iter.remove();
        }

        clientFrames.add(newFrame);

        evUpdatesCompleted.raise();
    }

    private void checkOldFrameValid(String which, ClientFrame oldFrame, int eIdx) {
        if (oldFrame == null) {
            throw new ClarityException("no old frame on entity %s", which);
        }
        if (!oldFrame.isValid(eIdx)) {
            throw new ClarityException("entity at index %d was not found for %s", eIdx, which);
        }
    }

    private void logModification(String which, ClientFrame frame, int eIdx) {
        if (!log.isDebugEnabled()) return;
        log.debug("\t%6s: index: %4d, serial: %03x, class: %s",
                which,
                eIdx,
                frame.getSerial(eIdx),
                frame.getDtClass(eIdx).getDtName()
        );
    }

    private ClientFrame getClientFrame(int tick, boolean exact) {
        Iterator<ClientFrame> iter = clientFrames.iterator();
        ClientFrame lastFrame = clientFrames.peekFirst();
        while (iter.hasNext()) {
            ClientFrame frame = iter.next();
            if (frame.getTick() >= tick) {
                if (frame.getTick() == tick) {
                    return frame;
                }
                if (exact) {
                    return null;
                }
                return lastFrame;
            }
            lastFrame = frame;
        }
        if (exact) {
            return null;
        }
        return lastFrame;
    }

    private CloneableEntityState getBaseline(int entityIdx, int clsId, int baseline) {
        Baseline b = baselines[entityIdx][baseline];
        if (b.dtClassId == clsId && b.state != null) {
            return b.state;
        }
        ClassBaseline be = classBaselines[clsId];
        if (be != null && be.state != null) {
            return be.state;
        }
        DTClass cls = dtClasses.forClassId(clsId);
        if (cls == null) {
            throw new ClarityException("DTClass for id %d not found.", clsId);
        }
        if (be == null) {
            throw new ClarityException("Baseline for class %s (%d) not found.", cls.getDtName(), clsId);
        }
        be.state = cls.getEmptyState();
        if (be.raw != null) {
            BitStream stream = BitStream.createBitStream(be.raw);
            fieldReader.readFields(stream, cls, be.state, false);
        }
        return be.state;
    }

    private Map<Integer, Entity> entityMap = new HashMap<>();

    public Entity getByIndex(int index) {
        ClientFrame currentFrame = clientFrames.getLast();
        if (currentFrame == null || !currentFrame.isValid(index)) return null;
        int handle = currentFrame.getHandle(index);
        return entityMap.computeIfAbsent(handle, h -> new Entity(index, clientFrames::getLast));
    }

    public Entity getByHandle(int handle) {
        Entity e = getByIndex(engineType.indexForHandle(handle));
        return e == null || e.getSerial() != engineType.serialForHandle(handle) ? null : e;
    }

    public Iterator<Entity> getAllByPredicate(final Predicate<Entity> predicate) {
        return new SimpleIterator<Entity>() {
            int i = -1;

            @Override
            public Entity readNext() {
                while (++i < entityCount) {
                    Entity e = getByIndex(i);
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
