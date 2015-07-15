package skadistats.clarity;

import skadistats.clarity.parser.*;
import skadistats.clarity.wire.s1.proto.Demo.CDemoFileInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Clarity {

    private static DemoInputStream demoInputStreamForStream(InputStream stream, Profile... profile) throws IOException {
    	DemoInputStream d = new DemoInputStream(stream, profile);
    	d.bootstrap();
    	return d;
    }

    public static PeekIterator peekIteratorForFile(String fileName, Profile... profile) throws IOException {
        return new PeekIterator(demoInputStreamForStream(new FileInputStream(fileName), profile));
    }

    public static TickIterator tickIteratorForStream(InputStream stream, Profile... profile) throws IOException {
        return new TickIterator(demoInputStreamForStream(stream, profile));
    }
    
    public static TickIterator tickIteratorForFile(String fileName, Profile... profile) throws IOException {
        return new TickIterator(demoInputStreamForStream(new FileInputStream(fileName), profile));
    }

    public static DemoIndex indexForStream(InputStream stream, Profile... profile) throws IOException {
        return new DemoIndex(new PeekIterator(demoInputStreamForStream(stream, profile)));
    }

    public static DemoIndex indexForFile(String fileName, Profile... profile) throws IOException {
        return new DemoIndex(peekIteratorForFile(fileName, profile));
    }

    public static CDemoFileInfo infoForStream(InputStream stream) throws IOException {
        DemoInputStream s = null;
        try {
            s = demoInputStreamForStream(stream, Profile.FILE_INFO);
            s.skipToFileInfo();
            return (CDemoFileInfo) s.read().getMessage();
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }

    public static CDemoFileInfo infoForFile(String fileName) throws IOException {
    	return infoForStream(new FileInputStream(fileName));
    }
    
}
