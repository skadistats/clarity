package skadistats.clarity.processor.sendtables;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.engine.EngineType;
import skadistats.clarity.event.Provides;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s2.proto.S2NetMessages;

import java.io.IOException;

@Provides(value = OnDTClass.class, engine = EngineType.SOURCE2)
public class S2DTClassEmitter {

    @OnMessage(Demo.CDemoSendTables.class)
    public void onSendTables(Context ctx, Demo.CDemoSendTables message) throws IOException {
        CodedInputStream cis = CodedInputStream.newInstance(ZeroCopy.extract(message.getData()));
        int size = cis.readRawVarint32();
        S2NetMessages.CSVCMsg_FlattenedSerializer fs = Packet.parse(S2NetMessages.CSVCMsg_FlattenedSerializer.class, ZeroCopy.wrap(cis.readRawBytes(size)));

        int c = 0;
        for (S2NetMessages.ProtoFlattenedSerializer_t s : fs.getSerializersList()) {
            System.out.println("SERIALIZER: " + fs.getSymbols(s.getSerializerNameSym()));
            for (int fi : s.getFieldsIndexList()) {
                S2NetMessages.ProtoFlattenedSerializerField_t f = fs.getFields(fi);
                System.out.format(
                    "type: %s, name: %s, node: %s, serializer: %s, flags: %s, bitcount: %s, low: %s, high: %s\n",
                    fs.getSymbols(f.getVarTypeSym()),
                    fs.getSymbols(f.getVarNameSym()),
                    fs.getSymbols(f.getSendNodeSym()),
                    f.hasFieldSerializerNameSym() ? fs.getSymbols(f.getFieldSerializerNameSym()) : "-",
                    f.hasEncodeFlags() ? f.getEncodeFlags() : "-",
                    f.hasBitCount() ? f.getBitCount() : "-",
                    f.hasLowValue() ? f.getLowValue() : "-",
                    f.hasHighValue() ? f.getHighValue() : "-"
                );
            }
            System.out.println("-----------------------------------------------------------------------------");
            //if (c++ == 250) break;
        }
    }

}
