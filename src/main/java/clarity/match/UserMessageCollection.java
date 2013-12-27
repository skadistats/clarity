package clarity.match;

import java.util.ArrayList;
import java.util.List;

import clarity.model.UserMessage;

public class UserMessageCollection {
    
    private final List<UserMessage> userMessages = new ArrayList<UserMessage>();

    public void add(UserMessage event) {
        userMessages.add(event);
    }
    
    public void clear() {
        userMessages.clear();
    }
    
}
