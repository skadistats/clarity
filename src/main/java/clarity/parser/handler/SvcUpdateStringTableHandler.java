package clarity.parser.handler;

import java.util.List;

import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.decoder.StringTableApplier;
import clarity.decoder.StringTableDecoder;
import clarity.match.Match;
import clarity.model.StringTable;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_UpdateStringTable;
import com.google.protobuf.ByteString;

public class SvcUpdateStringTableHandler implements Handler<CSVCMsg_UpdateStringTable> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(CSVCMsg_UpdateStringTable message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        StringTable table = match.getStringTables().forId(message.getTableId());
        List<Triplet<Integer, String, ByteString>> changes = StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumChangedEntries());
        StringTableApplier a = StringTableApplier.forName(table.getName());
        for (Triplet<Integer, String, ByteString> t : changes) {
            a.apply(match, table.getName(), t.getValue0(), t.getValue1(), t.getValue2());
        }
    }

}
