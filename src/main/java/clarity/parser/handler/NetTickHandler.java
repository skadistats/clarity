package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CNETMsg_Tick;

public class NetTickHandler implements Handler<CNETMsg_Tick> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(CNETMsg_Tick message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        //match.setTick(message.getTick());
        match.getGameEvents().clear();
    }

}
