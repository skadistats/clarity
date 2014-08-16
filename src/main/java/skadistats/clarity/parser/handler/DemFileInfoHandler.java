package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.Demo.CDemoFileInfo;

@RegisterHandler(CDemoFileInfo.class)
public class DemFileInfoHandler implements Handler<CDemoFileInfo> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDemoFileInfo message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        //TODO Handle
    }

}
