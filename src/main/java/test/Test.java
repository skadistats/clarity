package test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.model.PacketType;
import clarity.parser.ReplayFile;
import clarity.parser.ReplayIndex;

public class Test {

    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("TEST");

        // last ranked
        //ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\432850581.dem");
        // TI3 final
        ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\271145478.dem");
        // big and old, has NAGA in it, maybe TI2?
        //ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\37633163.dem");
        // pudge
        //ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\448793524.dem");
        
        long tIndex = System.currentTimeMillis() - tStart;
        log.info("index built in {}s", tIndex / 1000.0);

        tStart = System.currentTimeMillis();
        Match match = new Match(idx.prologueIterator());
        long tPrologue = System.currentTimeMillis() - tStart;
        log.info("prologue applied in {}s", tPrologue / 1000.0);
        
        tStart = System.currentTimeMillis();

        // this applies the whole match
        match.apply(idx.matchIteratorForTicks(0, idx.getLastTick(), PacketType.DELTA));
        
        // this does several seek operations
//        for (int c = 0; c < 100; c++) {
//            long tSkip = System.currentTimeMillis();
//            int t = (int)(Math.random() * idx.getLastTick());
//            match.reset();
//            int nApplied = match.apply(idx.skipToIterator(t));
//            Snapshot s = new Snapshot();
//            log.info("restored to peek {} in {}s, visited {} packets, got snapshot {}", t, (System.currentTimeMillis() - tSkip) / 1000.0, nApplied, s);
//        }

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("match applied in {}s", tMatch / 1000.0);
        log.info("total time taken: {}s", (tIndex + tPrologue + tMatch) / 1000.0);

    }

}
