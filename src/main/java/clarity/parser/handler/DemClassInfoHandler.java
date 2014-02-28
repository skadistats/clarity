package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;
import clarity.parser.RegisterHandler;

import com.dota2.proto.Demo.CDemoClassInfo;
import com.dota2.proto.Demo.CDemoClassInfo.class_t;

@RegisterHandler(CDemoClassInfo.class)
public class DemClassInfoHandler implements Handler<CDemoClassInfo> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void apply(int peekTick, CDemoClassInfo message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        for (class_t ct : message.getClassesList()) {
            match.getDtClasses().setClassIdForDtName(ct.getTableName(), ct.getClassId());
        }
    }

}
