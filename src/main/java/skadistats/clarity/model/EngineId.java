package skadistats.clarity.model;

public enum EngineId {

    DOTA_S1(false),
    DOTA_S2(true),
    CSGO_S1(false),
    CSGO_S2(true);

    private final boolean entitySkipExtraVarint;

    EngineId(boolean entitySkipExtraVarint) {
        this.entitySkipExtraVarint = entitySkipExtraVarint;
    }

    public boolean isEntitySkipExtraVarint() {
        return entitySkipExtraVarint;
    }

}
