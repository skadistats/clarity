package test;

import clarity.iterator.BidiIterator;
import clarity.match.Match;
import clarity.parser.Peek;
import clarity.parser.ReplayFile;
import clarity.parser.ReplayIndex;

public class Test {

    public static void main(String[] args) throws Exception {

        try {
            long tStart = System.currentTimeMillis();

            // last
            ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\432850581.dem");

            // TI3 final
            //ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\271145478.dem");
            
            Match match = new Match(idx);
            long tInit = System.currentTimeMillis() - tStart;
            System.out.println("init: " + tInit / 1000.0 + "s");
            
            tStart = System.currentTimeMillis();
            for (BidiIterator<Peek> i = idx.matchIterator(); i.hasNext();) {
                Peek p = i.next();
                p.apply(match);
            }
            long tMatch = System.currentTimeMillis() - tStart;
            System.out.println("match: " + tMatch / 1000.0 + "s");

            System.out.println("total: " + (tInit + tMatch) / 1000.0 + "s");
            
        } catch (Throwable e) {
            System.out.flush();
            throw e;
        }

    }

}
