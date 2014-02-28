package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;
import clarity.parser.RegisterHandler;

import com.dota2.proto.Netmessages.CSVCMsg_VoiceData;

@RegisterHandler(CSVCMsg_VoiceData.class)
public class SvcVoiceDataHandler implements Handler<CSVCMsg_VoiceData> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void apply(int peekTick, CSVCMsg_VoiceData message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
    }

}
