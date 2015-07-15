package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;
import skadistats.clarity.wire.s1.proto.Netmessages.CSVCMsg_VoiceData;

@RegisterHandler(CSVCMsg_VoiceData.class)
public class SvcVoiceDataHandler implements Handler<CSVCMsg_VoiceData> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void apply(int peekTick, CSVCMsg_VoiceData message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
    }

}
