package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ChatEvent;

@RegisterHandler(CDOTAUserMsg_ChatEvent.class)
public class UserMsgChatEventHandler implements Handler<CDOTAUserMsg_ChatEvent> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTAUserMsg_ChatEvent message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} CHAT_EVENT [type={}]",
            match.getReplayTimeAsString(),
            message.getType()
        );
        match.getChatEvents().add(message);
    }

}
