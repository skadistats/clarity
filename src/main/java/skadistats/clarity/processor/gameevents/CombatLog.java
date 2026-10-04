package skadistats.clarity.processor.gameevents;

import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
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
import skadistats.clarity.wire.dota.common.proto.DOTACombatLog;
import skadistats.clarity.wire.dota.common.proto.DOTAUserMessages;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides {@link OnCombatLogEntry} for Dota 2 combat log entries.
 * <p>
 * Source 1 replays deliver entries as {@code dota_combatlog} game events (using the {@code "CombatLogNames"} string
 * table); Source 2 replays as {@code CMsgDOTACombatLogEntry} messages. Entries are collected during a tick and
 * raised at the end of the tick.
 */
@Provides({OnCombatLogEntry.class})
public class CombatLog {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

    /** Name of the string table holding the names referenced by combat log entries. */
    public static final String STRING_TABLE_NAME = "CombatLogNames";
    /** Name of the Source 1 game event that carries a combat log entry. */
    public static final String GAME_EVENT_NAME = "dota_combatlog";

    @Insert
    private StringTables stringTables;
    @InsertEvent
    private OnCombatLogEntry.Event evCombatLogEntry;

    private S1CombatLogIndices indices = null;

    private final List<CombatLogEntry> logEntries = new LinkedList<>();

    /** Internal: reads the field indices of the combat log game event. */
    @OnGameEventDescriptor(GAME_EVENT_NAME)
    @UsesStringTable(STRING_TABLE_NAME)
    public void onGameEventDescriptor(GameEventDescriptor descriptor) {
        indices = new S1CombatLogIndices(descriptor);
    }

    /** Internal: converts a Source 1 combat log game event to an entry. */
    @OnGameEvent(GAME_EVENT_NAME)
    public void onGameEvent(GameEvent gameEvent) {
        logEntries.add(new S1CombatLogEntry(
            indices,
            stringTables.forName(STRING_TABLE_NAME),
            gameEvent
        ));
    }

    private boolean logBulkData = true;

    /** Internal: logs a warning, bulk data is not decoded. */
    @OnMessage(DOTAUserMessages.CDOTAUserMsg_CombatLogBulkData.class)
    public void onCombatLogBulkData(DOTAUserMessages.CDOTAUserMsg_CombatLogBulkData message) {
        if (logBulkData) {
            log.warn("This replay contains a CDOTAUserMsg_CombatLogBulkData message. I need one of those replays to analyze. Please report the match id: https://github.com/skadistats/clarity/issues/58");
            logBulkData = false;
        }
    }

    /** Internal: converts a Source 2 combat log message to an entry. */
    @OnMessage(DOTACombatLog.CMsgDOTACombatLogEntry.class)
    public void onCombatLogEntry(DOTACombatLog.CMsgDOTACombatLogEntry message) {
        logEntries.add(new S2CombatLogEntry(
            stringTables.forName(STRING_TABLE_NAME),
            message
        ));
    }

    /** Internal: raises the collected entries. */
    @OnTickEnd
    public void onTickEnd(boolean synthetic) {
        for (var e : logEntries) {
            evCombatLogEntry.raise(e);
        }
        logEntries.clear();
    }

}
