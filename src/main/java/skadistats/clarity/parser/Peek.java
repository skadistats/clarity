package skadistats.clarity.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;

import com.google.protobuf.GeneratedMessage;

public class Peek {

    public static enum BorderType {
        NONE,
        PEEK,
        NET,
        BOTH;
        
        public BorderType addPeekTickBorder() {
            return BorderType.values()[this.ordinal() | 1];
        }

        public BorderType addNetTickBorder() {
            return BorderType.values()[this.ordinal() | 2];
        }
        
        public boolean isPeekTickBorder() {
            return (ordinal() & 1) != 0;
        }

        public boolean isNetTickBorder() {
            return (ordinal() & 1) != 0;
        }
    }
    
    private static final Logger log = LoggerFactory.getLogger(Peek.class);
    
    private final int id;
    private int tick;
    private final int peekTick;
    private final boolean full;
    private final BorderType border;
    private final GeneratedMessage message;

    public Peek(int id, int tick, int peekTick, boolean full, BorderType border, GeneratedMessage message) {
        this.id = id;
        this.tick = tick;
        this.peekTick = peekTick;
        this.full = full;
        this.border = border;
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

    public void applySkew(int skew) {
        tick -= skew;
    }
    
    public BorderType getBorder() {
        return border;
    }
    
    public void apply(Match match) {
        trace();
        match.setTick(tick);
        if (border.isPeekTickBorder()) {
            match.startTick();
        }
        HandlerRegistry.apply(tick, message, match);
    }
    
    public void trace() {
        if (log.isTraceEnabled()) {
            log.trace("id: {}, peekTick: {}, tick: {}, full: {}, border: {}, messageType: {}", id, peekTick, tick, full, border, message.getDescriptorForType().getName());
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
