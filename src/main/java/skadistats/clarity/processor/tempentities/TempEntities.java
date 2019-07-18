package skadistats.clarity.processor.tempentities;

import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.decoder.s1.ReceiveProp;
import skadistats.clarity.decoder.s1.S1DTClass;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.EntityStateSupplier;
import skadistats.clarity.model.state.CloneableEntityState;
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
                CloneableEntityState state = EntityStateFactory.withLength(receiveProps.length);
                fieldReader.readFields(stream, cls, state, null, false);
                S1DTClass finalCls = cls;
                evTempEntity.raise(new Entity(new EntityStateSupplier() {
                    @Override
                    public int getIndex() {
                        return 0;
                    }
                    @Override
                    public DTClass getDTClass() {
                        return finalCls;
                    }
                    @Override
                    public int getSerial() {
                        return 0;
                    }
                    @Override
                    public boolean isActive() {
                        return true;
                    }
                    @Override
                    public int getHandle() {
                        return 0;
                    }
                    @Override
                    public CloneableEntityState getState() {
                        return state;
                    }
                }));
            }
        }
    }

}
