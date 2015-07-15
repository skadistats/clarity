package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;
import skadistats.clarity.wire.s1.proto.Demo.CDemoClassInfo;
import skadistats.clarity.wire.s1.proto.Demo.CDemoClassInfo.class_t;

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
