package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.decoder.PacketEntitiesDecoder;
import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_PacketEntities;

public class SvcPacketEntitiesHandler implements Handler<CSVCMsg_PacketEntities> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_PacketEntities message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        new PacketEntitiesDecoder(
            message.getEntityData().toByteArray(),
            message.getUpdatedEntries(),
            message.getIsDelta(),
            match.getDtClasses(),
            match.getStringTables().forName("instancebaseline")
            ).decodeAndApply(match.getEntities());
    }

}
