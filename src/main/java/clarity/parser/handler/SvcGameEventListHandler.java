package clarity.parser.handler;

import clarity.match.Match;
import clarity.model.GameEventDescriptor;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_GameEventList;
import com.dota2.proto.Netmessages.CSVCMsg_GameEventList.descriptor_t;

public class SvcGameEventListHandler implements Handler<CSVCMsg_GameEventList> {

    @Override
    public void apply(CSVCMsg_GameEventList message, Match match) {
        for (descriptor_t d : message.getDescriptorsList()) {
            match.getGameEventDescriptors().add(new GameEventDescriptor(d));
        }
    }

}
