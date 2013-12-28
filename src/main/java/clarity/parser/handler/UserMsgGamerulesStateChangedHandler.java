package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.model.GameRulesStateType;
import clarity.parser.Handler;

import com.dota2.proto.DotaUsermessages.CDOTA_UM_GamerulesStateChanged;

public class UserMsgGamerulesStateChangedHandler implements Handler<CDOTA_UM_GamerulesStateChanged> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTA_UM_GamerulesStateChanged message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        log.debug("{} GAMERULES_STATE_CHANGED [state={}]",
            match.getReplayTimeAsString(),
            GameRulesStateType.forId(message.getState())
        );
    }

}
