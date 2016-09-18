package skadistats.clarity.util;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class MurmurHash {

	public static long hash64(String which, int seed) {
		int m = 0x5bd1e995;
		int r = 24;

		int h1 = seed ^ which.length();
		int h2 = 0;

		ByteBuffer buf = ByteBuffer.wrap(which.getBytes()).order(ByteOrder.LITTLE_ENDIAN);
		while (buf.remaining() >= 8) {
			int k1 = buf.getInt();
			k1 *= m; k1 ^= k1 >>> r; k1 *= m;
			h1 *= m; h1 ^= k1;

			int k2 = buf.getInt();
			k2 *= m; k2 ^= k2 >>> r; k2 *= m;
			h2 *= m; h2 ^= k2;
		}

		if (buf.remaining() >= 4) {
			int k1 = buf.getInt();
			k1 *= m; k1 ^= k1 >>> r; k1 *= m;
			h1 *= m; h1 ^= k1;
		}

		switch(buf.remaining()) {
			case 3:
				h2 ^= (((int)buf.get(buf.position() + 2)) & 0xff) << 16;
			case 2:
				h2 ^= (((int)buf.get(buf.position() + 1)) & 0xff) << 8;
			case 1:
				h2 ^= (((int)buf.get(buf.position())) & 0xff);
				h2 *= m;
		}

		h1 ^= h2 >>> 18; h1 *= m;
		h2 ^= h1 >>> 22; h2 *= m;
		h1 ^= h2 >>> 17; h1 *= m;
		h2 ^= h1 >>> 19; h2 *= m;

		long lh1 = ((long) h1) & 0xffffffffL;
		long lh2 = ((long) h2) & 0xffffffffL;

		return (lh1 << 32) | lh2;
	}

}
