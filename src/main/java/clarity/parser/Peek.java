package clarity.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;

import com.google.protobuf.GeneratedMessage;

public class Peek {

    private static final Logger log = LoggerFactory.getLogger(Peek.class);
    
    private final int id;
    private final int tick;
    private final int peekTick;
    private final boolean full;
    private final GeneratedMessage message;

    public Peek(int id, int tick, int peekTick, boolean full, GeneratedMessage message) {
        this.id = id;
        this.tick = tick;
        this.peekTick = peekTick;
        this.full = full;
        this.message = message;
    }
    
    public int getId() {
        return id;
    }

    public int getTick() {
        return tick;
    }

    public int getPeekTick() {
        return peekTick;
    }
    
    public boolean isFull() {
        return full;
    }

    public GeneratedMessage getMessage() {
        return message;
    }
    
    public void apply(Match match) {
        match.setTick(tick);
        HandlerRegistry.apply(tick, message, match);
        log.trace("id: {}, peekTick: {}, tick: {}, full: {}, mesageType: {}", id, peekTick, tick, full, message.getDescriptorForType().getName());
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
