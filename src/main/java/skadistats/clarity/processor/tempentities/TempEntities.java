package skadistats.clarity.processor.tempentities;

import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.s1.ReceiveProp;
import skadistats.clarity.io.s1.S1DTClass;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.EntityStateFactory;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.OnInit;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.wire.s1.proto.S1NetMessages;

@Provides({ OnTempEntity.class })
@UsesDTClasses
public class TempEntities {

    @Insert
    private EngineType engineType;
    @Insert
    private DTClasses dtClasses;

    @InsertEvent
    private Event<OnTempEntity> evTempEntity;

    private FieldReader fieldReader;

    @OnInit
    public void onInit() {
        fieldReader = engineType.getNewFieldReader();
    }

    @OnMessage(S1NetMessages.CSVCMsg_TempEntities.class)
    public void onTempEntities(S1NetMessages.CSVCMsg_TempEntities message) {
        if (evTempEntity.isListenedTo()) {
            BitStream stream = BitStream.createBitStream(message.getEntityData());
            S1DTClass cls = null;
            ReceiveProp[] receiveProps = null;
            int count = message.getNumEntries();
            while (count-- > 0) {
                stream.readUBitInt(1); // seems to be always 0
                if (stream.readBitFlag()) {
                    cls = (S1DTClass) dtClasses.forClassId(stream.readUBitInt(dtClasses.getClassBits()) - 1);
                    receiveProps = cls.getReceiveProps();
                }
                EntityState state = EntityStateFactory.forS1(receiveProps);
                fieldReader.readFields(stream, cls, state, null, false);

                int handle = engineType.emptyHandle();
                Entity te = new Entity(
                        engineType.indexForHandle(handle),
                        engineType.serialForHandle(handle),
                        handle,
                        cls);
                te.setExistent(true);
                te.setActive(true);
                te.setState(state);
                evTempEntity.raise(te);
            }
        }
    }

}
