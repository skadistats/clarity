package clarity.parser.handler;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.Demo.CDemoFileHeader;

public class DemFileHeaderHandler implements Handler<CDemoFileHeader> {

    @Override
    public void apply(CDemoFileHeader message, Match match) {
        System.out.println(message);
    }

}
