package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.model.UserMessage;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.Usermessages.CUserMsg_SayText2;

@RegisterHandler(CUserMsg_SayText2.class)
public class UserMsgSayText2Handler implements Handler<CUserMsg_SayText2> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CUserMsg_SayText2 message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        match.getUserMessages().add(UserMessage.build(message, match));
    }

}
