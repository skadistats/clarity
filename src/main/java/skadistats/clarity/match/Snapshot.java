package skadistats.clarity.match;

import com.rits.cloning.Cloner;
import skadistats.clarity.model.Entity;

public class Snapshot implements Cloneable {
    
    private static final Cloner CLONER = new Cloner();

    private final StringTableCollection stringTables;
    private final EntityCollection entities = new EntityCollection();
    private final GameEventCollection gameEvents = new GameEventCollection();
    private final ModifierCollection modifiers = new ModifierCollection();
    private final TempEntityCollection tempEntities = new TempEntityCollection();
    private final UserMessageCollection userMessages = new UserMessageCollection();
    
    
    
    private Entity gameRulesProxy;
    private Entity playerResource;

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

    public GameEventCollection getGameEvents() { return gameEvents; }
    
    public ModifierCollection getModifiers() {
        return modifiers;
    }
    
    public TempEntityCollection getTempEntities() {
        return tempEntities;
    }
    
    public UserMessageCollection getUserMessages() {
        return userMessages;
    }

    @Override
    public Snapshot clone() {
       return CLONER.deepClone(this);
    }
    
    public void clearTransientData() {
        gameEvents.clear();
        tempEntities.clear();
        modifiers.clear();
        userMessages.clear();
        gameRulesProxy = null;
        playerResource = null;
    }
    
    public Entity getGameRulesProxy() {
        if (gameRulesProxy == null) {
            gameRulesProxy = entities.getByDtName("DT_DOTAGamerulesProxy");
        }
        return gameRulesProxy; 
    }
    
    public Entity getPlayerResource() {
        if (playerResource == null) {
            playerResource = entities.getByDtName("DT_DOTA_PlayerResource");
        }
        return playerResource; 
    }
    
}
