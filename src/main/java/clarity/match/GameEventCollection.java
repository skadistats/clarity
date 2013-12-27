package clarity.match;

import java.util.ArrayList;
import java.util.List;

import clarity.model.GameEvent;

public class GameEventCollection {
    
    private final List<GameEvent> gameEvents = new ArrayList<GameEvent>();

    public void add(GameEvent event) {
        gameEvents.add(event);
    }
    
    public void clear() {
        gameEvents.clear();
    }
    
}
