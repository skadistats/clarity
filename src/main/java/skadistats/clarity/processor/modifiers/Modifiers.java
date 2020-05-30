package skadistats.clarity.processor.modifiers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.wire.common.proto.DotaModifiers;

@Provides({OnModifierTableEntry.class})
public class Modifiers {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.modifiers);

    @InsertEvent
    private Event<OnModifierTableEntry> evEntry;

    @OnStringTableEntry("ActiveModifiers")
    public void onTableEntry(StringTable table, int index, String key, ByteString value) throws InvalidProtocolBufferException {
        if (value != null) {
            DotaModifiers.CDOTAModifierBuffTableEntry message;
            try {
                message = DotaModifiers.CDOTAModifierBuffTableEntry.parseFrom(value);
            } catch (InvalidProtocolBufferException ex) {
                message = (DotaModifiers.CDOTAModifierBuffTableEntry) ex.getUnfinishedMessage();
                byte[] b = ZeroCopy.extract(value);
                log.error("failed to parse CDOTAModifierBuffTableEntry, returning incomplete message. Only %d/%d bytes parsed.", message.getSerializedSize(), value.size());
                for (String line : formatHexDump(b, 0, b.length).split("\n")) {
                    log.info("%s", line);
                }
            }
            evEntry.raise(message);
        }
    }

    public static String formatHexDump(byte[] array, int offset, int length) {
        StringBuilder builder = new StringBuilder();
        for (int rowOffset = offset; rowOffset < offset + length; rowOffset += 16) {
            builder.append(String.format("%06d:  ", rowOffset));
            for (int index = 0; index < 16; index++) {
                if (rowOffset + index < array.length) {
                    builder.append(String.format("%02x ", array[rowOffset + index]));
                } else {
                    break;
                }
            }
            builder.append(String.format("%n"));
        }
        return builder.toString();
    }

}
