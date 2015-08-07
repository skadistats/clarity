package skadistats.clarity.processor.modifiers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.wire.s1.proto.S1DotaModifiers;

@Provides({OnModifierTableEntry.class})
public class Modifiers {

    @OnStringTableEntry("ActiveModifiers")
    public void onTableEntry(Context ctx, StringTable table, int index, String key, ByteString value) throws InvalidProtocolBufferException {
        if (value != null) {
            S1DotaModifiers.CDOTAModifierBuffTableEntry message = S1DotaModifiers.CDOTAModifierBuffTableEntry.parseFrom(value);
            ctx.createEvent(OnModifierTableEntry.class, S1DotaModifiers.CDOTAModifierBuffTableEntry.class).raise(message);
        }
    }

}
