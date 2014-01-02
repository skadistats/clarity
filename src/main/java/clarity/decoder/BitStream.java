package clarity.decoder;

public class BitStream {

    final int[] words;
    int pos;

    public BitStream(byte[] data) {

        this.words = new int[(data.length + 3) / 4];
        this.pos = 0;

        int akku = 0;
        for (int i = 0; i < data.length; i++) {
            int shift = 8 * (i & 3);
            int val = ((int) data[i]) & 0xFF;
            akku = akku | (val << shift);
            if ((i & 3) == 3) {
                words[i / 4] = akku;
                akku = 0;
            }
        }
        if ((data.length & 3) != 0) {
            words[data.length / 4] = akku;
        }
    }

    public int peekNumericBits(int num) {
        int l = words[pos >> 5];
        int r = words[(pos + num - 1) >> 5];
        int shift = pos & 31;
        int rebuild = (r << (32 - shift)) | (l >>> shift);
        return (rebuild & ((1 << num) - 1));
    }

    public int readNumericBits(int num) {
        int result = peekNumericBits(num);
        pos += num;
        return result;
    }

    public boolean readBit() {
        boolean result = peekNumericBits(1) != 0;
        pos += 1;
        return result;
    }

    public byte[] readBits(int num) {
        byte[] result = new byte[(num + 7) / 8];
        int i = 0;
        while (num > 7) {
            num -= 8;
            result[i] = (byte) readNumericBits(8);
            i++;
        }
        if (num != 0) {
            result[i] = (byte) readNumericBits(num);
        }
        return result;
    }

    public String readString(int num) {
        StringBuffer buf = new StringBuffer();
        while (num > 0) {
            char c = (char) readNumericBits(8);
            if (c == 0) {
                break;
            }
            buf.append(c);
            num--;
        }
        return buf.toString();
    }

    public int readVarInt() {
        int run = 0;
        int value = 0;

        while (true) {
            int bits = readNumericBits(8);
            value = value | ((bits & 0x7f) << run);
            run += 7;
            if ((bits >> 7) == 0 || run == 35) {
                break;
            }
        }
        return value;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();

        int min = Math.max(0, (pos - 32) / 32);
        int max = Math.min(words.length - 1, (pos + 63) / 32);
        for (int i = min; i <= max; i++) {
            buf.append(new StringBuffer(String.format("%32s", Integer.toBinaryString(words[i])).replace(' ', '0')).reverse());
        }
        buf.insert(pos - min * 32, '*');
        return buf.toString();
    }

}
