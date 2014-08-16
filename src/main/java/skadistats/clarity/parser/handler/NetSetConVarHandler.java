package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.Netmessages.CNETMsg_SetConVar;

@RegisterHandler(CNETMsg_SetConVar.class)
public class NetSetConVarHandler implements Handler<CNETMsg_SetConVar> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CNETMsg_SetConVar message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} SET_CON_VAR",
            match.getReplayTimeAsString()
        );
        //TODO Handler
    }

}
