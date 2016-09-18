package skadistats.clarity.processor.modifiers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.wire.common.proto.DotaModifiers;

@Provides({OnModifierTableEntry.class})
public class Modifiers {

    @InsertEvent
    private Event<OnModifierTableEntry> evEntry;

    @OnStringTableEntry("ActiveModifiers")
    public void onTableEntry(StringTable table, int index, String key, ByteString value) throws InvalidProtocolBufferException {
        if (value != null) {
            DotaModifiers.CDOTAModifierBuffTableEntry message = DotaModifiers.CDOTAModifierBuffTableEntry.parseFrom(value);
            evEntry.raise(message);
        }
    }

}
