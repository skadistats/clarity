package skadistats.clarity.processor.resources;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.event.Provides;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.util.LZSS;
import skadistats.clarity.util.MurmurHash;
import skadistats.clarity.wire.common.proto.NetMessages;
import skadistats.clarity.wire.common.proto.NetworkBaseTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Provides({UsesResources.class})
public class Resources {

    private final List<String> dirs = new ArrayList<>();
    private final List<String> exts = new ArrayList<>();

    private GameSessionManifest gameSessionManifest;
    private final Map<Integer, SpawnGroupManifest> spawnGroupManifests = new HashMap<>();
    private final Map<Long, Manifest.Entry> resourceHandles = new HashMap<>();

    @OnMessage(NetMessages.CSVCMsg_ServerInfo.class)
    public void onServerInfo(Context ctx, NetMessages.CSVCMsg_ServerInfo message) throws IOException {
        gameSessionManifest = new GameSessionManifest();
        gameSessionManifest.addManifestData(message.getGameSessionManifest());
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_Load.class)
    public void onLoad(Context ctx, NetworkBaseTypes.CNETMsg_SpawnGroup_Load message) throws IOException {
        if (spawnGroupManifests.containsKey(message.getSpawngrouphandle())) {
            throw new RuntimeException("CNETMsg_SpawnGroup_Load for an already existing handle: " + message.getSpawngrouphandle());
        }
        SpawnGroupManifest m = new SpawnGroupManifest();
        m.spawnGroupHandle = message.getSpawngrouphandle();
        m.incomplete = message.getManifestincomplete();
        spawnGroupManifests.put(m.spawnGroupHandle, m);

        m.addManifestData(message.getSpawngroupmanifest());
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate.class)
    public void onManifestUpdate(Context ctx, NetworkBaseTypes.CNETMsg_SpawnGroup_ManifestUpdate message) throws IOException {
        SpawnGroupManifest m = spawnGroupManifests.get(message.getSpawngrouphandle());
        if (m == null) {
            throw new RuntimeException("CNETMsg_SpawnGroup_ManifestUpdate for an unknown handle: " + message.getSpawngrouphandle());
        }

        m.incomplete = message.getManifestincomplete();
        m.addManifestData(message.getSpawngroupmanifest());
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted.class)
    public void onLoadCompleted(Context ctx, NetworkBaseTypes.CNETMsg_SpawnGroup_LoadCompleted message) {
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick.class)
    public void onSetCreationTick(Context ctx, NetworkBaseTypes.CNETMsg_SpawnGroup_SetCreationTick message) {
    }

    @OnMessage(NetworkBaseTypes.CNETMsg_SpawnGroup_Unload.class)
    public void onUnload(Context ctx, NetworkBaseTypes.CNETMsg_SpawnGroup_Unload message) {
    }

    public Manifest.Entry getEntryForResourceHandle(long resourceHandle) {
        return resourceHandles.get(resourceHandle);
    }

    public class SpawnGroupManifest extends Manifest {
        private int spawnGroupHandle;
        private boolean incomplete;
    }

    public class GameSessionManifest extends Manifest {

    }

    public class Manifest {

        private final List<Entry> entries = new ArrayList<>();

        protected void addManifestData(ByteString raw) throws IOException {

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

            int bExts = exts.size();
            int bDirs = dirs.size();

            int nTypes = bs.readUBitInt(16);
            int nDirs = bs.readUBitInt(16);
            int nEntries = bs.readUBitInt(16);

            for (int i = 0; i < nTypes; i++) {
                exts.add(bs.readString(Integer.MAX_VALUE));
            }
            for (int i = 0; i < nDirs; i++) {
                dirs.add(bs.readString(Integer.MAX_VALUE));
            }
            int bitsForType = Math.max(1, Util.calcBitsNeededFor(nTypes - 1));
            int bitsForDir = Math.max(1, Util.calcBitsNeededFor(nDirs - 1));
            for (int i = 0; i < nEntries; i++) {
                int dirIdx = bDirs + bs.readUBitInt(bitsForDir);
                String file = bs.readString(Integer.MAX_VALUE);
                int extIdx = bExts + bs.readUBitInt(bitsForType);
                Entry entry = new Entry(dirIdx, file, extIdx);
                entries.add(entry);

                String entryStr = entry.toString();
                long hash = MurmurHash.hash64(entryStr);
                resourceHandles.put(hash, entry);

                //System.out.format("%20d %s\n", hash, entryStr);
            }
        }

        public class Entry {

            private final int dirIdx;
            private final String name;
            private final int extIdx;

            public Entry(int dirIdx, String name, int extIdx) {
                this.dirIdx = dirIdx;
                this.name = name;
                this.extIdx = extIdx;
            }

            @Override
            public String toString() {
                return String.format("%s%s.%s", dirs.get(dirIdx), name, exts.get(extIdx));
            }

        }

    }

}
