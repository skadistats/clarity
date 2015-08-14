package skadistats.clarity.source;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.IOException;

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
	public void testReadVarInt32() {
		fail("Not yet implemented");
	}

	@Test
	public void testReadFixedInt32() {
		fail("Not yet implemented");
	}

	@Test
	public void testSkipBytes() {
		fail("Not yet implemented");
	}

	@Test
	public void testSkipVarInt32() {
		fail("Not yet implemented");
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
