package skadistats.clarity.processor.sendtables;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.s2.*;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s2.proto.S2NetMessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Provides(value = OnDTClass.class, engine = EngineType.SOURCE2)
public class S2DTClassEmitter {

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
                        fieldType = new FieldType(protoMessage.getSymbols(protoField.getVarTypeSym()));
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
                    field = new Field(
                        fieldType,
                        protoMessage.getSymbols(protoField.getVarNameSym()),
                        protoMessage.getSymbols(protoField.getSendNodeSym()),
                        protoField.getEncodeFlags(),
                        protoField.hasBitCount() ? protoField.getBitCount() : 0,
                        protoField.hasLowValue() ? protoField.getLowValue() : 0.0f,
                        protoField.hasHighValue() ? protoField.getHighValue() : null,
                        fieldSerializer,
                        protoField.hasVarEncoderSym() ? protoMessage.getSymbols(protoField.getVarEncoderSym()) : null);
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
