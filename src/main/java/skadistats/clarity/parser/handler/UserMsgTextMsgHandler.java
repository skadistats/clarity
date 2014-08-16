package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.model.UserMessage;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.Usermessages.CUserMsg_TextMsg;

@RegisterHandler(CUserMsg_TextMsg.class)
public class UserMsgTextMsgHandler implements Handler<CUserMsg_TextMsg> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CUserMsg_TextMsg message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} TEXT_MSG",
            match.getReplayTimeAsString()
        );
        match.getUserMessages().add(UserMessage.build(message, match));
    }

}
