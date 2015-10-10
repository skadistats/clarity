package skadistats.clarity.model.s1;

import skadistats.clarity.model.CombatLogEntry;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

public class S1CombatLogEntry implements CombatLogEntry {

    private final S1CombatLogIndices indices;
    private final StringTable combatLogNames;
    private final GameEvent e;

    public S1CombatLogEntry(S1CombatLogIndices indices, StringTable combatLogNames, GameEvent event) {
        this.indices = indices;
        this.combatLogNames = combatLogNames;
        this.e = event;
    }

    private String readCombatLogName(int idx) {
        return idx == 0 ? null : combatLogNames.getNameByIndex(idx);
    }

    @Override
    public boolean hasType() {
        return true;
    }

    @Override
    public DotaUserMessages.DOTA_COMBATLOG_TYPES getType() {
        return DotaUserMessages.DOTA_COMBATLOG_TYPES.valueOf((int) e.getProperty(indices.typeIdx));
    }

    @Override
    public boolean hasTargetName() {
        return (int) e.getProperty(indices.targetNameIdx) != 0;
    }

    @Override
    public String getTargetName() {
        return readCombatLogName((int) e.getProperty(indices.targetNameIdx));
    }

    @Override
    public boolean hasTargetSourceName() {
        return (int) e.getProperty(indices.targetSourceNameIdx) != 0;
    }

    @Override
    public String getTargetSourceName() {
        return readCombatLogName((int) e.getProperty(indices.targetSourceNameIdx));
    }

    @Override
    public boolean hasAttackerName() {
        return (int) e.getProperty(indices.attackerNameIdx) != 0;
    }

    @Override
    public String getAttackerName() {
        return readCombatLogName((int) e.getProperty(indices.attackerNameIdx));
    }

    @Override
    public boolean hasDamageSourceName() {
        return (int) e.getProperty(indices.sourceNameIdx) != 0;
    }

    @Override
    public String getDamageSourceName() {
        return readCombatLogName((int) e.getProperty(indices.sourceNameIdx));
    }

    @Override
    public boolean hasInflictorName() {
        return (int) e.getProperty(indices.inflictorNameIdx) != 0;
    }

    @Override
    public String getInflictorName() {
        return readCombatLogName((int) e.getProperty(indices.inflictorNameIdx));
    }

    @Override
    public boolean hasAttackerIllusion() {
        return true;
    }

    @Override
    public boolean isAttackerIllusion() {
        return e.getProperty(indices.attackerIllusionIdx);
    }

    @Override
    public boolean hasAttackerHero() {
        return indices.attackerHeroIdx != null;
    }

    @Override
    public boolean isAttackerHero() {
        return e.getProperty(indices.attackerHeroIdx);
    }

    @Override
    public boolean hasTargetIllusion() {
        return true;
    }

    @Override
    public boolean isTargetIllusion() {
        return e.getProperty(indices.targetIllusionIdx);
    }

    @Override
    public boolean hasTargetHero() {
        return indices.targetHeroIdx != null;
    }

    @Override
    public boolean isTargetHero() {
        return e.getProperty(indices.targetHeroIdx);
    }

    @Override
    public boolean hasVisibleRadiant() {
        return false;
    }

    @Override
    public boolean isVisibleRadiant() {
        return false;
    }

    @Override
    public boolean hasVisibleDire() {
        return false;
    }

    @Override
    public boolean isVisibleDire() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public int getValue() {
        return e.getProperty(indices.valueIdx);
    }

    @Override
    public String getValueName() {
        return readCombatLogName((int)e.getProperty(indices.valueIdx));
    }

    @Override
    public boolean hasHealth() {
        return true;
    }

    @Override
    public int getHealth() {
        return e.getProperty(indices.healthIdx);
    }

    @Override
    public boolean hasTimestamp() {
        return true;
    }

    @Override
    public float getTimestamp() {
        return e.getProperty(indices.timestampIdx);
    }

    @Override
    public boolean hasStunDuration() {
        return false;
    }

    @Override
    public float getStunDuration() {
        return 0;
    }

    @Override
    public boolean hasSlowDuration() {
        return false;
    }

    @Override
    public float getSlowDuration() {
        return 0;
    }

    @Override
    public boolean hasAbilityToggleOn() {
        return indices.abilityToggleOnIdx != null;
    }

    @Override
    public boolean isAbilityToggleOn() {
        return e.getProperty(indices.abilityToggleOnIdx);
    }

    @Override
    public boolean hasAbilityToggleOff() {
        return indices.abilityToggleOffIdx != null;
    }

    @Override
    public boolean isAbilityToggleOff() {
        return e.getProperty(indices.abilityToggleOffIdx);
    }

    @Override
    public boolean hasAbilityLevel() {
        return indices.abilityLevelIdx != null;
    }

    @Override
    public int getAbilityLevel() {
        return e.getProperty(indices.abilityLevelIdx);
    }

    @Override
    public boolean hasLocationX() {
        return false;
    }

    @Override
    public float getLocationX() {
        return 0;
    }

    @Override
    public boolean hasLocationY() {
        return false;
    }

    @Override
    public float getLocationY() {
        return 0;
    }

    @Override
    public boolean hasGoldReason() {
        return indices.goldReasonIdx != null;
    }

    @Override
    public int getGoldReason() {
        return e.getProperty(indices.goldReasonIdx);
    }

    @Override
    public boolean hasTimestampRaw() {
        return indices.timestampRawIdx != null;
    }

    @Override
    public float getTimestampRaw() {
        return e.getProperty(indices.timestampRawIdx);
    }

    @Override
    public boolean hasModifierDuration() {
        return false;
    }

    @Override
    public float getModifierDuration() {
        return 0;
    }

    @Override
    public boolean hasXpReason() {
        return indices.xpReasonIdx != null;
    }

    @Override
    public int getXpReason() {
        return e.getProperty(indices.xpReasonIdx);
    }

    @Override
    public boolean hasLastHits() {
        return false;
    }

    @Override
    public int getLastHits() {
        return 0;
    }

    @Override
    public boolean hasAttackerTeam() {
        return false;
    }

    @Override
    public int getAttackerTeam() {
        return 0;
    }

    @Override
    public boolean hasTargetTeam() {
        return false;
    }

    @Override
    public int getTargetTeam() {
        return 0;
    }

    @Override
    public boolean hasObsWardsPlaced() {
        return false;
    }

    @Override
    public int getObsWardsPlaced() {
        return 0;
    }

    @Override
    public boolean hasAssistPlayer0() {
        return false;
    }

    @Override
    public int getAssistPlayer0() {
        return 0;
    }

    @Override
    public boolean hasAssistPlayer1() {
        return false;
    }

    @Override
    public int getAssistPlayer1() {
        return 0;
    }

    @Override
    public boolean hasAssistPlayer2() {
        return false;
    }

    @Override
    public int getAssistPlayer2() {
        return 0;
    }

    @Override
    public boolean hasAssistPlayer3() {
        return false;
    }

    @Override
    public int getAssistPlayer3() {
        return 0;
    }

    @Override
    public boolean hasStackCount() {
        return false;
    }

    @Override
    public int getStackCount() {
        return 0;
    }

    @Override
    public boolean hasHiddenModifier() {
        return false;
    }

    @Override
    public boolean getHiddenModifier() {
        return false;
    }

    @Override
    public boolean hasTargetBuilding() {
        return false;
    }

    @Override
    public boolean isTargetBuilding() {
        return false;
    }

    @Override
    public boolean hasNeutralCampType() {
        return false;
    }

    @Override
    public int getNeutralCampType() {
        return 0;
    }

    @Override
    public boolean hasRuneType() {
        return false;
    }

    @Override
    public int getRuneType() {
        return 0;
    }

    public String toString() {
        return e.toString();
    }
}

