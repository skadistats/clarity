package skadistats.clarity.two.processor.gameevents;

import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.GameEventDescriptor;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.two.framework.annotation.Provides;
import skadistats.clarity.two.framework.invocation.Event;
import skadistats.clarity.two.processor.reader.OnTickEnd;
import skadistats.clarity.two.processor.runner.Context;
import skadistats.clarity.two.processor.stringtables.StringTables;
import skadistats.clarity.two.processor.stringtables.UsesStringTable;

import java.util.LinkedList;
import java.util.List;

@Provides({OnCombatLogEntry.class})
public class CombatLog {

    public static final String STRING_TABLE_NAME = "CombatLogNames";
    public static final String GAME_EVENT_NAME   = "dota_combatlog";

    private final List<Entry> logEntries = new LinkedList<>();

    @OnGameEventDescriptor(GAME_EVENT_NAME)
    @UsesStringTable(CombatLog.STRING_TABLE_NAME)
    public void onGameEventDescriptor(Context ctx, GameEventDescriptor descriptor){
        typeIdx = descriptor.getIndexForKey("type");
        sourceNameIdx = descriptor.getIndexForKey("sourcename");
        targetNameIdx = descriptor.getIndexForKey("targetname");
        attackerNameIdx = descriptor.getIndexForKey("attackername");
        inflictorNameIdx = descriptor.getIndexForKey("inflictorname");
        attackerIllusionIdx = descriptor.getIndexForKey("attackerillusion");
        targetIllusionIdx = descriptor.getIndexForKey("targetillusion");
        valueIdx = descriptor.getIndexForKey("value");
        healthIdx = descriptor.getIndexForKey("health");
        timestampIdx = descriptor.getIndexForKey("timestamp");
        targetSourceNameIdx = descriptor.getIndexForKey("targetsourcename");

        timestampRawIdx = descriptor.getIndexForKey("timestampraw");
        attackerHeroIdx = descriptor.getIndexForKey("attackerhero");
        targetHeroIdx = descriptor.getIndexForKey("targethero");
        abilityToggleOnIdx = descriptor.getIndexForKey("ability_toggle_on");
        abilityToggleOffIdx = descriptor.getIndexForKey("ability_toggle_off");
        abilityLevelIdx = descriptor.getIndexForKey("ability_level");
        goldReasonIdx = descriptor.getIndexForKey("gold_reason");
    }

    @OnGameEvent(GAME_EVENT_NAME)
    public void onGameEvent(Context ctx, GameEvent gameEvent) {
        logEntries.add(new Entry(ctx, gameEvent));
    }

    @OnTickEnd
    public void onTickEnd(Context ctx) {
        Event<OnCombatLogEntry> ev = ctx.createEvent(OnCombatLogEntry.class, Entry.class);
        for (Entry e : logEntries) {
            ev.raise(e);
        }
        logEntries.clear();
    }

    private int typeIdx;
    private int sourceNameIdx;
    private int targetNameIdx;
    private int attackerNameIdx;
    private int inflictorNameIdx;
    private int attackerIllusionIdx;
    private int targetIllusionIdx;
    private int valueIdx;
    private int healthIdx;
    private int timestampIdx;
    private int targetSourceNameIdx;
    private Integer timestampRawIdx;
    private Integer attackerHeroIdx;
    private Integer targetHeroIdx;
    private Integer abilityToggleOnIdx;
    private Integer abilityToggleOffIdx;
    private Integer abilityLevelIdx;
    private Integer goldReasonIdx;

    public class Entry {

        private final StringTable combatLogNames;
        private final GameEvent event;

        private Entry(Context ctx, GameEvent event) {
            this.combatLogNames = ctx.getProcessor(StringTables.class).getByName(STRING_TABLE_NAME);
            this.event = event;
        }

        private String readCombatLogName(int idx) {
            return idx == 0 ? null : combatLogNames.getNameByIndex(idx);
        }

        private String translate(String in) {
            // TODO: translate modifier_XXX, or npc_hero_XXX into correct names...
            return in;
        }

        public GameEvent getGameEvent() {
            return event;
        }

        public int getType() {
            return event.getProperty(typeIdx);
        }

        public String getSourceName() {
            return translate(readCombatLogName((int)event.getProperty(sourceNameIdx)));
        }

        public String getTargetName() {
            return translate(readCombatLogName((int)event.getProperty(targetNameIdx)));
        }

        public String getTargetNameCompiled() {
            return getTargetName() + (isTargetIllusion() ? " (Illusion)" : "");
        }

        public String getAttackerName() {
            return translate(readCombatLogName((int)event.getProperty(attackerNameIdx)));
        }

        public String getAttackerNameCompiled() {
            return getAttackerName() + (isAttackerIllusion() ? " (Illusion)" : "");
        }

        public String getInflictorName() {
            return translate(readCombatLogName((int)event.getProperty(inflictorNameIdx)));
        }

        public boolean isAttackerIllusion() {
            return event.getProperty(attackerIllusionIdx);
        }

        public boolean isTargetIllusion() {
            return event.getProperty(targetIllusionIdx);
        }

        public int getValue() {
            return event.getProperty(valueIdx);
        }

        public int getHealth() {
            return event.getProperty(healthIdx);
        }

        public float getTimestamp() {
            return event.getProperty(timestampIdx);
        }

        public String getTargetSourceName() {
            return translate(readCombatLogName((int)event.getProperty(targetSourceNameIdx)));
        }

        public float getTimestampRaw() {
            return event.getProperty(timestampRawIdx);
        }

        public boolean isAttackerHero() {
            return event.getProperty(attackerHeroIdx);
        }

        public boolean isTargetHero() {
            return event.getProperty(targetHeroIdx);
        }

        public boolean isAbilityToggleOn() {
            return event.getProperty(abilityToggleOnIdx);
        }

        public boolean isAbilityToggleOff() {
            return event.getProperty(abilityToggleOffIdx);
        }

        public int getAbilityLevel() {
            return event.getProperty(abilityLevelIdx);
        }

        public int getGoldReason() {
            return event.getProperty(goldReasonIdx);
        }
    }

}
