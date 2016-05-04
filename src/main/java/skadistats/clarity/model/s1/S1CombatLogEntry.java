package skadistats.clarity.model.s1;

import skadistats.clarity.model.CombatLogEntry;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

import java.util.ArrayList;
import java.util.List;

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
        return indices.attackerHeroIdx != null ? (boolean) e.getProperty(indices.attackerHeroIdx) : true;
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
        return indices.targetHeroIdx != null ? (boolean) e.getProperty(indices.targetHeroIdx) : true;
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
        return indices.abilityToggleOnIdx != null ? (boolean) e.getProperty(indices.abilityToggleOnIdx) : false;
    }

    @Override
    public boolean hasAbilityToggleOff() {
        return indices.abilityToggleOffIdx != null;
    }

    @Override
    public boolean isAbilityToggleOff() {
        return indices.abilityToggleOffIdx != null ? (boolean) e.getProperty(indices.abilityToggleOffIdx) : false;
    }

    @Override
    public boolean hasAbilityLevel() {
        return indices.abilityLevelIdx != null;
    }

    @Override
    public int getAbilityLevel() {
        return indices.abilityLevelIdx != null ? (int) e.getProperty(indices.abilityLevelIdx) : 0;
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
        return indices.goldReasonIdx != null ? (int) e.getProperty(indices.goldReasonIdx) : 0;
    }

    @Override
    public boolean hasTimestampRaw() {
        return indices.timestampRawIdx != null;
    }

    @Override
    public float getTimestampRaw() {
        return indices.timestampRawIdx == null ? (float) e.getProperty(indices.timestampRawIdx) : 0.0f;
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
        return indices.xpReasonIdx != null ? (int) e.getProperty(indices.xpReasonIdx) : 0;
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
    public boolean hasAssistPlayers() {
        return false;
    }

    @Override
    public List<Integer> getAssistPlayers() {
        return new ArrayList<>();
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

    @Override
    public boolean hasHealSave() {
        return false;
    }

    @Override
    public boolean isHealSave() {
        return false;
    }

    @Override
    public boolean hasUltimateAbility() {
        return false;
    }

    @Override
    public boolean isUltimateAbility() {
        return false;
    }

    @Override
    public boolean hasAttackerHeroLevel() {
        return false;
    }

    @Override
    public int getAttackerHeroLevel() {
        return 0;
    }

    @Override
    public boolean hasTargetHeroLevel() {
        return false;
    }

    @Override
    public int getTargetHeroLevel() {
        return 0;
    }

    @Override
    public boolean hasXpm() {
        return false;
    }

    @Override
    public int getXpm() {
        return 0;
    }

    @Override
    public boolean hasGpm() {
        return false;
    }

    @Override
    public int getGpm() {
        return 0;
    }

    @Override
    public boolean hasEventLocation() {
        return false;
    }

    @Override
    public int getEventLocation() {
        return 0;
    }

    @Override
    public boolean hasTargetSelf() {
        return false;
    }

    @Override
    public boolean isTargetSelf() {
        return false;
    }

    @Override
    public boolean hasDamageType() {
        return false;
    }

    @Override
    public int getDamageType() {
        return 0;
    }

    @Override
    public boolean hasInvisibilityModifier() {
        return false;
    }

    @Override
    public boolean isInvisibilityModifier() {
        return false;
    }

    @Override
    public boolean hasDamageCategory() {
        return false;
    }

    @Override
    public int getDamageCategory() {
        return 0;
    }

    @Override
    public boolean hasNetworth() {
        return false;
    }

    @Override
    public int getNetworth() {
        return 0;
    }

    public String toString() {
        return e.toString();
    }
}

