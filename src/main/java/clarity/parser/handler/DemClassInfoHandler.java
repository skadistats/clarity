package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.parser.Handler;

import com.dota2.proto.Demo.CDemoClassInfo;
import com.dota2.proto.Demo.CDemoClassInfo.class_t;

public class DemClassInfoHandler implements Handler<CDemoClassInfo> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void apply(CDemoClassInfo message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        for (class_t ct : message.getClassesList()) {
            match.getDtClasses().setClassIdForDtName(ct.getTableName(), ct.getClassId());
        }
    }

}
