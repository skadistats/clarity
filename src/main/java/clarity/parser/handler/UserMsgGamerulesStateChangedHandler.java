package clarity.parser.handler;

import clarity.match.Match;
import clarity.model.GameRulesStateType;
import clarity.parser.Handler;

import com.dota2.proto.DotaUsermessages.CDOTA_UM_GamerulesStateChanged;

public class UserMsgGamerulesStateChangedHandler implements Handler<CDOTA_UM_GamerulesStateChanged> {

    @Override
    public void apply(CDOTA_UM_GamerulesStateChanged message, Match match) {
        System.out.println(String.format("%s GAMERULES_STATE_CHANGED [state=%s]",
            match.getReplayTimeAsString(),
            GameRulesStateType.forId(message.getState())
        ));
    }

}
