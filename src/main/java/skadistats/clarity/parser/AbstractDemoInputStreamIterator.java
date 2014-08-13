package skadistats.clarity.parser;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

public abstract class AbstractDemoInputStreamIterator<T> implements Iterator<T>, Closeable {
	
	protected final DemoInputStream s;

	public AbstractDemoInputStreamIterator(DemoInputStream s) {
		this.s = s;
	}
	
	@Override
	public void close() throws IOException {
		s.close();
	}
	
}
