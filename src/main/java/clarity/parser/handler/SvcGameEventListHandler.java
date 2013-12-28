package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.model.GameEventDescriptor;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_GameEventList;
import com.dota2.proto.Netmessages.CSVCMsg_GameEventList.descriptor_t;

public class SvcGameEventListHandler implements Handler<CSVCMsg_GameEventList> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(CSVCMsg_GameEventList message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        for (descriptor_t d : message.getDescriptorsList()) {
            match.getGameEventDescriptors().add(new GameEventDescriptor(d));
        }
    }

}
