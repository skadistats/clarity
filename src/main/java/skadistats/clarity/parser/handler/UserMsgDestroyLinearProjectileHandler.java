package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.model.UserMessage;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DestroyLinearProjectile;

@RegisterHandler(CDOTAUserMsg_DestroyLinearProjectile.class)
public class UserMsgDestroyLinearProjectileHandler implements Handler<CDOTAUserMsg_DestroyLinearProjectile> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTAUserMsg_DestroyLinearProjectile message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} DESTROY_LINEAR_PROJECTILE [handle={}]",
            match.getReplayTimeAsString(),
            message.getHandle()
        );
        match.getUserMessages().add(UserMessage.build(message, match));
    }

}
