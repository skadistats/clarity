package clarity.parser.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.decoder.StringTableDecoder;
import clarity.match.Match;
import clarity.model.StringTable;
import clarity.model.StringTableEntry;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;

import com.dota2.proto.Netmessages.CSVCMsg_CreateStringTable;

public class SvcCreateStringTableHandler implements Handler<CSVCMsg_CreateStringTable> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_CreateStringTable message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        StringTable table = new StringTable(
            message.getName(),
            message.getMaxEntries(),
            message.getUserDataFixedSize(),
            message.getUserDataSize(),
            message.getUserDataSizeBits(),
            message.getFlags()
        );
        match.getStringTables().add(table);
        List<StringTableEntry> changes = StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumEntries());
        StringTableApplier a = StringTableApplier.forName(table.getName());
        for (StringTableEntry t : changes) {
            a.apply(match, table.getName(), t.getIndex(), t.getKey(), t.getValue());
        }
    }

}
