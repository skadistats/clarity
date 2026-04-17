package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class StringZeroTerminatedDecoder extends Decoder {

    public static String decode(BitStream bs) {
        return bs.readString(BitStream.MAX_STRING_LENGTH);
    }

    /**
     * Zero-alloc inline-string decode. The wire format has no length prefix —
     * bytes are consumed until a zero byte is read or {@code maxLength} bytes have
     * been written. Writes a 2-byte little-endian length at
     * {@code data[offset..offset+1]} reflecting the actual bytes written, and
     * writes the UTF-8 bytes at {@code data[offset+2..]}. The caller is expected
     * to have allocated {@code 2 + maxLength} bytes at {@code offset}.
     */
    public static void decodeIntoInline(BitStream bs, byte[] data, int offset, int maxLength) {
        var written = bs.readStringInto(data, offset + 2, maxLength);
        data[offset]     = (byte) (written & 0xFF);
        data[offset + 1] = (byte) ((written >>> 8) & 0xFF);
    }

}
