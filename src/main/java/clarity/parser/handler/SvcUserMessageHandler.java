package clarity.parser.handler;

import clarity.match.Match;
import clarity.model.UserMessageType;
import clarity.parser.Handler;

import com.dota2.proto.Networkbasetypes.CSVCMsg_UserMessage;
import com.google.protobuf.GeneratedMessage;

public class SvcUserMessageHandler implements Handler<CSVCMsg_UserMessage> {

    @Override
    public void apply(CSVCMsg_UserMessage message, Match match) {
        UserMessageType umt = UserMessageType.forId(message.getMsgType());
        if (umt == null || umt.getClazz() == null) {
            return;
        }
        GeneratedMessage decoded = umt.parseFrom(message.getMsgData());
        
    }

}
