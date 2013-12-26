package clarity.parser.handler;

import clarity.decoder.StringTableDecoder;
import clarity.match.Match;
import clarity.model.StringTable;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_CreateStringTable;

public class SvcCreateStringTableHandler implements Handler<CSVCMsg_CreateStringTable> {

    @Override
    public void apply(CSVCMsg_CreateStringTable message, Match match) {
        StringTable table = new StringTable(message);
        StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumEntries());
        match.getStringTables().add(table);
    }

}
