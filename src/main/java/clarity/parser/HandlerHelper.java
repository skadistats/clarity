package clarity.parser;

import org.slf4j.Logger;

import com.google.protobuf.GeneratedMessage;

public class HandlerHelper {

    public static void traceMessage(Logger log, int peekTick, GeneratedMessage message) {
        log.trace("peek: {} {}\n{}", peekTick, message.getClass().getSimpleName(), message);
    }
    
}
