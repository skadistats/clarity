package skadistats.clarity.processor.gameevents;

import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.CombatLogEntry;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.GameEventDescriptor;
import skadistats.clarity.model.s1.S1CombatLogEntry;
import skadistats.clarity.model.s1.S1CombatLogIndices;
import skadistats.clarity.model.s2.S2CombatLogEntry;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

import java.util.LinkedList;
import java.util.List;

@Provides({OnCombatLogEntry.class})
public class CombatLog {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

    public static final String STRING_TABLE_NAME = "CombatLogNames";
    public static final String GAME_EVENT_NAME = "dota_combatlog";

    @Insert
    private StringTables stringTables;
    @InsertEvent
    private Event<OnCombatLogEntry> evCombatLogEntry;

    private S1CombatLogIndices indices = null;

    private final List<CombatLogEntry> logEntries = new LinkedList<>();

    @OnGameEventDescriptor(GAME_EVENT_NAME)
    @UsesStringTable(STRING_TABLE_NAME)
    public void onGameEventDescriptor(GameEventDescriptor descriptor) {
        indices = new S1CombatLogIndices(descriptor);
    }

    @OnGameEvent(GAME_EVENT_NAME)
    public void onGameEvent(GameEvent gameEvent) {
        logEntries.add(new S1CombatLogEntry(
            indices,
            stringTables.forName(STRING_TABLE_NAME),
            gameEvent
        ));
    }

    private boolean logBulkData = true;

    @OnMessage(DotaUserMessages.CDOTAUserMsg_CombatLogBulkData.class)
    public void onCombatLogBulkData(DotaUserMessages.CDOTAUserMsg_CombatLogBulkData message) {
        if (logBulkData) {
            log.warn("This replay contains a CDOTAUserMsg_CombatLogBulkData message. I need one of those replays to analyze. Please report the match id: https://github.com/skadistats/clarity/issues/58");
            logBulkData = false;
        }
    }

    @OnMessage(DotaUserMessages.CMsgDOTACombatLogEntry.class)
    public void onCombatLogEntry(DotaUserMessages.CMsgDOTACombatLogEntry message) {
        logEntries.add(new S2CombatLogEntry(
            stringTables.forName(STRING_TABLE_NAME),
            message
        ));
    }

    @OnTickEnd
    public void onTickEnd(boolean synthetic) {
        for (CombatLogEntry e : logEntries) {
            evCombatLogEntry.raise(e);
        }
        logEntries.clear();
    }

}
