package clarity.parser.handler;

import clarity.decoder.PacketEntitiesDecoder;
import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_PacketEntities;

public class SvcPacketEntitiesHandler implements Handler<CSVCMsg_PacketEntities> {

    @Override
    public void apply(CSVCMsg_PacketEntities message, Match match) {
        new PacketEntitiesDecoder(
            message.getEntityData().toByteArray(),
            message.getUpdatedEntries(),
            message.getIsDelta(),
            match.getDtClasses(),
            match.getStringTables().forName("instancebaseline")
            ).decodeAndApply(match.getEntities());
    }

}
