package clarity.parser.handler;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DestroyLinearProjectile;

public class UserMsgDestroyLinearProjectileHandler implements Handler<CDOTAUserMsg_DestroyLinearProjectile> {

    @Override
    public void apply(CDOTAUserMsg_DestroyLinearProjectile message, Match match) {
        System.out.println(String.format("tick %s: DESTROY_LINEAR_PROJECTILE [handle=%s]",
            match.getTick(),
            message.getHandle()
        ));
    }

}
