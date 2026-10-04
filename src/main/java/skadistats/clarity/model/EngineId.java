package skadistats.clarity.model;

/**
 * The game/engine combination a replay was recorded with.
 */
public enum EngineId {

    /** Dota 2, Source 1 engine. */
    DOTA_S1,
    /** Dota 2, Source 2 engine (Reborn). */
    DOTA_S2,
    /** Counter-Strike: Global Offensive, Source 1 engine. */
    CSGO,
    /** Counter-Strike 2, Source 2 engine. */
    CS2,
    /** Deadlock, Source 2 engine. */
    DEADLOCK;

    /**
     * @return true for the Source 2 engines, which use spawn groups
     */
    public boolean hasSpawnGroups() {
        switch(this) {
            case DOTA_S2:
            case CS2:
            case DEADLOCK:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return true if the game version can be read from the replay's server info (the Source 2 engines)
     */
    public boolean canExtractGameVersion() {
        switch(this) {
            case DOTA_S2:
            case CS2:
            case DEADLOCK:
                return true;
            default:
                return false;
        }
    }

}
