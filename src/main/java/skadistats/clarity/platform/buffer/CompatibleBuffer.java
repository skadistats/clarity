package skadistats.clarity.platform.buffer;

public class CompatibleBuffer implements Buffer {

    private final byte[] data;

    public CompatibleBuffer(byte[] data) {
        this.data = data;
    }

    @Override
    public byte getByte(int offs) {
        return data[offs];
    }

    @Override
    public int getInt(int offs) {
        int v = 0;
        int n = Math.min(4, data.length - offs);
        int o = offs + n - 1;
        switch (n) {
            case 4: v |= ((int)data[o--] << 24) & 0xFF000000L;
            case 3: v |= ((int)data[o--] << 16) & 0x00FF0000L;
            case 2: v |= ((int)data[o--] <<  8) & 0x0000FF00L;
            case 1: v |= ((int)data[o  ]      ) & 0x000000FFL;
        }
        return v;
    }

    @Override
    public long getLong(int offs) {
        long v = 0;
        int n = Math.min(8, data.length - offs);
        int o = offs + n - 1;
        switch (n) {
            case 8: v |= ((long)data[o--] << 56) & 0xFF00000000000000L;
            case 7: v |= ((long)data[o--] << 48) & 0x00FF000000000000L;
            case 6: v |= ((long)data[o--] << 40) & 0x0000FF0000000000L;
            case 5: v |= ((long)data[o--] << 32) & 0x000000FF00000000L;
            case 4: v |= ((long)data[o--] << 24) & 0x00000000FF000000L;
            case 3: v |= ((long)data[o--] << 16) & 0x0000000000FF0000L;
            case 2: v |= ((long)data[o--] <<  8) & 0x000000000000FF00L;
            case 1: v |= ((long)data[o  ]      ) & 0x00000000000000FFL;
        }
        return v;
    }

    @Override
    public void copy(int offs, Buffer dest, int nBytes) {
        CompatibleBuffer other = (CompatibleBuffer) dest;
        System.arraycopy(data, offs, other.data, 0, nBytes);
    }

    @Override
    public void putInt(int offs, int value) {
        int n = Math.min(4, data.length - offs);
        int o = offs + n - 1;
        switch (n) {
            case 4: data[o--] = (byte)((value >>> 24) & 0xFF);
            case 3: data[o--] = (byte)((value >>> 16) & 0xFF);
            case 2: data[o--] = (byte)((value >>>  8) & 0xFF);
            case 1: data[o  ] = (byte)((value       ) & 0xFF);
        }
    }

    @Override
    public void putLong(int offs, long value) {
        int n = Math.min(8, data.length - offs);
        int o = offs + n - 1;
        switch (n) {
            case 8: data[o--] = (byte)((value >>> 56) & 0xFFL);
            case 7: data[o--] = (byte)((value >>> 48) & 0xFFL);
            case 6: data[o--] = (byte)((value >>> 40) & 0xFFL);
            case 5: data[o--] = (byte)((value >>> 32) & 0xFFL);
            case 4: data[o--] = (byte)((value >>> 24) & 0xFFL);
            case 3: data[o--] = (byte)((value >>> 16) & 0xFFL);
            case 2: data[o--] = (byte)((value >>>  8) & 0xFFL);
            case 1: data[o  ] = (byte)((value       ) & 0xFFL);
        }
    }

}
