package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.Networkbasetypes.CNETMsg_Tick;

@RegisterHandler(CNETMsg_Tick.class)
public class NetTickHandler implements Handler<CNETMsg_Tick> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CNETMsg_Tick message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
    }

}
