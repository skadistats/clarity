package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;
import skadistats.clarity.wire.proto.Netmessages.CSVCMsg_ClassInfo;

@RegisterHandler(CSVCMsg_ClassInfo.class)
public class SvcClassInfoHandler implements Handler<CSVCMsg_ClassInfo> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_ClassInfo message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        //TODO Handle
    }

}
