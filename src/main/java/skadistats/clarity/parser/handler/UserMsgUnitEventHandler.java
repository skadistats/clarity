package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.model.UserMessage;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_UnitEvent;

@RegisterHandler(CDOTAUserMsg_UnitEvent.class)
public class UserMsgUnitEventHandler implements Handler<CDOTAUserMsg_UnitEvent> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTAUserMsg_UnitEvent message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} UNIT_EVENT [type={}]",
            match.getReplayTimeAsString(),
            message.getMsgType()
        );
        match.getUserMessages().add(UserMessage.build(message, match));
    }

}
