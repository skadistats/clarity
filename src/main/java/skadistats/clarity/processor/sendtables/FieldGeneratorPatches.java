package skadistats.clarity.processor.sendtables;

import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.SerializerId;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.GameVersionRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FieldGeneratorPatches {

    private static final Map<GameVersionRange, PatchFunc> PATCHES_DOTA_S2 = new LinkedHashMap<>();
    private static final Map<GameVersionRange, PatchFunc> PATCHES_CSGO_S2 = new LinkedHashMap<>();
    private static final Map<GameVersionRange, PatchFunc> PATCHES_DEADLOCK = new LinkedHashMap<>();

    static List<PatchFunc> getPatches(EngineId engineId, int gameVersion) {
        return switch (engineId) {
            case DOTA_S2 -> filterPatches(PATCHES_DOTA_S2, gameVersion);
            case CSGO_S2 -> filterPatches(PATCHES_CSGO_S2, gameVersion);
            case DEADLOCK -> filterPatches(PATCHES_DEADLOCK, gameVersion);
            default -> Collections.emptyList();
        };
    }

    private static List<PatchFunc> filterPatches(Map<GameVersionRange, PatchFunc> patches, int gameVersion) {
        List<PatchFunc> result = new ArrayList<>();
        for (var patchEntry : patches.entrySet()) {
            if (patchEntry.getKey().appliesTo(gameVersion)) {
                result.add(patchEntry.getValue());
            }
        }
        return result;
    }


    static {
        PATCHES_DOTA_S2.put(new GameVersionRange(null, 954),
            FieldGeneratorPatches::patchDotaS2EarlyBetaMana);
        PATCHES_DOTA_S2.put(new GameVersionRange(null, 990),
            FieldGeneratorPatches::patchDotaS2EarlyBetaFieldTypes);
        PATCHES_DOTA_S2.put(new GameVersionRange(1016, 1026),
            FieldGeneratorPatches::patchDotaS2EarlyBetaFixed64);
        PATCHES_DOTA_S2.put(new GameVersionRange(null, null),
            FieldGeneratorPatches::patchDotaS2RuneTime);
        PATCHES_DOTA_S2.put(new GameVersionRange(null, null),
            FieldGeneratorPatches::patchS2SimulationTime);

        PATCHES_CSGO_S2.put(new GameVersionRange(null, null),
            FieldGeneratorPatches::patchS2SimulationTime);

        PATCHES_DEADLOCK.put(new GameVersionRange(null, null),
            FieldGeneratorPatches::patchS2SimulationTime);
    }


    private static void patchDotaS2EarlyBetaMana(SerializerId serializerId, FieldGenerator.FieldData field) {
        switch (field.name) {
            case "m_flMana":
            case "m_flMaxMana":
                var up = field.decoderProperties;
                if (up.highValue == 3.4028235E38f) {
                    up.lowValue = null;
                    up.highValue = 8192.0f;
                }
        }
    }

    private static final SerializerId SID_PITCH_YAW = new SerializerId("CBodyComponentBaseAnimatingOverlay", 3);

    private static void patchDotaS2EarlyBetaFieldTypes(SerializerId serializerId, FieldGenerator.FieldData field) {
        switch (field.name) {
            case "dirPrimary":
            case "localSound":
            case "m_attachmentPointBoneSpace":
            case "m_attachmentPointRagdollSpace":
            case "m_flElasticity":
            case "m_location":
            case "m_poolOrigin":
            case "m_ragPos":
            case "m_vecEndPos":
            case "m_vecEyeExitEndpoint":
            case "m_vecGunCrosshair":
            case "m_vecLadderDir":
            case "m_vecPlayerMountPositionBottom":
            case "m_vecPlayerMountPositionTop":
            case "m_viewtarget":
            case "m_WorldMaxs":
            case "m_WorldMins":
            case "origin":
            case "vecExtraLocalOrigin":
            case "vecLocalOrigin":
                field.decoderProperties.encoderType = "coord";
                break;

            case "angExtraLocalAngles":
            case "angLocalAngles":
            case "m_angInitialAngles":
            case "m_ragAngles":
            case "m_vLightDirection":
                field.decoderProperties.encoderType = "QAngle";
                break;

            case "m_vecLadderNormal":
                field.decoderProperties.encoderType = "normal";
                break;

            case "m_angRotation":
                field.decoderProperties.encoderType = SID_PITCH_YAW.equals(serializerId) ? "qangle_pitch_yaw" : "QAngle";
                break;
        }
    }

    private static void patchDotaS2EarlyBetaFixed64(SerializerId serializerId, FieldGenerator.FieldData field) {
        switch (field.name) {
            case "m_bWorldTreeState":
            case "m_ulTeamLogo":
            case "m_ulTeamBaseLogo":
            case "m_ulTeamBannerLogo":
            case "m_iPlayerIDsInControl":
            case "m_bItemWhiteList":
            case "m_iPlayerSteamID":
                field.decoderProperties.encoderType = "fixed64";
        }
    }

    private static void patchDotaS2RuneTime(SerializerId serializerId, FieldGenerator.FieldData field) {
        switch (field.name) {
            case "m_flRuneTime":
                var up = field.decoderProperties;
                if (up.highValue == Float.MAX_VALUE && up.lowValue == -Float.MAX_VALUE) {
                    up.lowValue = null;
                    up.highValue = null;
                }
        }
    }

    private static void patchS2SimulationTime(SerializerId serializerId, FieldGenerator.FieldData field) {
        switch (field.name) {
            case "m_flSimulationTime":
            case "m_flAnimTime":
                field.fieldType = FieldType.forString("uint32");
        }
    }

    interface PatchFunc {
        void execute(SerializerId serializerId, FieldGenerator.FieldData field);
    }

}
