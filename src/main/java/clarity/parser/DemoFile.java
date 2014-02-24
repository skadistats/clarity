package clarity.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xerial.snappy.Snappy;

import clarity.parser.Profiles.Profile;

import com.dota2.proto.Demo.CDemoFileInfo;
import com.dota2.proto.Demo.EDemoCommands;
import com.google.protobuf.CodedInputStream;

public class DemoFile {

    public static DemoIndex indexForFile(String fileName, Profile... profile) throws IOException {
        return new DemoIndex(iteratorForFile(fileName, profile));
    }

    public static DemoInputStreamIterator iteratorForStream(InputStream stream, Profile... profile) throws IOException {
        CodedInputStream s = CodedInputStream.newInstance(stream);
        s.setSizeLimit(Integer.MAX_VALUE);
        ensureHeader(s);
        s.skipRawBytes(4); // offset of epilogue
        return new DemoInputStreamIterator(
            new DemoInputStream(s, profile)
        );
    }

    public static DemoInputStreamIterator iteratorForFile(String fileName, Profile... profile) throws IOException {
        return iteratorForStream(new FileInputStream(fileName), profile);
    }
    
    public static CDemoFileInfo infoForFile(String fileName) throws IOException {
        CodedInputStream s = CodedInputStream.newInstance(new FileInputStream(fileName));
        s.setSizeLimit(Integer.MAX_VALUE);
        ensureHeader(s);
        int offset = s.readFixed32();
        s.skipRawBytes(offset - 12);
        int kind = s.readRawVarint32();
        boolean isCompressed = (kind & EDemoCommands.DEM_IsCompressed_VALUE) != 0;
        kind &= ~EDemoCommands.DEM_IsCompressed_VALUE;
        int peekTick = s.readRawVarint32();
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
