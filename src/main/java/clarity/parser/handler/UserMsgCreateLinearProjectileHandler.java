package clarity.parser.handler;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CreateLinearProjectile;

public class UserMsgCreateLinearProjectileHandler implements Handler<CDOTAUserMsg_CreateLinearProjectile> {

    @Override
    public void apply(CDOTAUserMsg_CreateLinearProjectile message, Match match) {
        System.out.println(String.format("tick %s: CREATE_LINEAR_PROJECTILE [handle=%s, origin=%s, type=%s]",
            match.getTick(),
            message.getHandle(),
            match.getEntities().getByIndex(message.getEntindex()).getDtClass().getDtName(), 
            match.getStringTables().forName("ParticleEffectNames").getNameByIndex(message.getParticleIndex())
        ));
    }

}
