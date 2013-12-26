package clarity.parser.handler;

import clarity.match.Match;

import com.dota2.proto.Demo.CDemoClassInfo;
import com.dota2.proto.Demo.CDemoClassInfo.class_t;

public class DemClassInfoHandler implements Handler<CDemoClassInfo> {

	@Override
	public void apply(CDemoClassInfo message, Match match) {
		for (class_t ct : message.getClassesList()) {
			match.getDtClasses().setClassIdForDtName(ct.getTableName(), ct.getClassId());
		}
	}

}
