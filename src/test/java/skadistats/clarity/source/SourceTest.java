package skadistats.clarity.source;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import skadistats.clarity.model.EngineType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class SourceTest {
    //Source is Abstract so we need an implementing class
    public class SourceDummyClass extends Source {
        @Override
        public int getPosition() {
            return 0;
        }

        @Override
        public void setPosition(int position) throws IOException {
        }

        @Override
        public byte readByte() throws IOException {
            return 0;
        }

        @Override
        public void readBytes(byte[] dest, int offset, int length) throws IOException {
        }
    }

    public Source source;

    @Before
    public void setUp() throws Exception {
        source = new SourceDummyClass();
    }

    @Test
    public void testReadBytesInt() throws IOException {
        Source sourceSpy = spy(source);

        Answer<Void> readBytesAnswer = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                byte[] dst = (byte[]) args[0];

                for (int i = 0; i < dst.length; i++) {
                    dst[i] = (byte) i;
                }

                return null;
            }
        };
        doAnswer(readBytesAnswer).when(sourceSpy).readBytes(any(byte[].class), eq(0), eq(5));

        byte[] expected = new byte[]{0x0, 0x1, 0x2, 0x3, 0x4};
        byte[] actual = sourceSpy.readBytes(5);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void readFixedInt32_bigEndian() throws IOException {
        Source sourceSpy = spy(source);

        ByteBuffer returnValue = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE);
        returnValue = returnValue.order(ByteOrder.LITTLE_ENDIAN);
        returnValue.flip();

        ByteBuffer expected = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE);
        expected = expected.order(ByteOrder.LITTLE_ENDIAN);
        expected.flip();

        when(sourceSpy.readBytes(4)).thenReturn(returnValue.array());

        int actual = sourceSpy.readFixedInt32();

        assertEquals(expected.getInt(), actual);
    }

    @Test
    public void readFixedInt32_litleEndian() throws IOException {
        Source sourceSpy = spy(source);

        ByteBuffer returnValue = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE);
        returnValue = returnValue.order(ByteOrder.BIG_ENDIAN);
        returnValue.flip();

        ByteBuffer expected = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE);
        expected = expected.order(ByteOrder.LITTLE_ENDIAN);
        expected.flip();

        when(sourceSpy.readBytes(4)).thenReturn(expected.array());

        int actual = sourceSpy.readFixedInt32();

        assertEquals(expected.getInt(), actual);
    }

    @Test
    public void testReadVarInt32_positiveInt() throws IOException {
        int expectedResult = 127;

        Source sourceSpy = spy(source);
        doReturn((byte) expectedResult).when(sourceSpy).readByte();

        int result = sourceSpy.readVarInt32();

        assertEquals(expectedResult, result);
    }


    @Test
    public void testReadVarInt32_negativeInt() throws IOException {
        int expectedResult = -12;

        Source sourceSpy = spy(source);
        doReturn((byte) expectedResult).when(sourceSpy).readByte();

        int result = sourceSpy.readVarInt32();

        assertEquals(expectedResult, result);
    }

    @Test
    public void testReadVarInt32_malformedInt() throws IOException {
        Source sourceSpy = spy(source);
        doReturn((byte) 0xFF).when(sourceSpy).readByte();

        try {
            int result = sourceSpy.readVarInt32();
            fail("This should have failed with an exception");
        } catch (IOException exception) {
            assertEquals("malformed varint detected", exception.getMessage());
        }
    }

    @Test
    public void testReadFixedInt32() {
        fail("Not yet implemented");
    }

    @Test
    public void testSkipBytes() throws IOException {
        Source sourceSpy = spy(source);
        when(sourceSpy.getPosition()).thenReturn(42);

        sourceSpy.skipBytes(4);

        verify(sourceSpy, times(1)).setPosition(46);
    }

    @Test
    public void testSkipVarInt32_notEmpty() throws IOException {
        Source sourceSpy = spy(source);
        when(sourceSpy.readByte()).thenReturn((byte) 1);

        sourceSpy.skipVarInt32();

        verify(sourceSpy, times(1)).readByte();
    }

    @Test
    public void testSkipVarInt32_firstByteEmpty() throws IOException {
        Source sourceSpy = spy(source);
        when(sourceSpy.readByte())
                .thenReturn((byte) -1)
                .thenReturn((byte) 1);

        sourceSpy.skipVarInt32();

        verify(sourceSpy, times(2)).readByte();
    }

    @Test
    public void testSkipVarInt32_secondByteEmpty() throws IOException {
        Source sourceSpy = spy(source);
        when(sourceSpy.readByte())
                .thenReturn((byte) -1)
                .thenReturn((byte) -1)
                .thenReturn((byte) 1);

        sourceSpy.skipVarInt32();

        verify(sourceSpy, times(3)).readByte();
    }

    @Test
    public void testSkipVarInt32_thirdByteEmpty() throws IOException {
        Source sourceSpy = spy(source);
        when(sourceSpy.readByte())
                .thenReturn((byte) -1)
                .thenReturn((byte) -1)
                .thenReturn((byte) -1)
                .thenReturn((byte) 1);

        sourceSpy.skipVarInt32();

        verify(sourceSpy, times(4)).readByte();
    }

    @Test
    public void testSkipVarInt32_fourthByteEmpty() throws IOException {
        Source sourceSpy = spy(source);
        when(sourceSpy.readByte())
                .thenReturn((byte) -1)
                .thenReturn((byte) -1)
                .thenReturn((byte) -1)
                .thenReturn((byte) -1)
                .thenReturn((byte) 1);

        sourceSpy.skipVarInt32();

        verify(sourceSpy, times(5)).readByte();
    }

    @Test
    public void testReadEngineType_source1() throws IOException {
        Source sourceSpy = spy(source);
        byte[] source1ByteArray = EngineType.SOURCE1.getMagic().getBytes();
        when(sourceSpy.readBytes(8)).thenReturn(source1ByteArray);

        EngineType result = sourceSpy.readEngineType();

        assertEquals(EngineType.SOURCE1, result);
    }

    @Test
    public void testReadEngineType_source2() throws IOException {
        Source sourceSpy = spy(source);
        byte[] source2ByteArray = EngineType.SOURCE2.getMagic().getBytes();
        when(sourceSpy.readBytes(8)).thenReturn(source2ByteArray);

        EngineType result = sourceSpy.readEngineType();

        assertEquals(EngineType.SOURCE2, result);
    }

    @Test
    public void testReadEngineType_noEngineType() throws IOException {
        Source sourceSpy = spy(source);
        when(sourceSpy.readBytes(8)).thenReturn(new byte[8]);

        try {
            EngineType result = sourceSpy.readEngineType();
            fail("exception should have been thrown");
        } catch (IOException exception){
            assertEquals("given stream does not seem to contain a valid replay", exception.getMessage());
        }
    }

    @Test
    public void testReadEngineType_exceptionFromGetBytes() throws IOException {
        Source sourceSpy = spy(source);
        IOException ioException = new IOException("test exception");
        when(sourceSpy.readBytes(8)).thenThrow(ioException);

        try {
            EngineType result = sourceSpy.readEngineType();
            fail("exception should have been thrown");
        } catch (IOException exception){
            assertSame(ioException, exception);
        }
    }

    @Test
    public void testGetLastTick() {
        fail("Not yet implemented");
    }
}
