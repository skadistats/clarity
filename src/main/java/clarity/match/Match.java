package clarity.match;

import clarity.iterator.BidiIterator;
import clarity.parser.Peek;
import clarity.parser.ReplayIndex;

public class Match {

    private final DTClassCollection dtClasses = new DTClassCollection();
    private final StringTableCollection stringTables = new StringTableCollection();
    private final EntityCollection entities = new EntityCollection();
    private final GameEventDescriptorCollection gameEventDescriptors = new GameEventDescriptorCollection();
    private final GameEventCollection gameEvents = new GameEventCollection();
    private final UserMessageCollection userMessages = new UserMessageCollection();
    private int tick;
    
    public Match(ReplayIndex idx) {
        for (BidiIterator<Peek> i = idx.prologueIterator(); i.hasNext();) {
            Peek p = i.next();
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

    public UserMessageCollection getUserMessages() {
        return userMessages;
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }
    
    
    
    
    
    

}
