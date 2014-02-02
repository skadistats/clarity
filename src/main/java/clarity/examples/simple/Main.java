package clarity.examples.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.DemoFile;
import clarity.parser.DemoInputStreamIterator;
import clarity.parser.Profiles;

public class Main {

    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("simple");

        Match match = new Match();
        DemoInputStreamIterator iter = DemoFile.iteratorForFile(args[0], Profiles.ENTITIES);
        
        while(iter.hasNext()) {
            iter.next().apply(match);
        }

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
        
    }

}
