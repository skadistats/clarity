package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CreateLinearProjectile;

public class UserMsgCreateLinearProjectileHandler implements Handler<CDOTAUserMsg_CreateLinearProjectile> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(CDOTAUserMsg_CreateLinearProjectile message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        log.debug("{} CREATE_LINEAR_PROJECTILE [handle={}, origin={}, type={}]",
            match.getReplayTimeAsString(),
            message.getHandle(),
            match.getEntities().getByIndex(message.getEntindex()).getDtClass().getDtName(), 
            match.getStringTables().forName("ParticleEffectNames").getNameByIndex(message.getParticleIndex())
        );
    }

}
