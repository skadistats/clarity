package skadistats.clarity.model.state;

import skadistats.clarity.model.Vector;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public sealed interface PrimitiveType {

    VarHandle INT_VH = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    VarHandle FLOAT_VH = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
    VarHandle LONG_VH = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    int size();

    void write(byte[] data, int offset, Object value);

    Object read(byte[] data, int offset);

    enum Scalar implements PrimitiveType {
        INT(4) {
            @Override
            public void write(byte[] data, int offset, Object value) {
                INT_VH.set(data, offset, ((Integer) value).intValue());
            }

            @Override
            public Object read(byte[] data, int offset) {
                return (Integer) INT_VH.get(data, offset);
            }

            @Override
            void writeRaw(byte[] data, int offset, float value) {
                throw new UnsupportedOperationException();
            }

            @Override
            float readRaw(byte[] data, int offset) {
                throw new UnsupportedOperationException();
            }
        },
        FLOAT(4) {
            @Override
            public void write(byte[] data, int offset, Object value) {
                FLOAT_VH.set(data, offset, ((Float) value).floatValue());
            }

            @Override
            public Object read(byte[] data, int offset) {
                return (Float) FLOAT_VH.get(data, offset);
            }

            @Override
            void writeRaw(byte[] data, int offset, float value) {
                FLOAT_VH.set(data, offset, value);
            }

            @Override
            float readRaw(byte[] data, int offset) {
                return (float) FLOAT_VH.get(data, offset);
            }
        },
        LONG(8) {
            @Override
            public void write(byte[] data, int offset, Object value) {
                LONG_VH.set(data, offset, ((Long) value).longValue());
            }

            @Override
            public Object read(byte[] data, int offset) {
                return (Long) LONG_VH.get(data, offset);
            }

            @Override
            void writeRaw(byte[] data, int offset, float value) {
                throw new UnsupportedOperationException();
            }

            @Override
            float readRaw(byte[] data, int offset) {
                throw new UnsupportedOperationException();
            }
        },
        BOOL(1) {
            @Override
            public void write(byte[] data, int offset, Object value) {
                data[offset] = ((Boolean) value) ? (byte) 1 : (byte) 0;
            }

            @Override
            public Object read(byte[] data, int offset) {
                return data[offset] != 0;
            }

            @Override
            void writeRaw(byte[] data, int offset, float value) {
                throw new UnsupportedOperationException();
            }

            @Override
            float readRaw(byte[] data, int offset) {
                throw new UnsupportedOperationException();
            }
        };

        private final int size;

        Scalar(int size) {
            this.size = size;
        }

        @Override
        public int size() {
            return size;
        }

        abstract void writeRaw(byte[] data, int offset, float value);

        abstract float readRaw(byte[] data, int offset);
    }

    record VectorType(Scalar element, int count) implements PrimitiveType {
        @Override
        public int size() {
            return count * element.size();
        }

        @Override
        public void write(byte[] data, int offset, Object value) {
            var v = (Vector) value;
            var step = element.size();
            for (var i = 0; i < count; i++) {
                element.writeRaw(data, offset + i * step, v.getElement(i));
            }
        }

        @Override
        public Object read(byte[] data, int offset) {
            var floats = new float[count];
            var step = element.size();
            for (var i = 0; i < count; i++) {
                floats[i] = element.readRaw(data, offset + i * step);
            }
            return new Vector(floats);
        }
    }
}
