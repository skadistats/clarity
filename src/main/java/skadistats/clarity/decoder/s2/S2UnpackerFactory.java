package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.unpacker.*;
import skadistats.clarity.decoder.unpacker.factory.s2.FloatUnpackerFactory;
import skadistats.clarity.decoder.unpacker.factory.s2.QAngleUnpackerFactory;
import skadistats.clarity.decoder.unpacker.factory.s2.UnpackerFactory;
import skadistats.clarity.decoder.unpacker.factory.s2.VectorUnpackerFactory;
import skadistats.clarity.model.s2.Field;

import java.util.HashMap;
import java.util.Map;

public class S2UnpackerFactory {

    private static final Map<String, UnpackerFactory> FACTORIES = new HashMap<>();
    static {
        // Floats
        FACTORIES.put("float32", new FloatUnpackerFactory());
        FACTORIES.put("CNetworkedQuantizedFloat", new FloatUnpackerFactory());
        FACTORIES.put("QAngle", new QAngleUnpackerFactory());

        // Specials
        FACTORIES.put("Vector", new VectorUnpackerFactory());
    }

    private static final Map<String, Unpacker> UNPACKERS = new HashMap<>();
    static {
        // Booleans
        UNPACKERS.put("bool", new BoolUnpacker());

        // Unsigned ints
        UNPACKERS.put("uint8", new IntVarUnsignedUnpacker());
        UNPACKERS.put("uint16", new IntVarUnsignedUnpacker());
        UNPACKERS.put("uint32", new IntVarUnsignedUnpacker());
        UNPACKERS.put("uint64", new LongVarUnsignedUnpacker());

        // Signed ints
        UNPACKERS.put("int8", new IntVarSignedUnpacker());
        UNPACKERS.put("int16", new IntVarSignedUnpacker());
        UNPACKERS.put("int32", new IntVarSignedUnpacker());
        UNPACKERS.put("int64", new LongVarSignedUnpacker());

        // Strings
        UNPACKERS.put("CUtlSymbolLarge", new StringZeroTerminatedUnpacker());
        UNPACKERS.put("char", new StringZeroTerminatedUnpacker());
        UNPACKERS.put("CUtlStringToken", new IntVarUnsignedUnpacker());

        // Enums
        UNPACKERS.put("gender_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("DamageOptions_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("RenderMode_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("RenderFx_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("attributeprovidertypes_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("CourierState_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("MoveCollide_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("MoveType_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("SolidType_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("ShopItemViewMode_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("SurroundingBoundsType_t", new IntVarUnsignedUnpacker());
        UNPACKERS.put("DOTA_SHOP_TYPE", new IntVarUnsignedUnpacker());
        UNPACKERS.put("DOTA_HeroPickState", new IntVarUnsignedUnpacker());

        // Handles
        UNPACKERS.put("CHandle", new IntVarUnsignedUnpacker());
        UNPACKERS.put("CEntityHandle", new IntVarUnsignedUnpacker());
        UNPACKERS.put("CGameSceneNodeHandle", new IntVarUnsignedUnpacker());
        UNPACKERS.put("CStrongHandle", new LongVarUnsignedUnpacker());

        // Colors
        UNPACKERS.put("Color", new IntVarUnsignedUnpacker());
        UNPACKERS.put("color32", new IntVarUnsignedUnpacker());

        // Pointers type subs
        UNPACKERS.put("CBodyComponent", new BoolUnpacker());
        UNPACKERS.put("CEntityIdentity", new BoolUnpacker());
        UNPACKERS.put("CPhysicsComponent", new BoolUnpacker());
        UNPACKERS.put("CRenderComponent", new BoolUnpacker());
        UNPACKERS.put("CDOTAGamerules", new BoolUnpacker());
        UNPACKERS.put("CDOTAGameManager", new BoolUnpacker());
        UNPACKERS.put("CDOTASpectatorGraphManager", new BoolUnpacker());

        // Array type subs
        UNPACKERS.put("CDOTA_AbilityDraftAbilityState", new IntVarUnsignedUnpacker());
        UNPACKERS.put("C_DOTA_ItemStockInfo", new IntVarUnsignedUnpacker());
        UNPACKERS.put("CUtlVector", new IntVarUnsignedUnpacker());
        UNPACKERS.put("DOTA_PlayerChallengeInfo", new IntVarUnsignedUnpacker());
        UNPACKERS.put("m_SpeechBubbles", new IntVarUnsignedUnpacker());

        // Specials
        UNPACKERS.put("HSequence", new IntMinusOneUnpacker());
    }
    

    public static Unpacker createUnpacker(Field f, String type) {
        UnpackerFactory unpackerFactory = FACTORIES.get(type);
        if (unpackerFactory != null) {
            return unpackerFactory.createUnpacker(f);
        }
        Unpacker unpacker = UNPACKERS.get(type);
//        if (unpacker == null) {
//            throw new RuntimeException("don't know how to create unpacker for " + type);
//        }
        return unpacker;
    }
}
