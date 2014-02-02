package clarity.parser;

import java.io.FileInputStream;
import java.io.IOException;

import clarity.parser.Profiles.Profile;

import com.google.protobuf.CodedInputStream;

public class DemoFile {

    public static DemoIndex indexForFile(String fileName) throws IOException {
        return indexForFile(fileName, (Profile[]) null);
    }
    
    public static DemoIndex indexForFile(String fileName, Profile... profile) throws IOException {
        return new DemoIndex(iteratorForFile(fileName, profile));
    }

    public static DemoInputStreamIterator iteratorForFile(String fileName, Profile... profile) throws IOException {
        CodedInputStream s = CodedInputStream.newInstance(new FileInputStream(fileName));
        s.setSizeLimit(Integer.MAX_VALUE);
        ensureHeader(s);
        s.skipRawBytes(4); // offset of epilogue
        return new DemoInputStreamIterator(
            new DemoInputStream(s, profile)
        );
    }
    
    private static void ensureHeader(CodedInputStream s) throws IOException {
        String header = new String(s.readRawBytes(8));
        if (!"PBUFDEM\0".equals(header)) {
            throw new IOException("replay does not have the proper header");
        }
    }

}
