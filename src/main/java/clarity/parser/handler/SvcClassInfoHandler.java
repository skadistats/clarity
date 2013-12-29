package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;

import com.dota2.proto.Netmessages.CSVCMsg_ClassInfo;

public class SvcClassInfoHandler implements Handler<CSVCMsg_ClassInfo> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void apply(int peekTick, CSVCMsg_ClassInfo message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
    }

}
