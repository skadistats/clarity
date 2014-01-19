package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.decoder.TempEntitiesDecoder;
import clarity.match.Match;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;

import com.dota2.proto.Netmessages.CSVCMsg_TempEntities;

public class SvcTempEntitiesHandler implements Handler<CSVCMsg_TempEntities> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_TempEntities message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        new TempEntitiesDecoder(
            message.getEntityData().toByteArray(),
            message.getNumEntries(),
            match.getDtClasses()
        ).decodeAndApply(match.getTempEntities());
    }

}
