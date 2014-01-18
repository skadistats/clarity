package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.model.GameEventDescriptor;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;

import com.dota2.proto.Netmessages.CSVCMsg_GameEventList;
import com.dota2.proto.Netmessages.CSVCMsg_GameEventList.descriptor_t;
import com.dota2.proto.Netmessages.CSVCMsg_GameEventList.key_t;

public class SvcGameEventListHandler implements Handler<CSVCMsg_GameEventList> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_GameEventList message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        for (descriptor_t d : message.getDescriptorsList()) {
            String[] keys = new String[d.getKeysCount()]; 
            for (int i = 0; i < d.getKeysCount(); i++) {
                key_t k = d.getKeys(i);
                keys[i] = k.getName();
            }
            match.getGameEventDescriptors().add(new GameEventDescriptor(
                d.getEventid(),
                d.getName(),
                keys
            ));
        }
    }

}
