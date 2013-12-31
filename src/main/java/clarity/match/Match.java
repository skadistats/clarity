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
    
    private final DTClassCollection dtClasses = new DTClassCollection();
    private final StringTableCollection stringTables = new StringTableCollection();
    private final EntityCollection entities = new EntityCollection();
    private final GameEventDescriptorCollection gameEventDescriptors = new GameEventDescriptorCollection();
    private final GameEventCollection gameEvents = new GameEventCollection();
    private final ModifierCollection modifiers = new ModifierCollection();
    
    private GameRulesStateType state = GameRulesStateType.WAITING_FOR_LOADERS; 
    private int tick = 0;
    private float tickInterval = 1.0f/30.0f;
    
    public Match(Iterator<Peek> prologueIterator) {
        for (; prologueIterator.hasNext();) {
            Peek p = prologueIterator.next();
            p.apply(this);
        }
    }

    public DTClassCollection getDtClasses() {
        return dtClasses;
    }

    public StringTableCollection getStringTables() {
        return stringTables;
    }

    public EntityCollection getEntities() {
        return entities;
    }

    public GameEventDescriptorCollection getGameEventDescriptors() {
        return gameEventDescriptors;
    }

    public GameEventCollection getGameEvents() {
        return gameEvents;
    }
    
    public ModifierCollection getModifiers() {
        return modifiers;
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }
    
    public Duration getReplayTime() {
        return Duration.millis((long)(1000L * (tick) * tickInterval));
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
