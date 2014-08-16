package skadistats.clarity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import skadistats.clarity.parser.DemoIndex;
import skadistats.clarity.parser.DemoInputStream;
import skadistats.clarity.parser.DemoInputStreamIterator;
import skadistats.clarity.parser.PeekIterator;
import skadistats.clarity.parser.Profile;
import skadistats.clarity.parser.TickIterator;

import com.dota2.proto.Demo.CDemoFileInfo;

public class Clarity {

    private static DemoInputStream demoInputStreamForStream(InputStream stream, Profile... profile) throws IOException {
    	DemoInputStream d = new DemoInputStream(stream, profile);
    	d.bootstrap();
    	return d;
    }

    @Deprecated
    public static DemoInputStreamIterator iteratorForFile(String fileName, Profile... profile) throws IOException {
        return new DemoInputStreamIterator(demoInputStreamForStream(new FileInputStream(fileName), profile));
    }
    
    public static PeekIterator peekIteratorForFile(String fileName, Profile... profile) throws IOException {
        return new PeekIterator(demoInputStreamForStream(new FileInputStream(fileName), profile));
    }
    
    public static TickIterator tickIteratorForFile(String fileName, Profile... profile) throws IOException {
        return new TickIterator(demoInputStreamForStream(new FileInputStream(fileName), profile));
    }

    public static DemoIndex indexForStream(InputStream stream, Profile... profile) throws IOException {
        return new DemoIndex(new PeekIterator(demoInputStreamForStream(stream, profile)));
    }

    public static DemoIndex indexForFile(String fileName, Profile... profile) throws IOException {
        return new DemoIndex(iteratorForFile(fileName, profile));
    }
    
    public static CDemoFileInfo infoForFile(String fileName) throws IOException {
    	DemoInputStream s = null;
    	try {
	        s = demoInputStreamForStream(new FileInputStream(fileName), Profile.FILE_INFO);
	        s.skipToFileInfo();
	        return (CDemoFileInfo) s.read().getMessage();
    	} finally {
    		if (s != null) {
    			s.close();
    		}
    	}
    }
    
}
