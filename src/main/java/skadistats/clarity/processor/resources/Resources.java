package skadistats.clarity.processor.resources;

import skadistats.clarity.protobuf.ByteString;
import skadistats.clarity.protobuf.ZeroCopy;
import skadistats.clarity.ClarityException;
import skadistats.clarity.event.Provides;
import skadistats.clarity.io.Util;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.util.LZSS;
import skadistats.clarity.util.MurmurHash;
import skadistats.clarity.wire.shared.demo.proto.DemoNetMessages;
import skadistats.clarity.wire.shared.s2.proto.S2NetworkBaseTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resource manifests of a Source 2 replay, and a lookup from resource handle to resource path.
 * <p>
 * Applies to {@link EngineId#DOTA_S2}, {@link EngineId#CS2} and {@link EngineId#DEADLOCK}. Active when a processor
 * declares {@link UsesResources}. The game session manifest is read from {@code CSVCMsg_ServerInfo} (and all state
 * is reset when it arrives); spawn group manifests are read from the {@code CNETMsg_SpawnGroup_*} messages.
 * Resource handles are 64-bit MurmurHash values of the full resource path.
 */
@Provides(value = {UsesResources.class}, engine = {EngineId.DOTA_S2, EngineId.CS2, EngineId.DEADLOCK})
public class Resources {

    /*
        BIG thanks to Robin Dietrich (https://github.com/invokr/) for finding out how to map a resource path to a
        strong handle!
     */

    private final Map<Long, String> dirs = new HashMap<>();
    private final Map<Long, String> exts = new HashMap<>();

    private GameSessionManifest gameSessionManifest;
    private final Map<Integer, SpawnGroupManifest> spawnGroupManifests = new HashMap<>();
    private final Map<Long, Entry> resourceHandles = new HashMap<>();

    /**
     * @return the manifest sent with the server info, or {@code null} if no server info was seen yet
     */
    public GameSessionManifest getGameSessionManifest() {
        return gameSessionManifest;
    }

    /**
     * @return an unmodifiable view of the manifests of all currently known spawn groups
     */
    public Collection<SpawnGroupManifest> getManifests() {
        return Collections.unmodifiableCollection(spawnGroupManifests.values());
    }

    private void clear() {
        dirs.clear();
        exts.clear();
        gameSessionManifest = null;
        spawnGroupManifests.clear();
        resourceHandles.clear();
    }

    /** Internal: resets all state and reads the game session manifest. */
    @OnMessage(DemoNetMessages.CSVCMsg_ServerInfo.class)
    public void onServerInfo(DemoNetMessages.CSVCMsg_ServerInfo message) throws IOException {
        clear();
        gameSessionManifest = new GameSessionManifest();
        addManifestData(gameSessionManifest, message.getGameSessionManifest());
    }

    /** Internal: registers a new spawn group manifest. */
    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_Load.class)
    public void onLoad(S2NetworkBaseTypes.CNETMsg_SpawnGroup_Load message) throws IOException {
        if (spawnGroupManifests.containsKey(message.getSpawngrouphandle())) {
            throw new ClarityException("CNETMsg_SpawnGroup_Load for an already existing handle: %d", message.getSpawngrouphandle());
        }
        var m = new SpawnGroupManifest();
        m.spawnGroupHandle = message.getSpawngrouphandle();
        m.creationSequence = message.getCreationsequence();
        m.incomplete = message.getManifestincomplete();
        spawnGroupManifests.put(m.spawnGroupHandle, m);

        addManifestData(m, message.getSpawngroupmanifest());
    }

    /** Internal: adds entries to an existing spawn group manifest. */
    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate.class)
    public void onManifestUpdate(S2NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate message) throws IOException {
        var m = spawnGroupManifests.get(message.getSpawngrouphandle());
        if (m == null) {
            throw new ClarityException("CNETMsg_SpawnGroup_ManifestUpdate for an unknown handle: %d", message.getSpawngrouphandle());
        }

        m.incomplete = message.getManifestincomplete();
        addManifestData(m, message.getSpawngroupmanifest());
    }

    /** Internal: no-op. */
    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted.class)
    public void onLoadCompleted(S2NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted message) {
    }

    /** Internal: no-op. */
    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick.class)
    public void onSetCreationTick(S2NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick message) {
    }

    /** Internal: no-op, manifests are kept after unload. */
    @OnMessage(S2NetworkBaseTypes.CNETMsg_SpawnGroup_Unload.class)
    public void onUnload(S2NetworkBaseTypes.CNETMsg_SpawnGroup_Unload message) {
    }

    /**
     * @param resourceHandle a 64-bit resource handle
     * @return the entry for that handle, or {@code null} if the handle is not known from any manifest
     */
    public Entry getEntryForResourceHandle(long resourceHandle) {
        return resourceHandles.get(resourceHandle);
    }

    /**
     * Registers a resource that is not listed in any manifest.
     *
     * @param dir directory, including the trailing slash
     * @param name file name without extension
     * @param extension file extension without the dot
     */
    protected void addStaticResourceEntry(String dir, String name, String extension) {
        addEntryToResourceHandles(
                new Entry(
                        storeHash(dirs, dir),
                        name,
                        storeHash(exts, extension)
                )
        );
    }

    /**
     * Decodes a serialized (optionally LZSS-compressed) manifest, adds its entries to {@code manifest} and
     * registers them for handle lookup.
     *
     * @param manifest the manifest to add the entries to
     * @param raw the serialized manifest data
     * @throws IOException if the data cannot be read
     */
    protected void addManifestData(Manifest manifest, ByteString raw) throws IOException {

        var bs = BitStream.createBitStream(raw);
        var isCompressed = bs.readBitFlag();
        var size = bs.readUBitInt(24);

        byte[] data;
        if (isCompressed) {
            data = LZSS.unpack(bs);
        } else {
            data = new byte[size];
            bs.readBitsIntoByteArray(data, size * 8);
        }
        bs = BitStream.createBitStream(ZeroCopy.wrap(data));

        List<Long> extHashes = new ArrayList<>();
        List<Long> dirHashes = new ArrayList<>();

        var nTypes = bs.readUBitInt(16);
        var nDirs = bs.readUBitInt(16);
        var nEntries = bs.readUBitInt(16);

        for (var i = 0; i < nTypes; i++) {
            extHashes.add(storeHash(exts, bs.readString(BitStream.MAX_STRING_LENGTH)));
        }
        for (var i = 0; i < nDirs; i++) {
            dirHashes.add(storeHash(dirs, bs.readString(BitStream.MAX_STRING_LENGTH)));
        }
        var bitsForType = Math.max(1, Util.calcBitsNeededFor(nTypes - 1));
        var bitsForDir = Math.max(1, Util.calcBitsNeededFor(nDirs - 1));

        for (var i = 0; i < nEntries; i++) {

            var dirIdx = bs.readUBitInt(bitsForDir);
            var file = bs.readString(BitStream.MAX_STRING_LENGTH);
            var extIdx = bs.readUBitInt(bitsForType);
            var entry = new Entry(dirHashes.get(dirIdx), file, extHashes.get(extIdx));

            manifest.entries.add(entry);

            addEntryToResourceHandles(entry);

            //System.out.format("%20d %s\n", hash, entryStr);
        }
    }

    private long storeHash(Map<Long, String> map, String value) {
        var hash = hash(value);
        var existingValue = map.get(hash);
        if (existingValue != null) {
            if (!existingValue.equals(value)) {
                throw new ClarityException("hash collision for value %s", value);
            }
        } else {
            map.put(hash, value);
        }
        return hash;
    }

    private void addEntryToResourceHandles(Entry entry) {
        resourceHandles.put(hash(entry.toString()), entry);
    }

    private long hash(String value) {
        return MurmurHash.hash64(value, 0xEDABCDEF);
    }

    /** Manifest of a single spawn group. */
    public class SpawnGroupManifest extends Manifest {
        private int spawnGroupHandle;
        private int creationSequence;
        private boolean incomplete;

        /**
         * @return the handle of the spawn group, as in {@link skadistats.clarity.model.Entity#getSpawnGroupHandle()}
         */
        public int getSpawnGroupHandle() {
            return spawnGroupHandle;
        }

        /**
         * @return the creation sequence sent with the spawn group load message
         */
        public int getCreationSequence() {
            return creationSequence;
        }

        /**
         * @return the {@code manifestincomplete} flag of the latest load or manifest update message for this spawn
         *         group
         */
        public boolean isIncomplete() {
            return incomplete;
        }
    }

    /** Manifest sent with the server info. */
    public class GameSessionManifest extends Manifest {
    }

    /** A list of resource entries. */
    public class Manifest {
        private final List<Entry> entries = new ArrayList<>();

        /**
         * @return an unmodifiable view of the entries of this manifest
         */
        public List<Entry> getEntries() {
            return Collections.unmodifiableList(entries);
        }
    }

    /** A resource, identified by directory, name and extension. */
    public class Entry {
        private final long dirHash;
        private final String name;
        private final long extHash;

        private Entry(long dirHash, String name, long extHash) {
            this.dirHash = dirHash;
            this.name = name;
            this.extHash = extHash;
        }

        /**
         * @return the directory, including the trailing slash
         */
        public String getDir() {
            return dirs.get(dirHash);
        }

        /**
         * @return the file name without extension
         */
        public String getName() {
            return name;
        }

        /**
         * @return the file extension without the dot
         */
        public String getExtension() {
            return exts.get(extHash);
        }

        /**
         * @return the full resource path, {@code dir + name + "." + extension}
         */
        @Override
        public String toString() {
            return String.format("%s%s.%s", dirs.get(dirHash), name, exts.get(extHash));
        }

    }

    {
        addStaticResourceEntry("models/creeps/roshan/", "aegis", "vmdl");
        addStaticResourceEntry("models/heroes/shopkeeper_dire/", "shopkeeper_dire", "vmdl");
        addStaticResourceEntry("models/heroes/shopkeeper/", "shopkeeper", "vmdl");
        addStaticResourceEntry("models/props_gameplay/quirt/", "quirt", "vmdl");
        addStaticResourceEntry("models/props_gameplay/shopkeeper_fountain/", "shopkeeper_fountain", "vmdl");
        addStaticResourceEntry("models/props_gameplay/sithil/", "sithil", "vmdl");
        addStaticResourceEntry("models/props_structures/", "bad_ancient_destruction_camera", "vmdl");
        addStaticResourceEntry("models/props_structures/", "dire_ancient_base001", "vmdl");
        addStaticResourceEntry("models/props_structures/", "dire_barracks_melee001", "vmdl");
        addStaticResourceEntry("models/props_structures/", "dire_barracks_ranged001", "vmdl");
        addStaticResourceEntry("models/props_structures/", "dire_column001", "vmdl");
        addStaticResourceEntry("models/props_structures/", "dire_fountain002", "vmdl");
        addStaticResourceEntry("models/props_structures/", "dire_statue001", "vmdl");
        addStaticResourceEntry("models/props_structures/", "dire_statue002", "vmdl");
        addStaticResourceEntry("models/props_structures/", "dire_tower002", "vmdl");
        addStaticResourceEntry("models/props_structures/", "radiant_ancient001", "vmdl");
        addStaticResourceEntry("models/props_structures/", "radiant_endcam", "vmdl");
        addStaticResourceEntry("models/props_structures/", "radiant_fountain002", "vmdl");
        addStaticResourceEntry("models/props_structures/", "radiant_melee_barracks001", "vmdl");
        addStaticResourceEntry("models/props_structures/", "radiant_ranged_barracks001", "vmdl");
        addStaticResourceEntry("models/props_structures/", "radiant_statue001", "vmdl");
        addStaticResourceEntry("models/props_structures/", "radiant_statue002", "vmdl");
        addStaticResourceEntry("models/props_structures/", "radiant_tower002", "vmdl");
    }

}
