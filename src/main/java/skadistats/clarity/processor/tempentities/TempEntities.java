package skadistats.clarity.processor.tempentities;

import com.dota2.proto.Netmessages;
import skadistats.clarity.decoder.TempEntitiesDecoder;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.Handle;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;

import java.util.List;

@Provides({ OnTempEntity.class })
@UsesDTClasses
public class TempEntities {

    private final Entity[] entities = new Entity[1 << Handle.INDEX_BITS];

    @OnMessage(Netmessages.CSVCMsg_TempEntities.class)
    public void onTempEntities(Context ctx, Netmessages.CSVCMsg_TempEntities message) {
        Event<OnTempEntity> ev = ctx.createEvent(OnTempEntity.class, Entity.class);
        if (ev.isListenedTo()) {
            List<Entity> entityList = new TempEntitiesDecoder(
                message.getEntityData().toByteArray(),
                message.getNumEntries(),
                ctx.getProcessor(DTClasses.class)
            ).decode();
            for (Entity e : entityList) {
                ev.raise(e);
            }
        }
    }

}
