package skadistats.clarity.processor.sendtables;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.s2.S2DTClass;
import skadistats.clarity.decoder.s2.Serializer;
import skadistats.clarity.decoder.s2.SerializerId;
import skadistats.clarity.decoder.s2.field.*;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.BuildNumberRange;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s2.proto.S2NetMessages;

import java.io.IOException;
import java.util.*;

@Provides(value = OnDTClass.class, engine = EngineType.SOURCE2)
public class S2DTClassEmitter {

    private static final Set<String> POINTERS = new HashSet<>();
    static {
        POINTERS.add("CBodyComponent");
        POINTERS.add("CEntityIdentity");
        POINTERS.add("CPhysicsComponent");
        POINTERS.add("CRenderComponent");
        POINTERS.add("CDOTAGamerules");
        POINTERS.add("CDOTAGameManager");
        POINTERS.add("CDOTASpectatorGraphManager");
        POINTERS.add("CPlayerLocalData");
    }

    private static final Map<String, Integer> ITEM_COUNTS = new HashMap<>();
    static {
        ITEM_COUNTS.put("MAX_ITEM_STOCKS", 8);
        ITEM_COUNTS.put("MAX_ABILITY_DRAFT_ABILITIES", 48);
    }

    private FieldType createFieldType(String type) {
        return new FieldType(type);
    }

    private Field createField(FieldProperties properties) {
        if (properties.getSerializer() != null) {
            if (POINTERS.contains(properties.getType().getBaseType())) {
                return new FixedSubTableField(properties);
            } else {
                return new VarSubTableField(properties);
            }
        }
        String elementCount = properties.getType().getElementCount();
        if (elementCount != null && !"char".equals(properties.getType().getBaseType())) {
            Integer countAsInt = ITEM_COUNTS.get(elementCount);
            if (countAsInt == null) {
                countAsInt = Integer.valueOf(elementCount);
            }
            return new FixedArrayField(properties, countAsInt);
        }
        if ("CUtlVector".equals(properties.getType().getBaseType())) {
            return new VarArrayField(properties);
        }
        return new SimpleField(properties);
    }

    @OnMessage(Demo.CDemoSendTables.class)
    public void onSendTables(Context ctx, Demo.CDemoSendTables sendTables) throws IOException {
        CodedInputStream cis = CodedInputStream.newInstance(ZeroCopy.extract(sendTables.getData()));
        S2NetMessages.CSVCMsg_FlattenedSerializer protoMessage = Packet.parse(
            S2NetMessages.CSVCMsg_FlattenedSerializer.class,
            ZeroCopy.wrap(cis.readRawBytes(cis.readRawVarint32()))
        );

        Map<SerializerId, Serializer> serializers = new HashMap<>();
        Map<String, FieldType> fieldTypes = new HashMap<>();
        Field[] fields = new Field[protoMessage.getFieldsCount()];
        ArrayList<Field> currentFields = new ArrayList<>(128);
        for (int si = 0; si < protoMessage.getSerializersCount(); si++) {
            S2NetMessages.ProtoFlattenedSerializer_t protoSerializer = protoMessage.getSerializers(si);
            currentFields.clear();
            for (int fi : protoSerializer.getFieldsIndexList()) {
                Field field = fields[fi];
                if (field == null) {
                    SerializerField protoField = new SerializerField(protoMessage.getSymbols(protoSerializer.getSerializerNameSym()), protoMessage, protoMessage.getFields(fi));
                    for (Map.Entry<BuildNumberRange, PatchFunc> patchEntry : PATCHES.entrySet()) {
                        if (patchEntry.getKey().appliesTo(ctx.getBuildNumber())) {
                            patchEntry.getValue().execute(protoField);
                        }
                    }
                    FieldType fieldType = fieldTypes.get(protoField.varType);
                    if (fieldType == null) {
                        fieldType = createFieldType(protoField.varType);
                        fieldTypes.put(protoField.varType, fieldType);
                    }
                    Serializer fieldSerializer = null;
                    if (protoField.serializerName != null) {
                        fieldSerializer = serializers.get(
                            new SerializerId(protoField.serializerName, protoField.serializerVersion)
                        );
                    }
                    FieldProperties fieldProperties = new FieldProperties(
                        fieldType,
                        protoField.varName,
                        protoField.sendNode,
                        protoField.encodeFlags,
                        protoField.bitCount,
                        protoField.lowValue,
                        protoField.highValue,
                        fieldSerializer,
                        protoField.encoderType,
                        protoField.serializerType
                    );
                    field = createField(fieldProperties);
                    fields[fi] = field;
                }
                currentFields.add(field);
            }
            SerializerId sid = new SerializerId(
                protoMessage.getSymbols(protoSerializer.getSerializerNameSym()),
                protoSerializer.getSerializerVersion()
            );
            Serializer serializer = new Serializer(
                sid,
                currentFields.toArray(new Field[] {})
            );
            serializers.put(sid, serializer);
        }

        for (Serializer serializer : serializers.values()) {
            DTClass dtClass = new S2DTClass(serializer);
            ctx.createEvent(OnDTClass.class, DTClass.class).raise(dtClass);
        }

    }

    @OnMessage(Demo.CDemoClassInfo.class)
    public void onClassInfo(Context ctx, Demo.CDemoClassInfo message) {
        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
        for (Demo.CDemoClassInfo.class_t ct : message.getClassesList()) {
            DTClass dt = dtClasses.forDtName(ct.getNetworkName());
            dt.setClassId(ct.getClassId());
            dtClasses.byClassId.put(ct.getClassId(), dt);
        }
    }

    private static class SerializerField {
        private String parent;
        private String varName;
        private String varType;
        private String sendNode;
        private String serializerName;
        private Integer serializerVersion;
        private String encoderType;
        public String serializerType;
        private Integer encodeFlags;
        private Integer bitCount;
        private Float lowValue;
        private Float highValue;

        public SerializerField(String parent, S2NetMessages.CSVCMsg_FlattenedSerializer serializer, S2NetMessages.ProtoFlattenedSerializerField_t field) {
            this.parent = parent;
            this.varName = serializer.getSymbols(field.getVarNameSym());
            this.varType = serializer.getSymbols(field.getVarTypeSym());
            String sn = serializer.getSymbols(field.getSendNodeSym());
            this.sendNode = !"(root)".equals(sn) ? sn : null;
            this.serializerName = field.hasFieldSerializerNameSym() ? serializer.getSymbols(field.getFieldSerializerNameSym()) : null;
            this.serializerVersion = field.hasFieldSerializerVersion() ? field.getFieldSerializerVersion() : null;
            this.encoderType = field.hasVarEncoderSym() ? serializer.getSymbols(field.getVarEncoderSym()) : null;
            this.serializerType = null; // TODO: we hope to get this from the replay in the future
            this.encodeFlags = field.hasEncodeFlags() ? field.getEncodeFlags() : null;
            this.bitCount = field.hasBitCount() ? field.getBitCount() : null;
            this.lowValue = field.hasLowValue() ? field.getLowValue() : null;
            this.highValue = field.hasHighValue() ? field.getHighValue() : null;
//            if (encoder != null) {
//                System.out.format(
//                    "encoders.put(\"%s\", \"%s\");\n",
//                    parent + "." + varName,
//                    encoder
//                );
//            }
        }
    }

    private interface PatchFunc {
        void execute(SerializerField field);
    }

    private static final Map<BuildNumberRange, PatchFunc> PATCHES = new LinkedHashMap<>();
    static {
        PATCHES.put(new BuildNumberRange(null, 990), new PatchFunc() {
            Map<String, String> encoders = new HashMap<>(); {
                encoders.put("CBodyComponentBaseAnimating.m_angRotation", "QAngle");
                encoders.put("CBaseAnimating.m_flElasticity", "coord");
                encoders.put("CBaseAttributableItem.m_viewtarget", "coord");
                encoders.put("CBodyComponentPoint.m_angRotation", "QAngle");
                encoders.put("CPlayerLocalData.dirPrimary", "coord");
                encoders.put("CPlayerLocalData.origin", "coord");
                encoders.put("CPlayerLocalData.localSound", "coord");
                encoders.put("CBasePlayer.m_vecLadderNormal", "normal");
                encoders.put("CBeam.m_vecEndPos", "coord");
                encoders.put("CBodyComponentBaseAnimatingOverlay.m_angRotation", "qangle_pitch_yaw");
                encoders.put("CDOTA_BaseNPC_Barracks.m_angInitialAngles", "QAngle");
                encoders.put("CEnvDeferredLight.m_vLightDirection", "QAngle");
                encoders.put("CEnvWind.m_location", "coord");
                encoders.put("CFish.m_poolOrigin", "coord");
                encoders.put("CFogController.dirPrimary", "coord");
                encoders.put("CFuncLadder.m_vecLadderDir", "coord");
                encoders.put("CFuncLadder.m_vecPlayerMountPositionTop", "coord");
                encoders.put("CFuncLadder.m_vecPlayerMountPositionBottom", "coord");
                encoders.put("CPropVehicleDriveable.m_vecEyeExitEndpoint", "coord");
                encoders.put("CPropVehicleDriveable.m_vecGunCrosshair", "coord");
                encoders.put("CRagdollProp.m_ragPos", "coord");
                encoders.put("CRagdollProp.m_ragAngles", "QAngle");
                encoders.put("CRagdollPropAttached.m_attachmentPointBoneSpace", "coord");
                encoders.put("CRagdollPropAttached.m_attachmentPointRagdollSpace", "coord");
                encoders.put("CShowcaseSlot.vecLocalOrigin", "coord");
                encoders.put("CShowcaseSlot.angLocalAngles", "QAngle");
                encoders.put("CShowcaseSlot.vecExtraLocalOrigin", "coord");
                encoders.put("CShowcaseSlot.angExtraLocalAngles", "QAngle");
                encoders.put("CWorld.m_WorldMins", "coord");
                encoders.put("CWorld.m_WorldMaxs", "coord");
            }
            @Override
            public void execute(SerializerField field) {
                field.encoderType = encoders.get(field.parent + "." + field.varName);
            }
        });
        PATCHES.put(new BuildNumberRange(1016, 1026), new PatchFunc() {
            private final Set<String> fixed = new HashSet<>(Arrays.asList(
                "m_bWorldTreeState",
                "m_ulTeamLogo",
                "m_ulTeamBaseLogo",
                "m_ulTeamBannerLogo",
                "m_iPlayerIDsInControl",
                "m_bItemWhiteList",
                "m_iPlayerSteamID"
            ));
            @Override
            public void execute(SerializerField field) {
                if (fixed.contains(field.varName)) {
                    field.encoderType = "fixed64";
                }
            }
        });

        PATCHES.put(new BuildNumberRange(null, null), new PatchFunc() {
            private final Set<String> simTime = new HashSet<>(Arrays.asList(
                "m_flSimulationTime",
                "m_flAnimTime"
            ));
            @Override
            public void execute(SerializerField field) {
                if (simTime.contains(field.varName)) {
                    field.serializerType = "simulationtime";
                }
            }
        });

        PATCHES.put(new BuildNumberRange(null, 954), new PatchFunc() {
            private final Set<String> manaProps = new HashSet<>(Arrays.asList(
                "m_flMana",
                "m_flMaxMana"
            ));
            @Override
            public void execute(SerializerField field) {
                if (manaProps.contains(field.varName)) {
                    if (field.highValue == 3.4028235E38f) {
                        field.lowValue = null;
                        field.highValue = 8192.0f;
                    }
                }
            }
        });


    }

}
