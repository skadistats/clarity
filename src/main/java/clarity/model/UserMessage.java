package clarity.model;

import com.google.protobuf.GeneratedMessage;

public class UserMessage {

    private final UserMessageType type;
    private final GeneratedMessage message;
    
    public UserMessage(UserMessageType type, GeneratedMessage message) {
        this.type = type;
        this.message = message;
    }

    public UserMessageType getType() {
        return type;
    }

    public GeneratedMessage getMessage() {
        return message;
    }
    
}
