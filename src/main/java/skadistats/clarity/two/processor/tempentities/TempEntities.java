package skadistats.clarity.two.processor.tempentities;

import com.dota2.proto.Netmessages;
import skadistats.clarity.decoder.TempEntitiesDecoder;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.Handle;
import skadistats.clarity.two.framework.annotation.Provides;
import skadistats.clarity.two.framework.invocation.Event;
import skadistats.clarity.two.processor.reader.OnMessage;
import skadistats.clarity.two.processor.runner.Context;
import skadistats.clarity.two.processor.sendtables.DTClasses;
import skadistats.clarity.two.processor.sendtables.UsesDTClasses;

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
