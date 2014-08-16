package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.model.UserMessage;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_SendRoshanPopup;

@RegisterHandler(CDOTAUserMsg_SendRoshanPopup.class)
public class UserMsgSendRoshanPopupHandler implements Handler<CDOTAUserMsg_SendRoshanPopup> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTAUserMsg_SendRoshanPopup message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} SEND_ROSHAN_POPUP",
            match.getReplayTimeAsString()
        );
        match.getUserMessages().add(UserMessage.build(message, match));
    }

}
