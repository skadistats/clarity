package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DestroyLinearProjectile;

public class UserMsgDestroyLinearProjectileHandler implements Handler<CDOTAUserMsg_DestroyLinearProjectile> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTAUserMsg_DestroyLinearProjectile message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} DESTROY_LINEAR_PROJECTILE [handle={}]",
            match.getReplayTimeAsString(),
            message.getHandle()
        );
    }

}
