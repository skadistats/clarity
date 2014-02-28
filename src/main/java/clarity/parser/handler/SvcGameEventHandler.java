package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.model.GameEvent;
import clarity.model.GameEventDescriptor;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;
import clarity.parser.RegisterHandler;

import com.dota2.proto.Networkbasetypes.CSVCMsg_GameEvent;
import com.dota2.proto.Networkbasetypes.CSVCMsg_GameEvent.key_t;

@RegisterHandler(CSVCMsg_GameEvent.class)
public class SvcGameEventHandler implements Handler<CSVCMsg_GameEvent> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_GameEvent message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
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
        log.debug(e.toString());
        match.getGameEvents().add(e);
    }

}
