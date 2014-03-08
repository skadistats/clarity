package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.ParticleAttachmentType;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ParticleManager;

@RegisterHandler(CDOTAUserMsg_ParticleManager.class)
public class UserMsgParticleManagerHandler implements Handler<CDOTAUserMsg_ParticleManager> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTAUserMsg_ParticleManager message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        switch(message.getType()) {
            case DOTA_PARTICLE_MANAGER_EVENT_CREATE:
                logCreate(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE:
                logUpdate(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_FORWARD:
                logUnhanded(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_ORIENTATION:
                logUpdateOrientation(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_FALLBACK:
                logUnhanded(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_ENT:
                logUpdateEnt(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_UPDATE_OFFSET:
                logUnhanded(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_DESTROY:
                logDestroy(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_DESTROY_INVOLVING:
                logUnhanded(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_RELEASE:
                logRelease(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_LATENCY:
                logUnhanded(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_SHOULD_DRAW:
                logUnhanded(message, match);
                break;
            case DOTA_PARTICLE_MANAGER_EVENT_FROZEN:
                logUnhanded(message, match);
                break;
        }
        
    }
    
    private void logCreate(CDOTAUserMsg_ParticleManager message, Match match) {
        int entityHandle = message.getCreateParticle().getEntityHandle();
//        int entityIndex = Handle.indexForHandle(entityHandle);
//        int entitySerial = Handle.serialForHandle(entityHandle);
        Entity parent = match.getEntities().getByHandle(entityHandle);
        String name = match.getStringTables().forName("ParticleEffectNames").getNameByIndex(message.getCreateParticle().getParticleNameIndex());
        log.debug("{} {} [index={}, entity={}({}), effect={}, attach={}]",
            match.getReplayTimeAsString(),
            "PARTICLE_CREATE",
            message.getIndex(),
            entityHandle,
            parent == null ? "NOT_FOUND" : parent.getDtClass().getDtName(),
            name == null ? "NOT_FOUND" : name,
            message.getCreateParticle().getAttachType()
        );
        //log.debug(message.toString());
    }

    private void logUpdate(CDOTAUserMsg_ParticleManager message, Match match) {
        log.debug("{} {} [index={}, controlPoint={}, position=[{}, {}, {}]]",
            match.getReplayTimeAsString(),
            "PARTICLE_UPDATE",
            message.getIndex(),
            message.getUpdateParticle().getControlPoint(),
            message.getUpdateParticle().getPosition().getX(),
            message.getUpdateParticle().getPosition().getY(),
            message.getUpdateParticle().getPosition().getZ()
        );
        //log.debug(message.toString());
    }
    
    private void logUpdateOrientation(CDOTAUserMsg_ParticleManager message, Match match) {
        log.debug("{} {} [index={}, controlPoint={}, forward=[{}, {}, {}], right=[{}, {}, {}], up=[{}, {}, {}]]",
            match.getReplayTimeAsString(),
            "PARTICLE_UPDATE_ORIENT",
            message.getIndex(),
            message.getUpdateParticleOrient().getControlPoint(),
            message.getUpdateParticleOrient().getForward().getX(),
            message.getUpdateParticleOrient().getForward().getY(),
            message.getUpdateParticleOrient().getForward().getZ(),
            message.getUpdateParticleOrient().getRight().getX(),
            message.getUpdateParticleOrient().getRight().getY(),
            message.getUpdateParticleOrient().getRight().getZ(),
            message.getUpdateParticleOrient().getUp().getX(),
            message.getUpdateParticleOrient().getUp().getY(),
            message.getUpdateParticleOrient().getUp().getZ()
        );
        //log.debug(message.toString());
    }
    
    private void logUpdateEnt(CDOTAUserMsg_ParticleManager message, Match match) {
        int entityHandle = message.getUpdateParticleEnt().getEntityHandle();
        Entity parent = match.getEntities().getByHandle(entityHandle);
        log.debug("{} {} [index={}, entity={}({}), controlPoint={}, attachmentType={}, attachment={}, includeWearables={}]",
            match.getReplayTimeAsString(),
            "PARTICLE_UPDATE_ENT",
            message.getIndex(),
            entityHandle,
            parent == null ? "NOT_FOUND" : parent.getDtClass().getDtName(),
            message.getUpdateParticleEnt().getControlPoint(),
            ParticleAttachmentType.forId(message.getUpdateParticleEnt().getAttachType()),
            message.getUpdateParticleEnt().getAttachment(),
            message.getUpdateParticleEnt().getIncludeWearables()
        );
        //log.debug(message.toString());
    }
    
    private void logDestroy(CDOTAUserMsg_ParticleManager message, Match match) {
        log.debug("{} {} [index={}, immediately={}]",
            match.getReplayTimeAsString(),
            "PARTICLE_DESTROY",
            message.getIndex(),
            message.getDestroyParticle().getDestroyImmediately()
        );
        //log.debug(message.toString());
    }
    
    private void logRelease(CDOTAUserMsg_ParticleManager message, Match match) {
        log.debug("{} {} [index={}]",
            match.getReplayTimeAsString(),
            "PARTICLE_RELEASE",
            message.getIndex()
        );
        //log.debug(message.toString());
    }
    

    private void logUnhanded(CDOTAUserMsg_ParticleManager message, Match match) {
        log.debug(message.toString());
    }
    

}
