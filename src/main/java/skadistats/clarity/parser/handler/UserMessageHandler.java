package skadistats.clarity.parser.handler;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterMultiHandler;

@RegisterMultiHandler("CDOTAUserMsg_")
public class UserMessageHandler implements Handler<GeneratedMessage> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, GeneratedMessage message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        match.getUserMessages().add(message);
    }

}
