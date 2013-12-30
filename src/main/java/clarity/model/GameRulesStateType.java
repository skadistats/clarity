package clarity.model;

import com.dota2.proto.DotaUsermessages.CDOTA_UM_GamerulesStateChanged;

import clarity.parser.ReplayIndex;

public enum GameRulesStateType {

    WAITING_FOR_LOADERS(1),
    PICKING(2),
    START(3),
    PRE_GAME(4),
    PLAYING(5),
    POST_GAME(6);
    
    private int id;

    private GameRulesStateType(int id) {
        this.id = id;
    }
    
    public int findOnIndex(ReplayIndex idx) {
        int i = -1;
        int c = id - 1;
        while (c > 0) {
            i = idx.nextIndexOf(CDOTA_UM_GamerulesStateChanged.class, i + 1);
            c--;
        }
        return i == -1 ? 0 : i;
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
