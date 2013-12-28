package clarity.parser.handler;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DodgeTrackingProjectiles;

public class UserMsgDodgeTrackingProjectilesHandler implements Handler<CDOTAUserMsg_DodgeTrackingProjectiles> {

    @Override
    public void apply(CDOTAUserMsg_DodgeTrackingProjectiles message, Match match) {
        System.out.println(String.format("tick %s: DODGE_TRACKING_PROJECTILES [target=%s]",
            match.getPeekTick(),
            match.getEntities().getByIndex(message.getEntindex()).getDtClass().getDtName() 
        ));
    }

}
