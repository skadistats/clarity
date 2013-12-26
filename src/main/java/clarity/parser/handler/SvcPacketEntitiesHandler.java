package clarity.parser.handler;

import java.util.List;

import org.javatuples.Pair;

import clarity.decoder.PacketEntitiesDecoder;
import clarity.match.Match;
import clarity.model.Entity;
import clarity.model.EntityCollection;
import clarity.model.PVS;

import com.dota2.proto.Netmessages.CSVCMsg_PacketEntities;

public class SvcPacketEntitiesHandler implements Handler<CSVCMsg_PacketEntities> {

    @Override
    public void apply(CSVCMsg_PacketEntities message, Match match) {
        EntityCollection entities = match.getEntityCollection();
        List<Pair<PVS, Entity>> changes = new PacketEntitiesDecoder(
            message.getEntityData().toByteArray(),
            message.getUpdatedEntries(),
            message.getIsDelta(),
            match.getDtClasses(),
            match.getStringTables().byName("instancebaseline")
            ).decode(match.getEntityCollection());

        for (Pair<PVS, Entity> change : changes) {
            PVS pvs = change.getValue0();
            Entity entity = change.getValue1();
            switch (pvs) {
            case ENTER:
                entities.put(entity.getIndex(), entity);
                break;

            case PRESERVE:
                entities.get(entity.getIndex()).updateFrom(entity);
                break;

            case LEAVE:
                break;

            case LEAVE_AND_DELETE:
                entities.put(entity.getIndex(), null);
                break;
            }
        }
    }

}
