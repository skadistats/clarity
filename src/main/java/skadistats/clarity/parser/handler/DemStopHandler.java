package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;
import skadistats.clarity.wire.s1.proto.Demo.CDemoStop;

@RegisterHandler(CDemoStop.class)
public class DemStopHandler implements Handler<CDemoStop> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDemoStop message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        //TODO Handle
    }

}
