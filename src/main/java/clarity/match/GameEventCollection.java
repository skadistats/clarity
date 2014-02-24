package clarity.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import clarity.model.GameEvent;

import com.rits.cloning.Cloner;

public class GameEventCollection implements Cloneable {
    
    private static final Cloner CLONER = new Cloner();
    
    private final List<GameEvent> gameEvents = new ArrayList<GameEvent>();

    public void add(GameEvent event) {
        gameEvents.add(event);
    }
    
    public void clear() {
        gameEvents.clear();
    }
    
    public List<GameEvent> getAll() {
        return Collections.unmodifiableList(gameEvents);
    }
    
    @Override
    public GameEventCollection clone() {
       return CLONER.deepClone(this);
    }
    
}
