package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

/**
 * Decoder for {@code CUtlBinaryBlock} fields in Source 2 entity data.
 *
 * <p>Wire format: a varint-encoded length {@code N} followed by {@code N} raw
 * bytes. The default {@link IntVarUnsignedDecoder} fallback would read only
 * the length and leave the payload bytes in the stream, which desyncs the
 * entity decoder for the rest of the packet.
 */
public class CUtlBinaryBlockDecoder implements Decoder<byte[]> {

    @Override
    public byte[] decode(BitStream bs) {
        var n = bs.readVarUInt();
        var out = new byte[n];
        bs.readBitsIntoByteArray(out, n * 8);
        return out;
    }

}
