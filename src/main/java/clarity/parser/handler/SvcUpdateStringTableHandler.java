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
        //System.out.println("updating " + table.getName());
        StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumChangedEntries());
    }

}
