package skadistats.clarity.parser.handler;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ChatEvent;
import com.google.protobuf.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.match.Match;
import skadistats.clarity.model.UserMessage;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

@RegisterHandler(CDOTAUserMsg_ChatEvent.class)
public class UserMsgChatEventHandler implements Handler<CDOTAUserMsg_ChatEvent> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void apply(int peekTick, CDOTAUserMsg_ChatEvent message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        if (log.isDebugEnabled()) {
            StringBuffer b = new StringBuffer();
            for (int n = 1; n <=6; n++) {
                Descriptors.FieldDescriptor d = CDOTAUserMsg_ChatEvent.getDescriptor().findFieldByName("playerid_" + n);
                if (message.hasField(d)) {
                    if (b.length() > 0) {
                        b.append(", ");
                    }
                    b.append(message.getField(d).toString());
                } else {
                    break;
                }
            }
            log.debug("{} CHAT_EVENT [type={}, value={}, player_ids={}]",
                match.getReplayTimeAsString(),
                message.getType(),
                message.getValue(),
                b.toString()
            );
        }
        match.getChatEvents().add(message);
        match.getUserMessages().add(UserMessage.build(message, match));
    }

}
