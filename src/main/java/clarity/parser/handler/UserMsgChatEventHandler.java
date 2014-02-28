package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ChatEvent;

public class UserMsgChatEventHandler implements Handler<CDOTAUserMsg_ChatEvent> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTAUserMsg_ChatEvent message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} CHAT_EVENT [state={}]",
            match.getReplayTimeAsString(),
            message.getType()
        );
    }

}
