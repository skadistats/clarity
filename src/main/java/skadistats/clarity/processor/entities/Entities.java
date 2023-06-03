package skadistats.clarity.processor.entities;

import com.google.protobuf.ByteString;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import org.slf4j.Logger;
import skadistats.clarity.ClarityException;
import skadistats.clarity.LogChannel;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.io.FieldChanges;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.state.BaselineRegistry;
import skadistats.clarity.model.state.ClientFrame;
import skadistats.clarity.model.state.EntityRegistry;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnReset;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.processor.runner.OnInit;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.OnStringTableCreated;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.util.Predicate;
import skadistats.clarity.util.SimpleIterator;
import skadistats.clarity.util.StateDifferenceEvaluator;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages;
import skadistats.clarity.wire.shared.common.proto.CommonNetworkBaseTypes;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

@Provides({
        UsesEntities.class,
        OnEntityCreated.class,
        OnEntityUpdated.class,
        OnEntityPropertyCountChanged.class,
        OnEntityDeleted.class,
        OnEntityEntered.class,
        OnEntityLeft.class,
        OnEntityUpdatesCompleted.class
})
@UsesDTClasses
public class Entities {

    public static final String BASELINE_TABLE = "instancebaseline";

    private static final int DEFERRED_MESSAGE_MAX = 20;

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.entities);

    private int entityCount;
    private FieldReader<DTClass> fieldReader;
    private int[] deletions;
    private int entitiesServerTick;
    private int serverTick;
    private final EntityRegistry entityRegistry = new EntityRegistry();

    private BaselineRegistry baselineRegistry;

    private ClientFrame entities;

    private boolean resetInProgress;
    private ClientFrame.Capsule resetCapsule;

    private final List<Runnable> queuedUpdates = new ArrayList<>();

    private final Int2ObjectSortedMap<CommonNetMessages.CSVCMsg_PacketEntities> deferredMessages = new Int2ObjectAVLTreeMap<>();

    @Insert
    private EngineType engineType;
    @Insert
    private DTClasses dtClasses;

    @InsertEvent
    private Event<OnEntityCreated> evCreated;
    @InsertEvent
    private Event<OnEntityUpdated> evUpdated;
    @InsertEvent
    private Event<OnEntityPropertyCountChanged> evPropertyCountChanged;
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

    @Initializer(OnEntityPropertyCountChanged.class)
    public void initPropertyCountChanged(final EventListener<OnEntityPropertyCountChanged> listener) {
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
        final var p = Pattern.compile(classPattern);
        return value -> {
            var e = (Entity) value[0];
            return p.matcher(e.getDtClass().getDtName()).matches();
        };
    }

    @OnInit
    public void onInit() {
        entityCount = 1 << engineType.getIndexBits();
        entities = new ClientFrame(entityCount);

        fieldReader = engineType.getNewFieldReader();

        deletions = new int[entityCount];
    }

    @OnReset
    public void onReset(Demo.CDemoStringTables packet, ResetPhase phase) {
        switch (phase) {
            case START:
                resetInProgress = true;
                resetCapsule = entities.createCapsule();
                break;

            case CLEAR:
                entities = new ClientFrame(entityCount);
                baselineRegistry.clear();
                deferredMessages.clear();
                break;

            case COMPLETE:
                resetInProgress = false;

                //updateEventDebug = true;

                Entity entity;
                for (var eIdx = 0; eIdx < entityCount; eIdx++) {
                    entity = entities.getEntity(eIdx);
                    if (resetCapsule.isExistent(eIdx)) {
                        if (entity == null || entity.getUid() != resetCapsule.getUid(eIdx)) {
                            var deletedEntity = entityRegistry.get(resetCapsule.getUid(eIdx));
                            if (resetCapsule.isActive(eIdx)) {
                                emitLeftEvent(deletedEntity);
                            }
                            emitDeletedEvent(deletedEntity);
                        }
                    }
                    if (entity != null) {
                        if (!resetCapsule.isExistent(eIdx) || entity.getUid() != resetCapsule.getUid(eIdx)) {
                            emitCreatedEvent(entity);
                            if (entity.isActive()) {
                                emitEnteredEvent(entity);
                            }
                        } else {
                            emitCompleteEntityChangeUpdatedEvents(resetCapsule.getState(eIdx), entity);
                        }
                    }
                }

                resetCapsule = null;

                //updateEventDebug = false;

                evUpdatesCompleted.raise();
                break;
        }
    }

    private void emitCompleteEntityChangeUpdatedEvents(EntityState oldState, Entity entity) {
        if (resetInProgress || (!evUpdated.isListenedTo() && !evPropertyCountChanged.isListenedTo())) {
            return;
        }
        List<FieldPath> changedFieldPaths = new ArrayList<>();
        var countChanged = new boolean[]{false};
        new StateDifferenceEvaluator(oldState, entity.getState()) {
            @Override
            protected void onPropertiesDeleted(List<FieldPath> fieldPaths) {
                countChanged[0] = true;
            }
            @Override
            protected void onPropertiesAdded(List<FieldPath> fieldPaths) {
                countChanged[0] = true;
                changedFieldPaths.addAll(fieldPaths);
            }
            @Override
            protected void onPropertyChanged(FieldPath fieldPath) {
                changedFieldPaths.add(fieldPath);
            }
        }.work();
        if (countChanged[0]) {
            emitPropertyCountChangedEvent(entity);
        }
        if (!changedFieldPaths.isEmpty()) {
            emitUpdatedEvent(entity, changedFieldPaths.toArray(new FieldPath[0]));
        }
    }

    @OnStringTableCreated
    public void onStringTableCreated(int numTables, StringTable table) {
        if (!BASELINE_TABLE.equals(table.getName())) {
            return;
        }
        baselineRegistry = new BaselineRegistry(table, entityCount);
    }

    @OnStringTableEntry(BASELINE_TABLE)
    public void onBaselineEntry(StringTable table, int index, String key, ByteString value) {
        baselineRegistry.markClassBaselineDirty(index);
        var separatorIdx = key.indexOf(':');
        if (separatorIdx != -1) {
            // alternate baseline, do not register
            return;
        }
        baselineRegistry.updateClassBaselineIndex(Integer.parseInt(key), index);
    }

    @OnMessage(CommonNetworkBaseTypes.CNETMsg_Tick.class)
    public void onMessage(CommonNetworkBaseTypes.CNETMsg_Tick message) {
        serverTick = message.getTick();
    }

    boolean debug = false;
    boolean updateEventDebug = false;

    void debugUpdateEvent(String which, Entity entity) {
        if (!updateEventDebug) return;
        log.info("\t%6s: index: %4d, serial: %03x, handle: %7d, class: %s",
                which,
                entity.getIndex(),
                entity.getSerial(),
                entity.getHandle(),
                entity.getDtClass().getDtName()
        );
    }

    private void queueUpdate(Runnable update) {
        queuedUpdates.add(update);
    }

    @OnMessage(CommonNetMessages.CSVCMsg_PacketEntities.class)
    public void onPacketEntities(CommonNetMessages.CSVCMsg_PacketEntities message) {
        if (message.getIsDelta()) {
            if (serverTick == message.getDeltaFrom()) {
                throw new ClarityException("received self-referential delta update for tick %d", serverTick);
            }
            if (entitiesServerTick < message.getDeltaFrom()) {
                log.debug("defer message with delta from %d at %d, since we are only at %d", message.getDeltaFrom(), serverTick, entitiesServerTick);
                deferredMessages.put(serverTick, message);
                if (deferredMessages.size() > DEFERRED_MESSAGE_MAX) {
                    log.warn("more than %d deferred messages, forcing execution, here be dragons", DEFERRED_MESSAGE_MAX);
                    deferredMessages.forEach((deferredMessageTick, deferredMessage) -> {
                        log.warn("forcing executing deferred message with delta %d, we are now at tick %d", deferredMessage.getDeltaFrom(), deferredMessageTick);
                        processAndRunPacketEntities(deferredMessage, deferredMessageTick);
                    });
                    deferredMessages.clear();
                }
                return;
            }
        } else if (!deferredMessages.isEmpty()) {
            log.debug("received full packet, disposing deferred message");
            deferredMessages.clear();
        }
        if (!deferredMessages.isEmpty()) {
            var deferredToExecute = deferredMessages.headMap(serverTick);
            if (!deferredToExecute.isEmpty()) {
                log.debug("server is now at tick %d", serverTick);
                deferredToExecute.forEach((deferredMessageTick, deferredMessage) -> {
                    log.debug("executing deferred message with delta %d, we are now at tick %d", deferredMessage.getDeltaFrom(), deferredMessageTick);
                    processAndRunPacketEntities(deferredMessage, deferredMessageTick);
                });
                deferredToExecute.clear();
            }
        }
        processAndRunPacketEntities(message, serverTick);
    }

    private void processAndRunPacketEntities(CommonNetMessages.CSVCMsg_PacketEntities message, int serverTick) {
        try {
            processPacketEntities(message, serverTick);
            entitiesServerTick = serverTick;
            log.debug("executing %d changes", queuedUpdates.size());
            queuedUpdates.forEach(Runnable::run);
            if (!resetInProgress) {
                evUpdatesCompleted.raise();
            }
        } finally {
            queuedUpdates.clear();
        }
    }

    private void processPacketEntities(CommonNetMessages.CSVCMsg_PacketEntities message, int actualTick) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "processing packet entities: now: %6d, delta-from: %6d, update-count: %5d, baseline: %d, update-baseline: %5s",
                    actualTick,
                    message.getDeltaFrom(),
                    message.getUpdatedEntries(),
                    message.getBaseline(),
                    message.getUpdateBaseline()
            );
        }

        if (message.getUpdateBaseline()) {
            queueUpdate(() -> baselineRegistry.switchEntityBaselines(message.getBaseline()));
        }

        for (var ab : message.getAlternateBaselinesList()) {
            baselineRegistry.updateEntityAlternateBaselineIndex(ab.getEntityIndex(), ab.getBaselineIndex());
        }

        var stream = BitStream.createBitStream(message.getEntityData());

        var updateCount = message.getUpdatedEntries();
        int updateType;
        var eIdx = -1;
        Entity eEnt;

        while (updateCount-- != 0) {
            eIdx += stream.readUBitVar() + 1;
            eEnt = entities.getEntity(eIdx);

            updateType = stream.readUBitInt(2);
            switch (updateType) {

                case 2: // CREATE
                    var dtClassId = stream.readUBitInt(dtClasses.getClassBits());
                    var dtClass = dtClasses.forClassId(dtClassId);
                    if (dtClass == null) {
                        throw new ClarityException("class for new entity %d is %d, but no dtClass found!.", eIdx, dtClassId);
                    }
                    var serial = stream.readUBitInt(engineType.getSerialBits());
                    if (engineType.getId().isEntitySkipExtraVarint()) {
                        // TODO: there is an extra VarInt encoded here for S2, figure out what it is
                        stream.readVarUInt();
                    }
                    if (eEnt != null) {
                        var handle = engineType.handleForIndexAndSerial(eIdx, serial);
                        if (eEnt.getUid() == Entity.uid(dtClassId, handle)) {
                            queueEntityRecreate(eEnt, message, stream);
                            break;
                        }
                        if (eEnt.isActive()) {
                            queueEntityLeave(eEnt);
                        }
                        queueEntityDelete(eEnt);
                    }
                    queueEntityCreate(eIdx, serial, dtClass, message, stream);
                    break;

                case 0: // UPDATE
                    if (eEnt == null) {
                        throw new ClarityException("Entity not found for update at index %d. Entity update cannot be parsed!", eIdx);
                    }
                    queueEntityUpdate(eEnt, stream, false);
                    break;

                case 1: // LEAVE
                    if (eEnt != null && eEnt.isActive()) {
                        queueEntityLeave(eEnt);
                    }
                    break;

                case 3: // DELETE
                    if (eEnt != null) {
                        if (eEnt.isActive()) {
                            queueEntityLeave(eEnt);
                        }
                        queueEntityDelete(eEnt);
                    }
                    break;
            }
        }

        if (engineType.handleDeletions() && message.getIsDelta()) {
            var n = fieldReader.readDeletions(stream, engineType.getIndexBits(), deletions);
            for (var i = 0; i < n; i++) {
                eIdx = deletions[i];
                eEnt = entities.getEntity(eIdx);
                if (eEnt != null) {
                    if (eEnt.isActive()) {
                        queueEntityLeave(eEnt);
                    }
                    queueEntityDelete(eEnt);
                }
            }
        }

        log.debug("update finished for tick %d", actualTick);

    }

    private void queueEntityCreate(int eIdx, int serial, DTClass dtClass, CommonNetMessages.CSVCMsg_PacketEntities message, BitStream stream) {
        var changes = fieldReader.readFields(stream, dtClass, debug);
        queueUpdate(() -> executeEntityCreate(eIdx, serial, dtClass, message, changes));
    }

    private void executeEntityCreate(int eIdx, int serial, DTClass dtClass, CommonNetMessages.CSVCMsg_PacketEntities message, FieldChanges changes) {
        var newState = getBaseline(dtClass.getClassId(), message.getBaseline(), eIdx, message.getIsDelta()).copy();
        changes.applyTo(newState);
        var entity = entityRegistry.create(
                dtClass.getClassId(),
                eIdx, serial,
                engineType.handleForIndexAndSerial(eIdx, serial),
                dtClass);
        entity.setExistent(true);
        entity.setState(newState);
        entities.setEntity(entity);
        logModification("CREATE", entity);
        emitCreatedEvent(entity);
        executeEntityEnter(entity);
        if (message.getUpdateBaseline()) {
            baselineRegistry.updateEntityBaseline(
                    message.getBaseline(),
                    eIdx,
                    dtClass.getClassId(),
                    newState.copy()
            );
        }
    }

    private void queueEntityRecreate(Entity entity, CommonNetMessages.CSVCMsg_PacketEntities message, BitStream stream) {
        var changes = fieldReader.readFields(stream, entity.getDtClass(), debug);
        queueUpdate(() -> executeEntityRecreate(entity, message, changes));
    }

    private void executeEntityRecreate(Entity entity, CommonNetMessages.CSVCMsg_PacketEntities message, FieldChanges changes) {
        var oldState = entity.getState();
        var wasActive = entity.isActive();
        var eIdx = entity.getIndex();

        var dtClass = entity.getDtClass();
        var newState = getBaseline(dtClass.getClassId(), message.getBaseline(), eIdx, message.getIsDelta()).copy();
        changes.applyTo(newState);
        entity.setState(newState);
        entity.setActive(true);
        logModification("RECREATE", entity);
        if (message.getUpdateBaseline()) {
            baselineRegistry.updateEntityBaseline(
                    message.getBaseline(),
                    eIdx,
                    dtClass.getClassId(),
                    newState.copy()
            );
        }
        if (!wasActive) {
            emitEnteredEvent(entity);
        }
        emitCompleteEntityChangeUpdatedEvents(oldState, entity);
    }

    private void queueEntityUpdate(Entity entity, BitStream stream, boolean silent) {
        var changes = fieldReader.readFields(stream, entity.getDtClass(), debug);
        queueUpdate(() -> executeEntityUpdate(entity, changes, silent));
    }

    private void executeEntityUpdate(Entity entity, FieldChanges changes, boolean silent) {
        assert silent || (entity.isExistent() && entity.isActive());
        var capacityChanged = changes.applyTo(entity.getState());
        logModification("UPDATE", entity);
        if (!silent) {
            if (capacityChanged) {
                emitPropertyCountChangedEvent(entity);
            }
            emitUpdatedEvent(entity, changes.getFieldPaths());
        }
    }

    private void queueEntityEnter(Entity entity) {
        queueUpdate(() -> executeEntityEnter(entity));
    }

    private void executeEntityEnter(Entity entity) {
        assert !entity.isActive();
        entity.setActive(true);
        logModification("ENTER", entity);
        emitEnteredEvent(entity);
    }

    private void queueEntityLeave(Entity entity) {
        queueUpdate(() -> executeEntityLeave(entity));
    }

    private void executeEntityLeave(Entity entity) {
        assert entity.isActive();
        entity.setActive(false);
        logModification("LEAVE", entity);
        emitLeftEvent(entity);
    }

    private void queueEntityDelete(Entity entity) {
        queueUpdate(() -> executeEntityDelete(entity));
    }

    private void executeEntityDelete(Entity entity) {
        assert entity.isExistent();
        entity.setExistent(false);
        entities.removeEntity(entity);
        logModification("DELETE", entity);
        emitDeletedEvent(entity);
    }

    private void emitCreatedEvent(Entity entity) {
        if (resetInProgress || !evCreated.isListenedTo()) return;
        debugUpdateEvent("CREATE", entity);
        evCreated.raise(entity);
    }

    private void emitEnteredEvent(Entity entity) {
        if (resetInProgress || !evEntered.isListenedTo()) return;
        debugUpdateEvent("ENTER", entity);
        evEntered.raise(entity);
    }

    private void emitUpdatedEvent(Entity entity, FieldPath[] updatedFieldPaths) {
        if (resetInProgress || !evUpdated.isListenedTo()) return;
        debugUpdateEvent("UPDATE", entity);
        evUpdated.raise(entity, updatedFieldPaths, updatedFieldPaths.length);
    }

    private void emitPropertyCountChangedEvent(Entity entity) {
        if (resetInProgress || !evPropertyCountChanged.isListenedTo()) return;
        debugUpdateEvent("PROPERTYCOUNTCHANGE", entity);
        evPropertyCountChanged.raise(entity);
    }

    private void emitLeftEvent(Entity entity) {
        if (resetInProgress || !evLeft.isListenedTo()) return;
        debugUpdateEvent("LEAVE", entity);
        evLeft.raise(entity);
    }

    private void emitDeletedEvent(Entity entity) {
        if (resetInProgress || !evDeleted.isListenedTo()) return;
        debugUpdateEvent("DELETE", entity);
        evDeleted.raise(entity);
    }

    private void logModification(String which, Entity entity) {
        if (!log.isDebugEnabled()) return;
        log.debug("\t%6s: index: %4d, serial: %03x, handle: %7d, class: %s",
                which,
                entity.getIndex(),
                entity.getSerial(),
                entity.getHandle(),
                entity.getDtClass().getDtName()
        );
    }

    private EntityState getBaseline(int clsId, int baseline, int entityIdx, boolean delta) {
        EntityState s;
        if (delta) {
            s = baselineRegistry.getEntityBaselineState(entityIdx, baseline, clsId);
            if (s != null) {
                return s;
            }
        }
        var cls = dtClasses.forClassId(clsId);
        if (cls == null) {
            throw new ClarityException("DTClass for id %d not found.", clsId);
        }
        var baselineIdx = baselineRegistry.getClassBaselineIndex(clsId, entityIdx);
        if (baselineIdx == -1) {
            log.error("Baseline index for class %s (%d) not found. Continuing anyway, but data might be missing!", cls.getDtName(), clsId);
            return cls.getEmptyState();
        }
        s = baselineRegistry.getClassBaselineState(baselineIdx);
        if (s != null) {
            return s;
        }
        var raw = baselineRegistry.getClassBaselineData(baselineIdx);
        if (raw == null || raw.size() == 0) {
            log.error("Baseline data for class %s (%d) not found. Continuing anyway, but data might be missing!", cls.getDtName(), clsId);
            return cls.getEmptyState();
        }
        s = cls.getEmptyState();
        var stream = BitStream.createBitStream(raw);
        var changes = fieldReader.readFields(stream, cls, false);
        changes.applyTo(s);
        baselineRegistry.setClassBaselineState(baselineIdx, s);
        return s;
    }

    public Entity getByIndex(int index) {
        return entities.getEntity(index);
    }

    public Entity getByHandle(int handle) {
        var e = getByIndex(engineType.indexForHandle(handle));
        return e == null || e.getHandle() != handle ? null : e;
    }

    public Iterator<Entity> getAllByPredicate(final Predicate<Entity> predicate) {
        return new SimpleIterator<Entity>() {
            int i = -1;

            @Override
            public Entity readNext() {
                while (++i < entityCount) {
                    var e = getByIndex(i);
                    if (e != null && predicate.apply(e)) {
                        return e;
                    }
                }
                return null;
            }
        };
    }

    public Entity getByPredicate(Predicate<Entity> predicate) {
        var iter = getAllByPredicate(predicate);
        return iter.hasNext() ? iter.next() : null;
    }

    public Iterator<Entity> getAllByDtName(final String dtClassName) {
        return getAllByPredicate(
                e -> dtClassName.equals(e.getDtClass().getDtName()));
    }

    public Entity getByDtName(final String dtClassName) {
        var iter = getAllByDtName(dtClassName);
        return iter.hasNext() ? iter.next() : null;
    }

}
