package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;

import com.dota2.proto.Netmessages.CNETMsg_Tick;

public class NetTickHandler implements Handler<CNETMsg_Tick> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CNETMsg_Tick message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        match.getGameEvents().clear();
    }

}
