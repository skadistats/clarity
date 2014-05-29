package skadistats.clarity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xerial.snappy.Snappy;

import skadistats.clarity.parser.DemoIndex;
import skadistats.clarity.parser.DemoInputStream;
import skadistats.clarity.parser.DemoInputStreamIterator;
import skadistats.clarity.parser.PeekIterator;
import skadistats.clarity.parser.Profile;
import skadistats.clarity.parser.TickIterator;

import com.dota2.proto.Demo.CDemoFileInfo;
import com.dota2.proto.Demo.EDemoCommands;
import com.google.protobuf.CodedInputStream;

public class Clarity {

    private static DemoInputStream demoInputStreamForStream(InputStream stream, Profile... profile) throws IOException {
        CodedInputStream s = CodedInputStream.newInstance(stream);
        s.setSizeLimit(Integer.MAX_VALUE);
        ensureHeader(s);
        s.skipRawBytes(4); // offset of epilogue
        return new DemoInputStream(s, profile);
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
        CodedInputStream s = CodedInputStream.newInstance(new FileInputStream(fileName));
        s.setSizeLimit(Integer.MAX_VALUE);
        ensureHeader(s);
        int offset = s.readFixed32();
        s.skipRawBytes(offset - 12);
        boolean isCompressed = (s.readRawVarint32() & EDemoCommands.DEM_IsCompressed_VALUE) == EDemoCommands.DEM_IsCompressed_VALUE;
        s.readRawVarint32(); // skip peek tick
        int size = s.readRawVarint32();
        byte[] data = s.readRawBytes(size);
        if (isCompressed) {
            data = Snappy.uncompress(data);
        }
        return CDemoFileInfo.parseFrom(data);
    }
    
    
    private static void ensureHeader(CodedInputStream s) throws IOException {
        String header = new String(s.readRawBytes(8));
        if (!"PBUFDEM\0".equals(header)) {
            throw new IOException("replay does not have the proper header");
        }
    }

}
