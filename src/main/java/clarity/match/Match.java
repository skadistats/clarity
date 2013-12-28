package clarity.match;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import clarity.iterator.BidiIterator;
import clarity.model.GameRulesStateType;
import clarity.parser.Peek;
import clarity.parser.ReplayIndex;

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
    
    private final ReplayIndex idx;
    
    private final DTClassCollection dtClasses = new DTClassCollection();
    private final StringTableCollection stringTables = new StringTableCollection();
    private final EntityCollection entities = new EntityCollection();
    private final GameEventDescriptorCollection gameEventDescriptors = new GameEventDescriptorCollection();
    private final GameEventCollection gameEvents = new GameEventCollection();
    private final ModifierCollection modifiers = new ModifierCollection();
    
    private GameRulesStateType state = GameRulesStateType.WAITING_FOR_LOADERS; 
    private int peekTick = 0;
    private int startTick = 0;
    private float tickInterval = 1.0f/30.0f;
    
    public Match(ReplayIndex idx) {
        this.idx = idx;
        for (BidiIterator<Peek> i = idx.prologueIterator(); i.hasNext();) {
            Peek p = i.next();
            //System.out.println(p);
            p.apply(this);
        }
        startTick = idx.get(GameRulesStateType.PLAYING.findOnIndex(idx)).getTick();
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

    public int getPeekTick() {
        return peekTick;
    }

    public void setPeekTick(int tick) {
        this.peekTick = tick;
    }
    
    public Duration getReplayTime() {
        return Duration.millis((long)(1000L * (peekTick) * tickInterval));
    }
    
    public Duration getGameTime() {
        return Duration.millis((long)(1000L * (peekTick -startTick) * tickInterval));
    }

    public String getReplayTimeAsString() {
        return GAMETIME_FORMATTER.print(getReplayTime().toPeriod());
    }

    public String getGameTimeAsString() {
        return GAMETIME_FORMATTER.print(getGameTime().toPeriod());
    }

    public float getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(float tickInterval) {
        this.tickInterval = tickInterval;
    }

}
