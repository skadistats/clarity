package clarity.parser.handler;

import clarity.decoder.StringTableDecoder;
import clarity.match.Match;
import clarity.model.StringTable;
import clarity.parser.Handler;

import com.dota2.proto.DotaModifiers.CDOTAModifierBuffTableEntry;
import com.dota2.proto.Netmessages.CSVCMsg_CreateStringTable;
import com.google.protobuf.InvalidProtocolBufferException;

public class SvcCreateStringTableHandler implements Handler<CSVCMsg_CreateStringTable> {

    @Override
    public void apply(CSVCMsg_CreateStringTable message, Match match) {
        StringTable table = new StringTable(message);
        StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumEntries());
        match.getStringTables().add(table);
//        if (table.getName().equals("ActiveModifiers")) {
//
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
        
        
        //System.out.println("created string table " + table.getName());
        //System.out.println(table);
    }

}
