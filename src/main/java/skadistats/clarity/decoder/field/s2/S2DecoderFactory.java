package skadistats.clarity.decoder.field.s2;

import skadistats.clarity.decoder.field.BoolDecoder;
import skadistats.clarity.decoder.field.IntDecoder;
import skadistats.clarity.decoder.field.StringDecoder;
import skadistats.clarity.model.s2.Field;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

public class S2DecoderFactory {

    private static final Map<String, MethodHandle> HANDLES = new HashMap<>();
    static {
        // Booleans
        HANDLES.put("bool", BoolDecoder.decode);

        // Unsigned ints
        HANDLES.put("uint8", IntDecoder.decodeUnsignedInt);
        HANDLES.put("uint16", IntDecoder.decodeUnsignedInt);
        HANDLES.put("uint32", IntDecoder.decodeUnsignedInt);
        HANDLES.put("uint64", IntDecoder.decodeUnsignedLong);

        // Signed ints
        HANDLES.put("int8", IntDecoder.decodeSignedInt);
        HANDLES.put("int16", IntDecoder.decodeSignedInt);
        HANDLES.put("int32", IntDecoder.decodeSignedInt);
        HANDLES.put("int64", IntDecoder.decodeSignedLong);

        // Floats
        HANDLES.put("float32", FloatDecoderFactory.createDecoder);
        HANDLES.put("CNetworkedQuantizedFloat", FloatDecoderFactory.createDecoder);
        HANDLES.put("QAngle", QAngleDecoderFactory.createDecoder);

        // Strings
        HANDLES.put("CUtlSymbolLarge", StringDecoder.decode);
        HANDLES.put("char", StringDecoder.decode);
        HANDLES.put("CUtlStringToken", IntDecoder.decodeUnsignedInt);

        // Enums
        HANDLES.put("gender_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("DamageOptions_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("RenderMode_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("RenderFx_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("attributeprovidertypes_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("CourierState_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("MoveCollide_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("MoveType_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("SolidType_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("SurroundingBoundsType_t", IntDecoder.decodeUnsignedInt);
        HANDLES.put("DOTA_SHOP_TYPE", IntDecoder.decodeUnsignedInt);
        HANDLES.put("DOTA_HeroPickState", IntDecoder.decodeUnsignedInt);

        // Handles
        HANDLES.put("CHandle", IntDecoder.decodeUnsignedInt);
        HANDLES.put("CGameSceneNodeHandle", IntDecoder.decodeUnsignedInt);
        HANDLES.put("CStrongHandle", IntDecoder.decodeUnsignedLong);

        // Colors
        HANDLES.put("Color", IntDecoder.decodeUnsignedInt);
        HANDLES.put("color32", IntDecoder.decodeUnsignedInt);

        // Pointers type subs
        HANDLES.put("CBodyComponent", BoolDecoder.decode);
        HANDLES.put("CEntityIdentity", BoolDecoder.decode);
        HANDLES.put("CPhysicsComponent", BoolDecoder.decode);
        HANDLES.put("CRenderComponent", BoolDecoder.decode);
        HANDLES.put("CDOTAGamerules", BoolDecoder.decode);
        HANDLES.put("CDOTAGameManager", BoolDecoder.decode);
        HANDLES.put("CDOTASpectatorGraphManager", BoolDecoder.decode);

        // Array type subs
        HANDLES.put("CDOTA_AbilityDraftAbilityState", IntDecoder.decodeUnsignedInt);
        HANDLES.put("C_DOTA_ItemStockInfo", IntDecoder.decodeUnsignedInt);
        HANDLES.put("CUtlVector", IntDecoder.decodeUnsignedInt);
        HANDLES.put("DOTA_PlayerChallengeInfo", IntDecoder.decodeUnsignedInt);
        HANDLES.put("m_SpeechBubbles", IntDecoder.decodeUnsignedInt);

        // Specials
        HANDLES.put("HSequence", IntDecoder.decodeIntMinusOne);
        HANDLES.put("Vector", VectorDecoderFactory.createDecoder);
    }

    public static MethodHandle createDecoder(Field f) {
        MethodHandle h = HANDLES.get(f.getType().getBaseType());
        if (h == null) {
            throw new RuntimeException("don't know how to create decoder for " + f.getType().getBaseType());
        }
        if (h.type().parameterType(0) == Field.class) {
            try {
                h = (MethodHandle) h.invokeExact(f);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return h;
    }

}
