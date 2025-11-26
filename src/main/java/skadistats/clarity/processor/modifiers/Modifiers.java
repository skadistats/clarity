package skadistats.clarity.processor.modifiers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.io.Util;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.wire.dota.common.proto.DOTAModifiers;

@Provides({OnModifierTableEntry.class})
public class Modifiers {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.modifiers);

    @InsertEvent
    private Event<OnModifierTableEntry> evEntry;

    @OnStringTableEntry("ActiveModifiers")
    public void onTableEntry(StringTable table, int index, String key, ByteString value) throws InvalidProtocolBufferException {
        if (value != null) {
            DOTAModifiers.CDOTAModifierBuffTableEntry message;
            try {
                message = DOTAModifiers.CDOTAModifierBuffTableEntry.parseFrom(value);
            } catch (InvalidProtocolBufferException ex) {
                message = (DOTAModifiers.CDOTAModifierBuffTableEntry) ex.getUnfinishedMessage();
                var b = ZeroCopy.extract(value);
                log.error("failed to parse CDOTAModifierBuffTableEntry, returning incomplete message. Only %d/%d bytes parsed.", message.getSerializedSize(), value.size());
                for (var line : Util.formatHexDump(b, 0, b.length).split("\n")) {
                    log.info("%s", line);
                }
            }
            evEntry.raise(message);
        }
    }

}
