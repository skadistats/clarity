package clarity.parser.handler;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.DotaUsermessages.CDOTA_UM_GamerulesStateChanged;

public class UserMsgGamerulesStateChangedHandler implements Handler<CDOTA_UM_GamerulesStateChanged> {

    @Override
    public void apply(CDOTA_UM_GamerulesStateChanged message, Match match) {
        System.out.println(String.format("tick %s: GAMERULES_STATE_CHANGED [state=%s]",
            match.getTick(),
            message.getState()
        ));
    }

}
