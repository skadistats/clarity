package skadistats.clarity.processor.modifiers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.wire.proto.DotaModifiers;

@Provides({OnModifierTableEntry.class})
public class Modifiers {

    @OnStringTableEntry("ActiveModifiers")
    public void onTableEntry(Context ctx, StringTable table, int index, String key, ByteString value) throws InvalidProtocolBufferException {
        if (value != null) {
            DotaModifiers.CDOTAModifierBuffTableEntry message = DotaModifiers.CDOTAModifierBuffTableEntry.parseFrom(value);
            ctx.createEvent(OnModifierTableEntry.class, DotaModifiers.CDOTAModifierBuffTableEntry.class).raise(message);
        }
    }

}
