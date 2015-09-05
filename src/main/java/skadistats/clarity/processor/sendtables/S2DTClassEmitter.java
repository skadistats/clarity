package skadistats.clarity.processor.sendtables;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.s2.S2DTClass;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.SerializerId;
import skadistats.clarity.model.s2.field.*;
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
        Map<Integer, FieldType> fieldTypes = new HashMap<>();
        Field[] fields = new Field[protoMessage.getFieldsCount()];
        ArrayList<Field> currentFields = new ArrayList<>(128);
        for (int si = 0; si < protoMessage.getSerializersCount(); si++) {
            S2NetMessages.ProtoFlattenedSerializer_t protoSerializer = protoMessage.getSerializers(si);
            currentFields.clear();
            for (int fi : protoSerializer.getFieldsIndexList()) {
                Field field = fields[fi];
                if (field == null) {
                    S2NetMessages.ProtoFlattenedSerializerField_t protoField = protoMessage.getFields(fi);
                    FieldType fieldType = fieldTypes.get(protoField.getVarTypeSym());
                    if (fieldType == null) {
                        fieldType = createFieldType(protoMessage.getSymbols(protoField.getVarTypeSym()));
                        fieldTypes.put(protoField.getVarTypeSym(), fieldType);
                    }
                    Serializer fieldSerializer = null;
                    if (protoField.hasFieldSerializerNameSym()) {
                        fieldSerializer = serializers.get(
                            new SerializerId(
                                protoMessage.getSymbols(protoField.getFieldSerializerNameSym()),
                                protoField.getFieldSerializerVersion()
                            )
                        );
                    }
                    String sendNode = protoMessage.getSymbols(protoField.getSendNodeSym());
                    FieldProperties fieldProperties = new FieldProperties(
                        fieldType,
                        protoMessage.getSymbols(protoField.getVarNameSym()),
                        !"(root)".equals(sendNode) ? sendNode : null,
                        protoField.hasEncodeFlags() ? protoField.getEncodeFlags() : null,
                        protoField.hasBitCount() ? protoField.getBitCount() : null,
                        protoField.hasLowValue() ? protoField.getLowValue() : null,
                        protoField.hasHighValue() ? protoField.getHighValue() : null,
                        fieldSerializer,
                        protoField.hasVarEncoderSym() ? protoMessage.getSymbols(protoField.getVarEncoderSym()) : null);
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



}
