package skadistats.clarity.model;

import skadistats.clarity.io.Util;
import skadistats.clarity.model.engine.CsGoEngineType;
import skadistats.clarity.model.engine.DotaS1EngineType;
import skadistats.clarity.model.engine.DotaS2EngineType;

public enum EngineId {

    DOTA_S1("PBUFDEM\0", DotaS1EngineType.class),
    DOTA_S2("PBDEMS2\0", DotaS2EngineType.class),
    CSGO_S1("HL2DEMO\0", CsGoEngineType.class);

    public static EngineType typeForMagic(String magic) {
        for (EngineId et : values()) {
            if (et.magic.equals(magic)) {
                return et.newInstance();
            }
        }
        return null;
    }

    private final String magic;
    private final Class<? extends EngineType> implClass;

    EngineId(String magic, Class<? extends EngineType> implClass) {
        this.magic = magic;
        this.implClass = implClass;
    }

    public EngineType newInstance() {
        try {
            return implClass.getDeclaredConstructor(EngineId.class).newInstance(this);
        } catch (Exception e) {
            Util.uncheckedThrow(e);
            return null;
        }
    }

}
