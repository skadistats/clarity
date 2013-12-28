package clarity.parser.handler;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_ServerInfo;

public class SvcServerInfoHandler implements Handler<CSVCMsg_ServerInfo> {

    @Override
    public void apply(CSVCMsg_ServerInfo message, Match match) {
        //System.out.println(message);
        match.setTickInterval(message.getTickInterval());
    }

}
