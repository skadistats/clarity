package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.Netmessages.CSVCMsg_ServerInfo;

@RegisterHandler(CSVCMsg_ServerInfo.class)
public class SvcServerInfoHandler implements Handler<CSVCMsg_ServerInfo> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_ServerInfo message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        match.setTickInterval(message.getTickInterval());
    }

}
