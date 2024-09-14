package skadistats.clarity.model;

public enum EngineId {

    DOTA_S1,
    DOTA_S2,
    CSGO_S1,
    CSGO_S2,
    DEADLOCK;

    public boolean isEntitySkipExtraVarint() {
        switch(this) {
            case DOTA_S2:
            case CSGO_S2:
            case DEADLOCK:
                return true;
            default:
                return false;
        }
    }

    public boolean canExtractGameVersion() {
        switch(this) {
            case DOTA_S2:
            case CSGO_S2:
            case DEADLOCK:
                return true;
            default:
                return false;
        }
    }

}
