package clarity.match;

import com.rits.cloning.Cloner;

public class Snapshot implements Cloneable {
    
    private static final Cloner CLONER = new Cloner();

    private final StringTableCollection stringTables;
    private final EntityCollection entities = new EntityCollection();
    private final GameEventCollection gameEvents = new GameEventCollection();
    private final ModifierCollection modifiers = new ModifierCollection();

    public Snapshot() {
        this(new StringTableCollection());
    }
    
    public Snapshot(StringTableCollection initialStringTables) {
        this.stringTables = initialStringTables;
    }
    
    public StringTableCollection getStringTables() {
        return stringTables;
    }

    public EntityCollection getEntities() {
        return entities;
    }

    public GameEventCollection getGameEvents() {
        return gameEvents;
    }
    
    public ModifierCollection getModifiers() {
        return modifiers;
    }
    
    @Override
    public Snapshot clone() {
       return CLONER.deepClone(this);
    }

}
