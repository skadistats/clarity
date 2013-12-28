package test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.iterator.BidiIterator;
import clarity.match.Match;
import clarity.parser.Peek;
import clarity.parser.ReplayFile;
import clarity.parser.ReplayIndex;

public class Test {

    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("TEST");

        // last
        ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\432850581.dem");

        // TI3 final
        //ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\271145478.dem");
        
        Match match = new Match(idx);
        long tInit = System.currentTimeMillis() - tStart;
        log.info("prologue applied in {}s", tInit / 1000.0);
        
        tStart = System.currentTimeMillis();
        for (BidiIterator<Peek> i = idx.matchIterator(); i.hasNext();) {
            Peek p = i.next();
            p.apply(match);
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("match applied in {}s", tMatch / 1000.0);
        log.info("total time taken: {}s", (tInit + tMatch) / 1000.0);

    }

}
