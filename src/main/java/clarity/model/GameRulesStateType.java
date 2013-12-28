package clarity.model;

public enum GameRulesStateType {

    INIT(1),
    WAITING_FOR_LOADERS(2),
    PICKING_A_HERO(3),
    PRE_GAME(4),
    PLAYING(5),
    POST_GAME(6);
    
    private int id;

    private GameRulesStateType(int id) {
        this.id = id;
    }
    
    public static GameRulesStateType forId(int id) {
        for (GameRulesStateType t : values()) {
            if (t.id == id) {
                return t;
            }
        }
        throw new RuntimeException("unknown GamerulesState " + id);
    }
    
}
