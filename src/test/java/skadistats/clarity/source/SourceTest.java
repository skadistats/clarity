package skadistats.clarity.source;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SourceTest {
	//Source is Abstract so we need an implementing class
	public class SourceDummyClass extends Source {
		@Override
		public int getPosition() {return 0;}

		@Override
		public void setPosition(int position) throws IOException {}

		@Override
		public byte readByte() throws IOException {return 0;}

		@Override
		public void readBytes(byte[] dest, int offset, int length) throws IOException {}
	}

	Source source;

	@Before
	public void setUp() throws Exception {
		source = new SourceDummyClass();
	}

	@Test
	public void testReadBytesByteArrayIntInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testReadBytesInt() throws IOException {
		source = spy(source);

		Answer<Void> readBytesAnswer = new Answer<Void>(){
			public Void answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				byte[] dst = (byte[]) args[0];

				for(int i = 0; i < dst.length; i++){
					dst[i] = (byte)i;
				}

				return null;
			}
		};
		doAnswer(readBytesAnswer).when(source).readBytes(any(byte[].class), eq(0), eq(5));

		byte[] expected = new byte[]{0x0, 0x1, 0x2, 0x3, 0x4};
		byte[] actual = source.readBytes(5);

		assertArrayEquals(expected, actual);
	}

	@Test
	public void readFixedInt32_bigEndian() throws IOException {
		source = spy(source);

		ByteBuffer returnValue = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE);
		returnValue = returnValue.order(ByteOrder.LITTLE_ENDIAN);
		returnValue.flip();
		
		ByteBuffer expected = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE);
		expected = expected.order(ByteOrder.LITTLE_ENDIAN);
		expected.flip();
		
		when(source.readBytes(4)).thenReturn(returnValue.array());
		
		int actual = source.readFixedInt32();
		
		assertEquals(expected.getInt(), actual);;
	}

	@Test
	public void readFixedInt32_litleEndian() throws IOException {
		source = spy(source);

		ByteBuffer returnValue = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE);
		returnValue = returnValue.order(ByteOrder.BIG_ENDIAN);
		returnValue.flip();

		ByteBuffer expected = ByteBuffer.allocate(4).putInt(Integer.MAX_VALUE);
		expected = expected.order(ByteOrder.LITTLE_ENDIAN);
		expected.flip();
		
		when(source.readBytes(4)).thenReturn(expected.array());
		
		int actual = source.readFixedInt32();
		
		assertEquals(expected.getInt(), actual);
	}

	@Test
	public void testReadVarInt32() {
		fail("Not yet implemented");
	}

	@Test
	public void testReadFixedInt32() {
		fail("Not yet implemented");
	}

	@Test
	public void testSkipBytes() throws IOException {
		source = spy(source);
		when(source.getPosition()).thenReturn(42);

		source.skipBytes(4);
		
		verify(source, times(1)).setPosition(46);
	}

	@Test
	public void testSkipVarInt32_notEmpty() throws IOException {
		source = spy(source);
		when(source.readByte()).thenReturn((byte)1);
		
		source.skipVarInt32();
		
		verify(source, times(1)).readByte();
	}
	
	@Test
	public void testSkipVarInt32_firstByteEmpty() throws IOException {
		source = spy(source);
		when(source.readByte())
		.thenReturn((byte)-1)
		.thenReturn((byte)1);
		
		source.skipVarInt32();
		
		verify(source, times(2)).readByte();
	}
	
	@Test
	public void testSkipVarInt32_secondByteEmpty() throws IOException {
		source = spy(source);
		when(source.readByte())
		.thenReturn((byte)-1)
		.thenReturn((byte)-1)
		.thenReturn((byte)1);
		
		source.skipVarInt32();
		
		verify(source, times(3)).readByte();
	}
	
	@Test
	public void testSkipVarInt32_thirdByteEmpty() throws IOException {
		source = spy(source);
		when(source.readByte())
		.thenReturn((byte)-1)
		.thenReturn((byte)-1)
		.thenReturn((byte)-1)
		.thenReturn((byte)1);
		
		source.skipVarInt32();
		
		verify(source, times(4)).readByte();
	}
	
	@Test
	public void testSkipVarInt32_fourthByteEmpty() throws IOException {
		source = spy(source);
		when(source.readByte())
		.thenReturn((byte)-1)
		.thenReturn((byte)-1)
		.thenReturn((byte)-1)
		.thenReturn((byte)-1)
		.thenReturn((byte)1);
		
		source.skipVarInt32();
		
		verify(source, times(5)).readByte();
	}

	@Test
	public void testReadEngineType() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetFullPacketsBeforeTick() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetLastTick() {
		fail("Not yet implemented");
	}

	//Stubs of Abstract methods
	@Test
	public void testGetPosition() {
		assertEquals(0, source.getPosition());
	}

	@Test
	public void testSetPosition() throws IOException {
		//Nothing to assert
		source.setPosition(99);
	}

	@Test
	public void testReadByte() throws IOException {
		assertEquals(0, source.readByte());
	}
}
