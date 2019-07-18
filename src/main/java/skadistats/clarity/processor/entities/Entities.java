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
import skadistats.clarity.model.EntityStateSupplier;
import skadistats.clarity.model.FieldPath;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Provides({UsesEntities.class, OnEntityCreated.class, OnEntityUpdated.class, OnEntityDeleted.class, OnEntityEntered.class, OnEntityLeft.class, OnEntityUpdatesCompleted.class})
@UsesDTClasses
public class Entities {

    public static final String BASELINE_TABLE = "instancebaseline";

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.entities);

    private int entityCount;
    private FieldReader<DTClass> fieldReader;
    private int[] deletions;
    private int serverTick;
    private LinkedList<ClientFrame> clientFrames = new LinkedList<>();

    private Map<Integer, ByteString> rawBaselines = new HashMap<>();
    private class Baseline {
        private int dtClassId = -1;
        private CloneableEntityState state;
        private void reset() {
            state = null;
        }
        private void copyFrom(Baseline other) {
            this.dtClassId = other.dtClassId;
            this.state = other.state;
        }
    }
    private Baseline[] classBaselines;
    private Baseline[][] entityBaselines;

    private final FieldPath[] updatedFieldPaths = new FieldPath[FieldReader.MAX_PROPERTIES];

    private ClientFrame currentFrame;
    private ClientFrame lastFrame;
    private ClientFrame activeFrame;
    private ClientFrame deltaFrame;

    private List<Runnable> lastFrameEvents = new ArrayList<>();
    private List<Runnable> currentFrameEvents = new ArrayList<>();


    private boolean resetInProgress;
    private ClientFrame resetFrame;

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

        entityBaselines = new Baseline[entityCount][2];
        for (int i = 0; i < entityCount; i++) {
            entityBaselines[i][0] = new Baseline();
            entityBaselines[i][1] = new Baseline();
        }
    }

    @OnDTClassesComplete
    public void onDTClassesComplete() {
        classBaselines = new Baseline[dtClasses.getClassCount()];
        for (int i = 0; i < classBaselines.length; i++) {
            classBaselines[i] = new Baseline();
            classBaselines[i].dtClassId = i;
        }
    }

    @OnReset
    public void onReset(Demo.CDemoStringTables packet, ResetPhase phase) {
        switch (phase) {
            case START:
                resetInProgress = true;
                resetFrame = currentFrame;
                break;

            case CLEAR:
                clientFrames.clear();
                for (int i = 0; i < classBaselines.length; i++) {
                    classBaselines[i].reset();
                }
                for (int i = 0; i < entityBaselines.length; i++) {
                    entityBaselines[i][0].reset();
                    entityBaselines[i][1].reset();
                }
                break;

            case COMPLETE:
                lastFrame = resetFrame;
                resetInProgress = false;
                resetFrame = null;

                //updateEventDebug = true;

                for (int i = 0; i < entityCount; i++) {
                    boolean serialDiffers = lastFrame.getSerial(i) != currentFrame.getSerial(i);
                    if (serialDiffers || (lastFrame.isActive(i) && !currentFrame.isActive(i))) {
                        emitLeftEvent(i);
                    }
                    if (serialDiffers || (lastFrame.isValid(i) && !currentFrame.isValid(i))) {
                        emitDeletedEvent(i);
                    }
                }
                activeFrame = lastFrame;
                lastFrameEvents.forEach(Runnable::run);
                lastFrameEvents.clear();

                for (int i = 0; i < entityCount; i++) {
                    if (currentFrame.isValid(i)) {
                        emitCreatedEvent(i);
                    }
                    if (!lastFrame.isActive(i) && currentFrame.isActive(i)) {
                        emitEnteredEvent(i);
                    }
                }
                activeFrame = currentFrame;
                currentFrameEvents.forEach(Runnable::run);
                currentFrameEvents.clear();

                //updateEventDebug = false;

                evUpdatesCompleted.raise();
                break;
        }
    }

    @OnStringTableEntry(BASELINE_TABLE)
    public void onBaselineEntry(StringTable table, int index, String key, ByteString value) {
        Integer dtClassId = Integer.valueOf(key);
        rawBaselines.put(dtClassId, value);
        if (classBaselines != null) {
            classBaselines[dtClassId].reset();
        }
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_Tick.class)
    public void onMessage(NetworkBaseTypes.CNETMsg_Tick message) {
        serverTick = message.getTick();
    }

    boolean debug = false;
    boolean updateEventDebug = false;

    void debugUpdateEvent(String which, int eIdx) {
        if (!updateEventDebug) return;
        log.info("\t%6s: index: %4d, serial: %03x, handle: %7d, class: %s",
                which,
                eIdx,
                activeFrame.getSerial(eIdx),
                activeFrame.getHandle(eIdx),
                activeFrame.getDtClass(eIdx).getDtName()
        );
    }

    @OnMessage(NetMessages.CSVCMsg_PacketEntities.class)
    public void onPacketEntities(NetMessages.CSVCMsg_PacketEntities message) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "processing packet entities: now: %6d, delta-from: %6d, update-count: %5d, baseline: %d, update-baseline: %5s",
                    serverTick,
                    message.getDeltaFrom(),
                    message.getUpdatedEntries(),
                    message.getBaseline(),
                    message.getUpdateBaseline()
            );
        }

        lastFrame = currentFrame;
        currentFrame = new ClientFrame(engineType, serverTick);

        deltaFrame = null;
        if (message.getIsDelta()) {
            if (serverTick == message.getDeltaFrom()) {
                throw new ClarityException("received self-referential delta update for tick %d", serverTick);
            }
            deltaFrame = getClientFrame(message.getDeltaFrom(), false);
            if (deltaFrame == null) {
                throw new ClarityException("missing client frame for delta update from tick %d", message.getDeltaFrom());
            }
            log.debug("performing delta update, using previous frame from tick %d", deltaFrame.getTick());
        } else {
            log.debug("performing full update");
        }

        if (message.getUpdateBaseline()) {
            int iFrom = message.getBaseline();
            int iTo = 1 - message.getBaseline();
            for (Baseline[] baseline : entityBaselines) {
                baseline[iTo].copyFrom(baseline[iFrom]);
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
                if (deltaFrame != null) {
                    currentFrame.copyFromOtherFrame(deltaFrame, eIdx, updateIndex - eIdx);
                }
            }
            eIdx = updateIndex;
            if (eIdx == entityCount) {
                break;
            }

            updateType = stream.readUBitInt(2);
            switch (updateType) {
                case 2:
                    processEntityCreate(eIdx, message, stream);
                    break;
                case 0:
                    processEntityUpdate(eIdx, stream);
                    break;
                case 1:
                    processEntityLeave(eIdx);
                    break;
                case 3:
                    processEntityDelete(eIdx);
                    break;
            }

            eIdx++;
        }

        processDeletions(message, stream);

        log.debug("update finished for tick %d", currentFrame.getTick());

        removeObsoleteClientFrames(message.getDeltaFrom());
        clientFrames.add(currentFrame);

        if (!resetInProgress) {
            activeFrame = lastFrame;
            lastFrameEvents.forEach(Runnable::run);
            lastFrameEvents.clear();

            activeFrame = currentFrame;
            currentFrameEvents.forEach(Runnable::run);
            currentFrameEvents.clear();

            evUpdatesCompleted.raise();
        }
    }

    private void processEntityCreate(int eIdx, NetMessages.CSVCMsg_PacketEntities message, BitStream stream) {
        CloneableEntityState newState;
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
        boolean isCreate = true;
        Set<FieldPath> changedFieldPaths = null;
        Consumer<FieldPath> changedFieldPathConsumer = null;
        if (deltaFrame != null && deltaFrame.isValid(eIdx)) {
            if (deltaFrame.getSerial(eIdx) == serial) {
                // same entity, only enter
                isCreate = false;
                changedFieldPaths = new TreeSet<>();
                changedFieldPathConsumer = changedFieldPaths::add;
            } else {
                // recreate
                logModification("DELETE", deltaFrame, eIdx);
                if (lastFrame.getSerial(eIdx) != serial) {
                    emitLeftEvent(eIdx);
                    emitDeletedEvent(eIdx);
                }
            }
        }
        if (isCreate) {
            Baseline baseline = getBaseline(dtClassId, message.getBaseline(), eIdx, message.getIsDelta());
            newState = baseline.state.clone();
        } else {
            newState = deltaFrame.getState(eIdx).clone();
        }
        fieldReader.readFields(stream, dtClass, newState, changedFieldPathConsumer, debug);
        if (isCreate) {
            currentFrame.createNewEntity(eIdx, dtClass, serial, null, newState);
            logModification("CREATE", currentFrame, eIdx);
            emitCreatedEvent(eIdx);
            emitEnteredEvent(eIdx);
        } else {
            currentFrame.updateExistingEntity(deltaFrame, eIdx, changedFieldPaths, newState);
            currentFrame.setActive(eIdx, true);
            logModification("ENTER", currentFrame, eIdx);
            emitEnteredEvent(eIdx);
        }
        if (message.getUpdateBaseline()) {
            Baseline baseline = entityBaselines[eIdx][1 - message.getBaseline()];
            baseline.dtClassId = dtClassId;
            baseline.state = newState.clone();
        }
    }

    private void processEntityUpdate(int eIdx, BitStream stream) {
        Set<FieldPath> changedFieldPaths;
        CloneableEntityState newState;
        checkDeltaFrameValid("update", eIdx);
        changedFieldPaths = new TreeSet<>();
        newState = deltaFrame.getState(eIdx).clone();
        fieldReader.readFields(stream, deltaFrame.getDtClass(eIdx), newState, changedFieldPaths::add, debug);
        currentFrame.updateExistingEntity(deltaFrame, eIdx, changedFieldPaths, newState);
        logModification("UPDATE", currentFrame, eIdx);
        emitUpdatedEvent(eIdx);
    }

    private void processEntityLeave(int eIdx) {
        checkDeltaFrameValid("leave", eIdx);
        currentFrame.copyFromOtherFrame(deltaFrame, eIdx, 1);
        currentFrame.setActive(eIdx, false);
        logModification("LEAVE", currentFrame, eIdx);
        emitLeftEvent(eIdx);
    }

    private void processEntityDelete(int eIdx) {
        checkDeltaFrameValid("delete", eIdx);
        logModification("DELETE", deltaFrame, eIdx);
        emitLeftEvent(eIdx);
        emitDeletedEvent(eIdx);
    }

    private void processDeletions(NetMessages.CSVCMsg_PacketEntities message, BitStream stream) {
        int eIdx;
        if (engineType.handleDeletions() && message.getIsDelta()) {
            int n = fieldReader.readDeletions(stream, engineType.getIndexBits(), deletions);
            for (int i = 0; i < n; i++) {
                eIdx = deletions[i];
                if (currentFrame.isValid(eIdx)) {
                    log.debug("entity at index %d was ACTUALLY found when ordered to delete, tell the press!", eIdx);
                    if (currentFrame.isActive(eIdx)) {
                        currentFrame.setActive(eIdx, false);
                        emitLeftEvent(eIdx);
                    }
                    emitDeletedEvent(eIdx);
                    currentFrame.deleteEntity(eIdx);
                } else {
                    log.debug("entity at index %d was not found when ordered to delete.", eIdx);
                }
            }
        }
    }

    private void removeObsoleteClientFrames(int deltaFrom) {
        Iterator<ClientFrame> iter = clientFrames.iterator();
        while(iter.hasNext()) {
            ClientFrame frame = iter.next();
            if (frame.getTick() >= deltaFrom) {
                break;
            }
            log.debug("deleting client frame for tick %d", frame.getTick());
            iter.remove();
        }
    }

    private void emitCreatedEvent(int i) {
        if (lastFrame != null && lastFrame.isValid(i) && lastFrame.getSerial(i) == currentFrame.getSerial(i)) {
            if (resetInProgress || !evUpdated.isListenedTo()) return;
            // double create event -> emulate an update
            currentFrame.setChangedFieldPaths(
                    i,
                    new TreeSet<>(currentFrame.getDtClass(i).collectFieldPaths(currentFrame.getState(i)))
            );
            emitUpdatedEvent(i);
        } else {
            if (resetInProgress || !evCreated.isListenedTo()) return;
            currentFrameEvents.add(() -> {
                debugUpdateEvent("CREATE", i);
                evCreated.raise(getByIndex(i));
            });
        }
    }

    private void emitEnteredEvent(int i) {
        if (resetInProgress || !evEntered.isListenedTo()) return;
        if (lastFrame != null && lastFrame.isValid(i) && lastFrame.isActive(i)) return;
        currentFrameEvents.add(() -> {
            debugUpdateEvent("ENTER", i);
            evEntered.raise(getByIndex(i));
        });
    }

    private void emitUpdatedEvent(int i) {
        if (resetInProgress || !evUpdated.isListenedTo()) return;
        currentFrameEvents.add(() -> {
            Set<FieldPath> processedFieldPaths = new TreeSet<>();
            for (ClientFrame cf : clientFrames.subList(1, this.clientFrames.size())) {
                Set<FieldPath> changedFieldPaths = cf.getChangedFieldPaths(i);
                if (changedFieldPaths != null) {
                    processedFieldPaths.addAll(changedFieldPaths);
                }
            }

            DTClass cls = currentFrame.getDtClass(i);
            int n = 0;
            for (FieldPath changedFieldPath : processedFieldPaths) {
                Object v1 = cls.getValueForFieldPath(changedFieldPath, lastFrame.getState(i));
                Object v2 = cls.getValueForFieldPath(changedFieldPath, currentFrame.getState(i));
                if ((v1 == null) ^ (v2 == null)) {
                    updatedFieldPaths[n++] = changedFieldPath;
                } else if (v1 != null && !v1.equals(v2)) {
                    updatedFieldPaths[n++] = changedFieldPath;
                }
            }

            //Arrays.sort(updatedFieldPaths, 0, n);
            if (n > 0) {
                debugUpdateEvent("UPDATE", i);
                evUpdated.raise(getByIndex(i), updatedFieldPaths, n);
            }
        });
    }

    private void emitLeftEvent(int i) {
        if (resetInProgress || !evLeft.isListenedTo()) return;
        if (lastFrame == null || !lastFrame.isValid(i) || !lastFrame.isActive(i)) return;
        lastFrameEvents.add(() -> {
            debugUpdateEvent("LEAVE", i);
            evLeft.raise(getByIndex(i));
        });
    }

    private void emitDeletedEvent(int i) {
        if (resetInProgress || !evDeleted.isListenedTo()) return;
        if (lastFrame == null || !lastFrame.isValid(i)) return;
        lastFrameEvents.add(() -> {
            debugUpdateEvent("DELETE", i);
            evDeleted.raise(getByIndex(i));
        });
    }

    private void checkDeltaFrameValid(String which, int eIdx) {
        if (deltaFrame == null) {
            throw new ClarityException("no delta frame on entity %s", which);
        }
        if (!deltaFrame.isValid(eIdx)) {
            throw new ClarityException("entity at index %d was not found in delta frame for %s", eIdx, which);
        }
    }

    private void logModification(String which, ClientFrame frame, int eIdx) {
        if (!log.isDebugEnabled()) return;
        log.debug("\t%6s: index: %4d, serial: %03x, handle: %7d, class: %s",
                which,
                eIdx,
                frame.getSerial(eIdx),
                frame.getHandle(eIdx),
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

    private Baseline getBaseline(int clsId, int baseline, int entityIdx, boolean delta) {
        Baseline b;
        if (delta) {
            b = entityBaselines[entityIdx][baseline];
            if (b.dtClassId == clsId && b.state != null) {
                return b;
            }
        }
        b = classBaselines[clsId];
        if (b.state != null) {
            return b;
        }
        DTClass cls = dtClasses.forClassId(clsId);
        if (cls == null) {
            throw new ClarityException("DTClass for id %d not found.", clsId);
        }
        b.state = cls.getEmptyState();
        ByteString raw = rawBaselines.get(clsId);
        if (raw == null) {
            throw new ClarityException("Baseline for class %s (%d) not found.", cls.getDtName(), clsId);
        }
        if (raw.size() > 0) {
            BitStream stream = BitStream.createBitStream(raw);
            fieldReader.readFields(stream, cls, b.state, null, false);
        }
        return b;
    }

    private Map<Integer, Entity> entityMap = new HashMap<>();
    private Map<Integer, EntityStateSupplier> supplierMap = new HashMap<>();


    public Entity getByIndex(int index) {
        if (activeFrame == null || !activeFrame.isValid(index)) return null;
        int handle = currentFrame.getHandle(index);
        return entityMap.computeIfAbsent(handle, h -> new Entity(
            supplierMap.computeIfAbsent(index, i -> new EntityStateSupplier() {
                private <T> T get(BiFunction<ClientFrame, Integer, T> getter, T defaultValue) {
                    if (activeFrame == null || !activeFrame.isValid(i)) return defaultValue;
                    return getter.apply(activeFrame, index);
                }
                @Override
                public int getIndex() {
                    return index;
                }
                @Override
                public DTClass getDTClass() {
                    return get(ClientFrame::getDtClass, null);
                }
                @Override
                public int getSerial() {
                    return get(ClientFrame::getSerial, 0);
                }
                @Override
                public boolean isActive() {
                    return get(ClientFrame::isActive, false);
                }
                @Override
                public int getHandle() {
                    // TODO: maybe return empty handle?
                    return get(ClientFrame::getHandle, 0);
                }
                @Override
                public CloneableEntityState getState() {
                    return get(ClientFrame::getState, null);
                }
            })
        ));
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
