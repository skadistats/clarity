package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.model.GameRulesStateType;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.DotaUsermessages.CDOTA_UM_GamerulesStateChanged;

@RegisterHandler(CDOTA_UM_GamerulesStateChanged.class)
public class UserMsgGamerulesStateChangedHandler implements Handler<CDOTA_UM_GamerulesStateChanged> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDOTA_UM_GamerulesStateChanged message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        log.debug("{} GAMERULES_STATE_CHANGED [state={}]",
            match.getReplayTimeAsString(),
            GameRulesStateType.forId(message.getState())
        );
    }

}
