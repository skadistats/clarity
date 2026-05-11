package skadistats.clarity.processor.entities;

import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * End-to-end parity: parsing a replay once with no filter and once with a
 * reject-everything filter SHALL reach the same final tick and process the
 * same number of CSVCMsg_PacketEntities messages. Drift in any decoder's
 * skip path manifests as a thrown exception (misaligned downstream reads
 * resolve to garbage class ids / eIdx overflows) or as a tick count
 * mismatch.
 */
public class RejectEverythingParityTest {

    private static final String REPLAY_DIR =
            System.getProperty("clarity.test.replayDir", "/home/spheenik/projects/replays");

    @DataProvider(name = "replays")
    public Object[][] replays() {
        return new Object[][]{
                {"dota/s2/normal/1560289528.dem"},
                {"dota/s1/normal/271145478.dem"},
        };
    }

    @Test(dataProvider = "replays")
    public void rejectEverythingParity(String relPath) throws Exception {
        Path replay = Paths.get(REPLAY_DIR, relPath);
        if (!Files.isReadable(replay)) {
            throw new SkipException("replay not available: " + replay);
        }

        var baseline = parse(replay, false);
        var rejectAll = parse(replay, true);

        assertTrue(baseline.packetCount > 0, "baseline parse processed no entity packets");
        assertTrue(baseline.liveEntities > 0, "baseline parse has no live entities");
        assertEquals(rejectAll.lastTick, baseline.lastTick, "lastTick differs");
        assertEquals(rejectAll.packetCount, baseline.packetCount,
                "CSVCMsg_PacketEntities count differs");
        assertEquals(rejectAll.liveEntities, 0,
                "reject-all parse left live entities in the collection");
    }

    private static Summary parse(Path replay, boolean rejectAll) throws Exception {
        var counter = new PacketCounter();
        try (var src = new MappedFileSource(replay.toString())) {
            var runner = new SimpleRunner(src);
            if (rejectAll) {
                runner.withEntityFilter(dt -> false);
            }
            runner.runWith(counter);
            var entities = runner.getContext().getProcessor(Entities.class);
            var liveEntities = (int) entities.stream().count();
            return new Summary(runner.getTick(), counter.count, liveEntities);
        }
    }

    /**
     * Seek with a reject-all filter: the seek triggers the CLEAR/APPLY/COMPLETE
     * reset cycle. CLEAR wipes {@code skippedClass}, APPLY's CDemoFullPacket
     * replay re-runs the CREATE branch for every alive entity (and our filter
     * re-marks {@code skippedClass}); ticking forward then exercises UPDATEs on
     * filtered ids, which would throw "Entity not found for update" if the reset
     * path forgot to repopulate the skip-class map.
     */
    @Test(dataProvider = "replays")
    public void rejectEverythingSeekHandlesReset(String relPath) throws Exception {
        Path replay = Paths.get(REPLAY_DIR, relPath);
        if (!Files.isReadable(replay)) {
            throw new SkipException("replay not available: " + replay);
        }
        try (var src = new MappedFileSource(replay.toString())) {
            var holder = new EntityHolder();
            var runner = new ControllableRunner(src);
            runner.withEntityFilter(dt -> false);
            runner.runWith(holder);
            try {
                var targetTick = (int) (runner.getLastTick() * 0.7);
                runner.seek(targetTick);
                assertTrue(runner.getTick() >= targetTick - 1,
                        "seek did not reach target tick: target=" + targetTick + " got=" + runner.getTick());
                var entities = runner.getContext().getProcessor(Entities.class);
                assertTrue(entities != null, "Entities processor not in the run");
                assertEquals(entities.stream().count(), 0L,
                        "live entities present after seek with reject-all");
                for (int i = 0; i < 200; i++) {
                    runner.tick();
                }
                assertEquals(entities.stream().count(), 0L,
                        "live entities appeared after post-seek forward ticks");
            } finally {
                runner.halt();
                runner.join();
            }
        }
    }

    @skadistats.clarity.processor.entities.UsesEntities
    public static class EntityHolder {
        // class-level @UsesEntities pulls the Entities processor into the run
    }

    @skadistats.clarity.processor.entities.UsesEntities
    public static class PacketCounter {
        int count;

        @OnMessage(CommonNetMessages.CSVCMsg_PacketEntities.class)
        public void onPacketEntities(CommonNetMessages.CSVCMsg_PacketEntities msg) {
            count++;
        }
    }

    private record Summary(int lastTick, int packetCount, int liveEntities) {}
}
