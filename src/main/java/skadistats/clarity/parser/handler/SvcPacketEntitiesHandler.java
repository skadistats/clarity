package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.PacketEntitiesDecoder;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;
import skadistats.clarity.wire.proto.Netmessages.CSVCMsg_PacketEntities;

@RegisterHandler(CSVCMsg_PacketEntities.class)
public class SvcPacketEntitiesHandler implements Handler<CSVCMsg_PacketEntities> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_PacketEntities message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        new PacketEntitiesDecoder(
            message.getEntityData().toByteArray(),
            message.getUpdatedEntries(),
            message.getIsDelta(),
            match.getDtClasses(),
            match.getStringTables().forName("instancebaseline")
            ).decodeAndApply(match.getEntities());
    }

}
