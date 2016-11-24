package skadistats.clarity.processor.resources;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.ClarityException;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.event.Provides;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.util.LZSS;
import skadistats.clarity.util.MurmurHash;
import skadistats.clarity.wire.common.proto.NetMessages;
import skadistats.clarity.wire.common.proto.NetworkBaseTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Provides({UsesResources.class})
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

    @OnMessage(NetMessages.CSVCMsg_ServerInfo.class)
    public void onServerInfo(NetMessages.CSVCMsg_ServerInfo message) throws IOException {
        clear();
        gameSessionManifest = new GameSessionManifest();
        addManifestData(gameSessionManifest, message.getGameSessionManifest());
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_Load.class)
    public void onLoad(NetworkBaseTypes.CNETMsg_SpawnGroup_Load message) throws IOException {
        if (spawnGroupManifests.containsKey(message.getSpawngrouphandle())) {
            throw new ClarityException("CNETMsg_SpawnGroup_Load for an already existing handle: %d", message.getSpawngrouphandle());
        }
        SpawnGroupManifest m = new SpawnGroupManifest();
        m.spawnGroupHandle = message.getSpawngrouphandle();
        m.creationSequence = message.getCreationsequence();
        m.incomplete = message.getManifestincomplete();
        spawnGroupManifests.put(m.spawnGroupHandle, m);

        addManifestData(m, message.getSpawngroupmanifest());
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate.class)
    public void onManifestUpdate(NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate message) throws IOException {
        SpawnGroupManifest m = spawnGroupManifests.get(message.getSpawngrouphandle());
        if (m == null) {
            throw new ClarityException("CNETMsg_SpawnGroup_ManifestUpdate for an unknown handle: %d", message.getSpawngrouphandle());
        }

        m.incomplete = message.getManifestincomplete();
        addManifestData(m, message.getSpawngroupmanifest());
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted.class)
    public void onLoadCompleted(NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted message) {
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick.class)
    public void onSetCreationTick(NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick message) {
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_Unload.class)
    public void onUnload(NetworkBaseTypes.CNETMsg_SpawnGroup_Unload message) {
    }

    public Entry getEntryForResourceHandle(long resourceHandle) {
        return resourceHandles.get(resourceHandle);
    }

    protected void addStaticResourceEntry(String dir, String name, String extension) {
        addEntryToResourceHandles(
                new Entry(
                        storeHash(dirs, dir),
                        name,
                        storeHash(exts, extension)
                )
        );
    }

    protected void addManifestData(Manifest manifest, ByteString raw) throws IOException {

        BitStream bs = BitStream.createBitStream(raw);
        boolean isCompressed = bs.readBitFlag();
        int size = bs.readUBitInt(24);

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

        int nTypes = bs.readUBitInt(16);
        int nDirs = bs.readUBitInt(16);
        int nEntries = bs.readUBitInt(16);

        for (int i = 0; i < nTypes; i++) {
            extHashes.add(storeHash(exts, bs.readString(Integer.MAX_VALUE)));
        }
        for (int i = 0; i < nDirs; i++) {
            dirHashes.add(storeHash(dirs, bs.readString(Integer.MAX_VALUE)));
        }
        int bitsForType = Math.max(1, Util.calcBitsNeededFor(nTypes - 1));
        int bitsForDir = Math.max(1, Util.calcBitsNeededFor(nDirs - 1));

        for (int i = 0; i < nEntries; i++) {

            int dirIdx = bs.readUBitInt(bitsForDir);
            String file = bs.readString(Integer.MAX_VALUE);
            int extIdx = bs.readUBitInt(bitsForType);
            Entry entry = new Entry(dirHashes.get(dirIdx), file, extHashes.get(extIdx));

            manifest.entries.add(entry);

            addEntryToResourceHandles(entry);

            //System.out.format("%20d %s\n", hash, entryStr);
        }
    }

    private long storeHash(Map<Long, String> map, String value) {
        long hash = hash(value);
        String existingValue = map.get(hash);
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

    public class SpawnGroupManifest extends Manifest {
        private int spawnGroupHandle;
        private int creationSequence;
        private boolean incomplete;

        public int getSpawnGroupHandle() {
            return spawnGroupHandle;
        }

        public int getCreationSequence() {
            return creationSequence;
        }

        public boolean isIncomplete() {
            return incomplete;
        }
    }

    public class GameSessionManifest extends Manifest {
    }

    public class Manifest {
        private final List<Entry> entries = new ArrayList<>();
    }

    public class Entry {
        private final long dirHash;
        private final String name;
        private final long extHash;

        private Entry(long dirHash, String name, long extHash) {
            this.dirHash = dirHash;
            this.name = name;
            this.extHash = extHash;
        }

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
