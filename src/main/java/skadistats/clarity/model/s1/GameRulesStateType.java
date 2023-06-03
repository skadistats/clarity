package skadistats.clarity.model.s1;


import skadistats.clarity.ClarityException;

public enum GameRulesStateType {

    WAITING_FOR_LOADERS(1),
    PICKING(2),
    START(3),
    PRE_GAME(4),
    PLAYING(5),
    POST_GAME(6);
    
    private final int id;

    GameRulesStateType(int id) {
        this.id = id;
    }
    
    public static GameRulesStateType forId(int id) {
        for (GameRulesStateType t : values()) {
            if (t.id == id) {
                return t;
            }
        }
        throw new ClarityException("unknown GamerulesState %d", id);
    }
    
}
