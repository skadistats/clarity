package skadistats.clarity.processor.gameevents;

import skadistats.clarity.event.Event;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.CombatLogEntry;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.GameEventDescriptor;
import skadistats.clarity.model.s1.S1CombatLogEntry;
import skadistats.clarity.model.s1.S1CombatLogIndices;
import skadistats.clarity.model.s2.S2CombatLogEntry;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

import java.util.LinkedList;
import java.util.List;

@Provides({OnCombatLogEntry.class})
public class CombatLog {

    public static final String STRING_TABLE_NAME = "CombatLogNames";
    public static final String GAME_EVENT_NAME = "dota_combatlog";

    private S1CombatLogIndices indices = null;

    private final List<CombatLogEntry> logEntries = new LinkedList<>();

    @OnGameEventDescriptor(GAME_EVENT_NAME)
    @UsesStringTable(STRING_TABLE_NAME)
    public void onGameEventDescriptor(Context ctx, GameEventDescriptor descriptor) {
        indices = new S1CombatLogIndices(descriptor);
    }

    @OnGameEvent(GAME_EVENT_NAME)
    public void onGameEvent(Context ctx, GameEvent gameEvent) {
        logEntries.add(new S1CombatLogEntry(
            indices,
            ctx.getProcessor(StringTables.class).forName(STRING_TABLE_NAME),
            gameEvent
        ));
    }

    @OnMessage(DotaUserMessages.CMsgDOTACombatLogEntry.class)
    public void onNewCombatLogMessage(Context ctx, DotaUserMessages.CMsgDOTACombatLogEntry message) {
        logEntries.add(new S2CombatLogEntry(
            ctx.getProcessor(StringTables.class).forName(STRING_TABLE_NAME),
            message
        ));
    }

    @OnTickEnd
    public void onTickEnd(Context ctx, boolean synthetic) {
        Event<OnCombatLogEntry> ev = ctx.createEvent(OnCombatLogEntry.class, CombatLogEntry.class);
        for (CombatLogEntry e : logEntries) {
            ev.raise(e);
        }
        logEntries.clear();
    }

}
