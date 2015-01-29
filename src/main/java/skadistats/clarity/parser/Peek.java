package skadistats.clarity.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;

import com.google.protobuf.GeneratedMessage;

public class Peek {

    private static final Logger log = LoggerFactory.getLogger(Peek.class);
    
    private final int id;
    private int tick;
    private final boolean full;
    private final GeneratedMessage message;

    public Peek(int id, int tick, boolean full, GeneratedMessage message) {
        this.id = id;
        this.tick = tick;
        this.full = full;
        this.message = message;
    }
    
    public int getId() {
        return id;
    }

    public int getTick() {
        return tick;
    }

    public boolean isFull() {
        return full;
    }
    
    public GeneratedMessage getMessage() {
        return message;
    }

    public void applySkew(int skew) {
        tick -= skew;
    }
    
    public void apply(Match match) {
        trace();
        boolean newTick = match.getTick() != tick;
        match.setTick(tick);
        if (newTick) {
            match.startTick();
        }
        HandlerRegistry.apply(tick, message, match);
    }
    
    public void trace() {
        if (log.isTraceEnabled()) {
            log.trace("id: {}, tick: {}, full: {}, messageType: {}", id, tick, full, message.getDescriptorForType().getName());
        }
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
