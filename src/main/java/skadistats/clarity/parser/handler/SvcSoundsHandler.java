package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.Netmessages.CSVCMsg_Sounds;

@RegisterHandler(CSVCMsg_Sounds.class)
public class SvcSoundsHandler implements Handler<CSVCMsg_Sounds> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_Sounds message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        //Handle
    }

}
