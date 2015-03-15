package skadistats.clarity.two.processor.modifiers;

import com.dota2.proto.DotaModifiers;
import com.google.protobuf.InvalidProtocolBufferException;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.Handle;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.StringTableEntry;
import skadistats.clarity.two.framework.annotation.Provides;
import skadistats.clarity.two.processor.runner.Context;
import skadistats.clarity.two.processor.stringtables.OnStringTableEntry;

@Provides({OnModifierTableEntry.class})
public class Modifiers {

    private final Entity[] entities = new Entity[1 << Handle.INDEX_BITS];

    @OnStringTableEntry("ActiveModifiers")
    public void onTableEntry(Context ctx, StringTable table, StringTableEntry oldEntry, StringTableEntry newEntry) throws InvalidProtocolBufferException {
        if (newEntry.getValue() != null) {
            DotaModifiers.CDOTAModifierBuffTableEntry message = DotaModifiers.CDOTAModifierBuffTableEntry.parseFrom(newEntry.getValue());
            ctx.createEvent(OnModifierTableEntry.class, DotaModifiers.CDOTAModifierBuffTableEntry.class).raise(message);
        }
    }

}
