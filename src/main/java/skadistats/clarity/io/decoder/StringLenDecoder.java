package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class StringLenDecoder extends Decoder {

    public static String decode(BitStream bs) {
        return bs.readString(bs.readUBitInt(9));
    }

    /**
     * Zero-alloc inline-string decode. Reads a 9-bit length prefix from the wire,
     * writes a 2-byte little-endian length at {@code data[offset..offset+1]}, and
     * writes up to {@code wireLen} UTF-8 bytes at {@code data[offset+2..]} — stops
     * early on a zero byte. The stored 2-byte length reflects the actual bytes
     * written (may be less than {@code wireLen} on mid-string zero termination).
     * <p>
     * {@code maxLength} is unused here — the caller reserves at least 512 bytes
     * for every StringLen leaf, which covers the decoder's intrinsic 9-bit
     * (511-byte) wire cap. It is declared for parity with
     * {@link StringZeroTerminatedDecoder#decodeIntoInline} so both string decoders
     * share a call shape at the inline-string dispatch.
     */
    public static void decodeIntoInline(BitStream bs, byte[] data, int offset, int maxLength) {
        var wireLen = bs.readUBitInt(9);
        var written = bs.readStringInto(data, offset + 2, wireLen);
        data[offset]     = (byte) (written & 0xFF);
        data[offset + 1] = (byte) ((written >>> 8) & 0xFF);
    }

}
