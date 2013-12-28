package clarity.parser.handler;

import clarity.decoder.StringTableDecoder;
import clarity.match.Match;
import clarity.model.StringTable;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_UpdateStringTable;

public class SvcUpdateStringTableHandler implements Handler<CSVCMsg_UpdateStringTable> {

    @Override
    public void apply(CSVCMsg_UpdateStringTable message, Match match) {
        StringTable table = match.getStringTables().forId(message.getTableId());
        StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumChangedEntries());
//        if (table.getName().equals("ActiveModifiers")) {
//            for (int i = 0; i < table.getMaxEntries(); i++) {
//                if (table.getValueByIndex(i) == null) {
//                    continue;
//                }
//                try {
//                    CDOTAModifierBuffTableEntry e = CDOTAModifierBuffTableEntry.parseFrom(table.getValueByIndex(i));
//                    System.out.println(e);
//                } catch (InvalidProtocolBufferException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

    }

}
