package clarity.parser.handler;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CNETMsg_Tick;

public class NetTickHandler implements Handler<CNETMsg_Tick> {

    @Override
    public void apply(CNETMsg_Tick message, Match match) {
        match.getGameEvents().clear();
        
    }

}
