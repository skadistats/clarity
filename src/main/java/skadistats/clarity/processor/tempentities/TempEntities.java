package skadistats.clarity.processor.tempentities;

import com.dota2.proto.Netmessages;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.ReceiveProp;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;

@Provides({ OnTempEntity.class })
@UsesDTClasses
public class TempEntities {

    private final int[] indices = new int[Entities.MAX_ENTITY + 1];

    @OnMessage(Netmessages.CSVCMsg_TempEntities.class)
    public void onTempEntities(Context ctx, Netmessages.CSVCMsg_TempEntities message) {
        Event<OnTempEntity> ev = ctx.createEvent(OnTempEntity.class, Entity.class);
        if (ev.isListenedTo()) {
            BitStream stream = new BitStream(message.getEntityData().toByteArray());
            DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
            DTClass cls = null;
            int count = message.getNumEntries();
            while (count-- > 0) {
                stream.readNumericBits(1); // seems to be always 0
                if (stream.readNumericBits(1) == 1) {
                    cls = dtClasses.forClassId(stream.readNumericBits(dtClasses.getClassBits()) - 1);
                }
                Object[] state = new Object[cls.getReceiveProps().size()];
                int cIndices = stream.readEntityPropList(indices);
                for (int ci = 0; ci < cIndices; ci++) {
                    ReceiveProp r = cls.getReceiveProps().get(indices[ci]);
                    state[indices[ci]] = r.getType().getDecoder().decode(stream, r);
                }
                ev.raise(new Entity(0, 0, cls, null, state));
            }
        }
    }

}
