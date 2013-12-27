package clarity.parser.handler;

import clarity.match.Match;
import clarity.model.Entity;
import clarity.model.UserMessage;
import clarity.model.UserMessageType;
import clarity.parser.Handler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CreateLinearProjectile;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DestroyLinearProjectile;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DodgeTrackingProjectiles;
import com.dota2.proto.Networkbasetypes.CSVCMsg_UserMessage;
import com.google.protobuf.GeneratedMessage;

public class SvcUserMessageHandler implements Handler<CSVCMsg_UserMessage> {

    @Override
    public void apply(CSVCMsg_UserMessage message, Match match) {
        UserMessageType umt = UserMessageType.forId(message.getMsgType());
        if (umt == null || umt.getClazz() == null) {
            return;
        }
        GeneratedMessage decoded = umt.parseFrom(message.getMsgData());
        match.getUserMessages().add(new UserMessage(umt, decoded));
        Entity e = null;
        switch(umt) {
            case CREATE_LINEAR_PROJECTILE:
                CDOTAUserMsg_CreateLinearProjectile clp = (CDOTAUserMsg_CreateLinearProjectile) decoded;
                System.out.println(String.format("tick %s: CREATE_LINEAR_PROJECTILE [handle=%s, origin=%s, type=%s]",
                    match.getTick(),
                    clp.getHandle(),
                    match.getEntities().get(clp.getEntindex()).getDtClass().getDtName(), 
                    match.getStringTables().forName("ParticleEffectNames").getNameByIndex(clp.getParticleIndex())
                ));
                break;
            case DESTROY_LINEAR_PROJECTILE:
                CDOTAUserMsg_DestroyLinearProjectile dlp = (CDOTAUserMsg_DestroyLinearProjectile) decoded;
                System.out.println(String.format("tick %s: DESTROY_LINEAR_PROJECTILE [handle=%s]",
                    match.getTick(),
                    dlp.getHandle()
                ));
                break;
            case DODGE_TRACKING_PROJECTILES:
                CDOTAUserMsg_DodgeTrackingProjectiles dtp = (CDOTAUserMsg_DodgeTrackingProjectiles) decoded;
                System.out.println(String.format("tick %s: DODGE_TRACKING_PROJECTILES [target=%s]",
                    match.getTick(),
                    match.getEntities().get(dtp.getEntindex()).getDtClass().getDtName() 
                ));
                
//            case OVERHEAD_EVENT:
//                Entity target = match.getEntities().get(((CDOTAUserMsg_OverheadEvent) decoded).getTargetEntindex());
//                Entity targetPlayer = match.getEntities().get(((CDOTAUserMsg_OverheadEvent) decoded).getTargetPlayerEntindex());
//                System.out.println(umt + ": target_player " + targetPlayer.getDtClass().getDtName() + ", target_ent " + target.getDtClass().getDtName() );
//                System.out.println(decoded);
            default:
        }
    }

}
