package clarity.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;

import com.google.protobuf.GeneratedMessage;

public class Peek {

    private static final Logger log = LoggerFactory.getLogger(Peek.class);
    
    private final int tick;
    private final GeneratedMessage message;

    public Peek(int tick, GeneratedMessage message) {
        this.tick = tick;
        this.message = message;
    }

    public int getTick() {
        return tick;
    }

    public GeneratedMessage getMessage() {
        return message;
    }
    
    public void apply(Match match) {
        match.setPeekTick(tick);
        log.trace("peekTick: {}, netTick: {}, mesageType: {}", match.getPeekTick(), match.getServerTick(), message.getDescriptorForType().getName());
        HandlerRegistry.apply(tick, message, match);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Peek [tick=");
        builder.append(tick);
        builder.append(", type=");
        builder.append(message.getDescriptorForType().getName());
        builder.append(", size=");
        builder.append(message.getSerializedSize());
        builder.append("]");
        return builder.toString();
    }

}
