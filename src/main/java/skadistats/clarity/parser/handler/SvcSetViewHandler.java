package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;
import skadistats.clarity.wire.proto.Netmessages.CSVCMsg_SetView;

@RegisterHandler(CSVCMsg_SetView.class)
public class SvcSetViewHandler implements Handler<CSVCMsg_SetView> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_SetView message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        //TODO Handle
    }

}
