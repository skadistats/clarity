package skadistats.clarity.processor.modifiers;

import skadistats.clarity.protobuf.ByteString;
import skadistats.clarity.protobuf.InvalidProtocolBufferException;
import skadistats.clarity.protobuf.ZeroCopy;
import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.stringtables.OnStringTableEntry;
import skadistats.clarity.wire.dota.common.proto.DOTAModifiers;

/**
 * Provides {@link OnModifierTableEntry}, decoded from the {@code "ActiveModifiers"} string table (Dota 2).
 * <p>
 * An entry that fails to parse is logged with a hex dump and raised as an incomplete message.
 */
@Provides({OnModifierTableEntry.class})
public class Modifiers {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.modifiers);

    @InsertEvent
    private OnModifierTableEntry.Event evEntry;

    /** Event handler bound by the runtime; not for direct use. */
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
                for (var line : formatHexDump(b, 0, b.length).split("\n")) {
                    log.info("%s", line);
                }
            }
            evEntry.raise(message);
        }
    }

    /**
     * Formats bytes as a hex dump, 16 bytes per row, each row prefixed with its decimal offset.
     *
     * @param array the data
     * @param offset index of the first byte to dump
     * @param length number of bytes to dump
     * @return the formatted dump
     */
    public static String formatHexDump(byte[] array, int offset, int length) {
        var builder = new StringBuilder();
        for (var rowOffset = offset; rowOffset < offset + length; rowOffset += 16) {
            builder.append(String.format("%06d:  ", rowOffset));
            for (var index = 0; index < 16; index++) {
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
