package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;
import clarity.parser.RegisterHandler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DodgeTrackingProjectiles;

@RegisterHandler(CDOTAUserMsg_DodgeTrackingProjectiles.class)
public class UserMsgDodgeTrackingProjectilesHandler implements Handler<CDOTAUserMsg_DodgeTrackingProjectiles> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTAUserMsg_DodgeTrackingProjectiles message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} DODGE_TRACKING_PROJECTILES [target={}]",
            match.getReplayTimeAsString(),
            match.getEntities().getByIndex(message.getEntindex()).getDtClass().getDtName() 
        );
    }

}
