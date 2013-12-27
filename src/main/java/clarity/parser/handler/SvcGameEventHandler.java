package clarity.parser.handler;

import clarity.match.Match;
import clarity.model.GameEvent;
import clarity.model.GameEventDescriptor;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_GameEvent;
import com.dota2.proto.Netmessages.CSVCMsg_GameEvent.key_t;

public class SvcGameEventHandler implements Handler<CSVCMsg_GameEvent> {

    @Override
    public void apply(CSVCMsg_GameEvent message, Match match) {
        GameEventDescriptor desc = match.getGameEventDescriptors().forId(message.getEventid());
        GameEvent e = new GameEvent(desc);
        for (int i = 0; i < message.getKeysCount(); i++) {
            key_t key = message.getKeys(i);
            Object value = null;
            switch(key.getType()) {
            case 1: 
                value = key.getValString();
                break;
            case 2: 
                value = key.getValFloat();
                break;
            case 3: 
                value = key.getValLong();
                break;
            case 4: 
                value = key.getValShort();
                break;
            case 5: 
                value = key.getValByte();
                break;
            case 6: 
                value = key.getValBool();
                break;
            case 7: 
                value = key.getValUint64();
                break;
            default:
                throw new RuntimeException("cannot handle game event key type " + key.getType());
            }
            e.set(i, value);
        }
        match.getGameEvents().add(e);
    }

}
