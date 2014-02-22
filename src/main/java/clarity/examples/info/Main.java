package clarity.examples.info;

import clarity.parser.DemoFile;

import com.dota2.proto.Demo.CDemoFileInfo;

public class Main {
    
    public static void main(String[] args) throws Exception {

        CDemoFileInfo info = DemoFile.infoForFile(args[0]);
        System.out.println(info);
        
    }

}
