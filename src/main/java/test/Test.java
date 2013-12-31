package test;

import java.io.FileInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.DemoInputStream;
import clarity.parser.Peek;
import clarity.parser.ReplayFile;
import clarity.parser.ReplayIndex;

import com.google.protobuf.CodedInputStream;

public class Test {

    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("TEST");

        // last
        //ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\432850581.dem");

        // TI3 final
        //ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\271145478.dem");
        
        //ReplayIndex idx = ReplayFile.indexForFile("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\37633163.dem");
        
        long tIndex = System.currentTimeMillis() - tStart;
        log.info("index built in {}s", tIndex / 1000.0);

        tStart = System.currentTimeMillis();
        //Match match = new Match(idx);
        long tPrologue = System.currentTimeMillis() - tStart;
        log.info("prologue applied in {}s", tPrologue / 1000.0);
        
        tStart = System.currentTimeMillis();

        
        
        CodedInputStream s = CodedInputStream.newInstance(new FileInputStream("C:\\Program Files (x86)\\Steam\\steamapps\\common\\dota 2 beta\\dota\\replays\\37633163.dem"));
        s.setSizeLimit(Integer.MAX_VALUE);
        s.skipRawBytes(12);
        DemoInputStream di = new DemoInputStream(s);
        Peek p;
        do {
            p = di.read();
            if (p != null) {
                p.trace();    
            }
            
        } while (p != null);
        
        
        
//        for (Iterator<Peek> i = idx.matchIteratorForTicks(0, idx.getLastTick(), PacketType.DELTA); i.hasNext();) {
//            Peek p = i.next();
//            p.apply(match);
//        }        

//        for (int c = 0; c < 100; c++) {
//            long tSkip = System.currentTimeMillis();
//            int t = (int)(Math.random() * idx.getLastTick());
//            int v = 0;
//            for (Iterator<Peek> i = idx.skipToIterator(t); i.hasNext();) {
//                Peek p = i.next();
//                p.apply(match);
//                v++;
//            }
//            log.info("restored to peek {} in {}s, visited {} packets", t, (System.currentTimeMillis() - tSkip) / 1000.0, v);
//        }

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("match applied in {}s", tMatch / 1000.0);
        log.info("total time taken: {}s", (tIndex + tPrologue + tMatch) / 1000.0);

    }

}
