package clarity.parser;

import java.io.FileInputStream;
import java.io.IOException;

import com.google.protobuf.CodedInputStream;

public class ReplayFile {

    public static ReplayIndex indexForFile(String fileName) throws IOException {
        CodedInputStream s = CodedInputStream.newInstance(new FileInputStream(fileName));
        s.setSizeLimit(Integer.MAX_VALUE);
        ensureHeader(s);
        s.skipRawBytes(4); // offset of epilogue
        return new ReplayIndex(s);
    }

    private static void ensureHeader(CodedInputStream s) throws IOException {
        String header = new String(s.readRawBytes(8));
        if (!"PBUFDEM\0".equals(header)) {
            throw new IOException("replay does not have the proper header");
        }
    }

}
