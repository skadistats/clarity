package skadistats.clarity.match;

import skadistats.clarity.model.Entity;

import com.rits.cloning.Cloner;

public class Snapshot implements Cloneable {
    
    private static final Cloner CLONER = new Cloner();

    private final StringTableCollection stringTables;
    private final EntityCollection entities = new EntityCollection();
    private final GameEventCollection gameEvents = new GameEventCollection();
    private final ModifierCollection modifiers = new ModifierCollection();
    private final TempEntityCollection tempEntities = new TempEntityCollection();
    private final ChatEventCollection chatEvents = new ChatEventCollection();
    
    
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

    public GameEventCollection getGameEvents() {
        return gameEvents;
    }
    
    public ModifierCollection getModifiers() {
        return modifiers;
    }
    
    public TempEntityCollection getTempEntities() {
        return tempEntities;
    }
    
    public ChatEventCollection getChatEvents() {
        return chatEvents;
    }

    @Override
    public Snapshot clone() {
       return CLONER.deepClone(this);
    }
    
    public void clearTransientData() {
        gameEvents.clear();
        tempEntities.clear();
        chatEvents.clear();
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
