package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.decoder.TempEntitiesDecoder;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;

import com.dota2.proto.Netmessages.CSVCMsg_TempEntities;

@RegisterHandler(CSVCMsg_TempEntities.class)
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
