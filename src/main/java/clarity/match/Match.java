
package clarity.match;

import java.util.Iterator;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import clarity.model.GameRulesStateType;
import clarity.parser.Peek;

public class Match {

    private static final PeriodFormatter GAMETIME_FORMATTER = new PeriodFormatterBuilder()
        .minimumPrintedDigits(2)
        .printZeroAlways()
        .appendMinutes()
        .appendLiteral(":")
        .appendSeconds()
        .appendLiteral(".")
        .appendMillis3Digit()
        .toFormatter();
    
    // info from the prologue
    private final DTClassCollection dtClasses = new DTClassCollection();
    private final GameEventDescriptorCollection gameEventDescriptors = new GameEventDescriptorCollection();
    private StringTableCollection prologueStringTables;
    private float tickInterval = 1.0f/30.0f;

    // current information
    private Snapshot current = new Snapshot();
    private GameRulesStateType state = GameRulesStateType.WAITING_FOR_LOADERS; 
    private int tick = 0;

    public Match() {
    }
    
    public Match(Iterator<Peek> prologueIterator) {
        apply(prologueIterator);
        prologueStringTables = current.getStringTables().clone();
    }
    
    public int apply(Iterator<Peek> peekIterator) {
        int v = -1;
        for (; peekIterator.hasNext(); v++) {
            Peek p = peekIterator.next();
            p.apply(this);
        }
        return v + 1;
    }
    
    public void reset() {
        current = new Snapshot(prologueStringTables);
        tick = 0;
        state = GameRulesStateType.WAITING_FOR_LOADERS;        
    }
    
    public Snapshot getSnapshot() {
        return current.clone();
    }
    
    public DTClassCollection getDtClasses() {
        return dtClasses;
    }
    
    public GameEventDescriptorCollection getGameEventDescriptors() {
        return gameEventDescriptors;
    }

    public StringTableCollection getStringTables() {
        return current.getStringTables();
    }

    public EntityCollection getEntities() {
        return current.getEntities();
    }

    public GameEventCollection getGameEvents() {
        return current.getGameEvents();
    }
    
    public ModifierCollection getModifiers() {
        return current.getModifiers();
    }
    
    public TempEntityCollection getTempEntities() {
        return current.getTempEntities();
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }
    
    public Duration getReplayTime() {
        return Duration.millis((long)(1000L * tick * tickInterval));
    }
    
    public String getReplayTimeAsString() {
        return GAMETIME_FORMATTER.print(getReplayTime().toPeriod());
    }

    public float getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(float tickInterval) {
        this.tickInterval = tickInterval;
    }

}
