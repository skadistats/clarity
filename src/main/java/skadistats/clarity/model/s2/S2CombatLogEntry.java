package skadistats.clarity.model.s2;

import skadistats.clarity.model.CombatLogEntry;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

import java.util.List;

public class S2CombatLogEntry implements CombatLogEntry {

    private final StringTable combatLogNames;
    private final DotaUserMessages.CMsgDOTACombatLogEntry e;

    public S2CombatLogEntry(StringTable combatLogNames, DotaUserMessages.CMsgDOTACombatLogEntry entry) {
        this.combatLogNames = combatLogNames;
        this.e = entry;
    }

    private String readCombatLogName(int idx) {
        return combatLogNames.hasIndex(idx) ? combatLogNames.getNameByIndex(idx) : null;
    }

    @Override
    public boolean hasType() {
        return e.hasType();
    }

    @Override
    public DotaUserMessages.DOTA_COMBATLOG_TYPES getType() {
        return e.getType();
    }

    @Override
    public boolean hasTargetName() {
        return e.hasTargetName();
    }

    @Override
    public String getTargetName() {
        return readCombatLogName(e.getTargetName());
    }

    @Override
    public boolean hasTargetSourceName() {
        return e.hasTargetSourceName();
    }

    @Override
    public String getTargetSourceName() {
        return readCombatLogName(e.getTargetSourceName());
    }

    @Override
    public boolean hasAttackerName() {
        return e.hasAttackerName();
    }

    @Override
    public String getAttackerName() {
        return readCombatLogName(e.getAttackerName());
    }

    @Override
    public boolean hasDamageSourceName() {
        return e.hasDamageSourceName();
    }

    @Override
    public String getDamageSourceName() {
        return readCombatLogName(e.getDamageSourceName());
    }

    @Override
    public boolean hasInflictorName() {
        return e.hasInflictorName();
    }

    @Override
    public String getInflictorName() {
        return readCombatLogName(e.getInflictorName());
    }

    @Override
    public boolean hasAttackerIllusion() {
        return e.hasIsAttackerIllusion();
    }

    @Override
    public boolean isAttackerIllusion() {
        return e.getIsAttackerIllusion();
    }

    @Override
    public boolean hasAttackerHero() {
        return e.hasIsAttackerHero();
    }

    @Override
    public boolean isAttackerHero() {
        return e.getIsAttackerHero();
    }

    @Override
    public boolean hasTargetIllusion() {
        return e.hasIsTargetIllusion();
    }

    @Override
    public boolean isTargetIllusion() {
        return e.getIsTargetIllusion();
    }

    @Override
    public boolean hasTargetHero() {
        return e.hasIsTargetHero();
    }

    @Override
    public boolean isTargetHero() {
        return e.getIsTargetHero();
    }

    @Override
    public boolean hasVisibleRadiant() {
        return e.hasIsVisibleRadiant();
    }

    @Override
    public boolean isVisibleRadiant() {
        return e.getIsVisibleRadiant();
    }

    @Override
    public boolean hasVisibleDire() {
        return e.hasIsVisibleDire();
    }

    @Override
    public boolean isVisibleDire() {
        return e.getIsVisibleDire();
    }

    @Override
    public boolean hasValue() {
        return e.hasValue();
    }

    @Override
    public int getValue() {
        return e.getValue();
    }

    @Override
    public String getValueName() {
        return readCombatLogName(e.getValue());
    }

    @Override
    public boolean hasHealth() {
        return e.hasHealth();
    }

    @Override
    public int getHealth() {
        return e.getHealth();
    }

    @Override
    public boolean hasTimestamp() {
        return e.hasTimestamp();
    }

    @Override
    public float getTimestamp() {
        return e.getTimestamp();
    }

    @Override
    public boolean hasStunDuration() {
        return e.hasStunDuration();
    }

    @Override
    public float getStunDuration() {
        return e.getStunDuration();
    }

    @Override
    public boolean hasSlowDuration() {
        return e.hasSlowDuration();
    }

    @Override
    public float getSlowDuration() {
        return e.getSlowDuration();
    }

    @Override
    public boolean hasAbilityToggleOn() {
        return e.hasIsAbilityToggleOn();
    }

    @Override
    public boolean isAbilityToggleOn() {
        return e.getIsAbilityToggleOn();
    }

    @Override
    public boolean hasAbilityToggleOff() {
        return e.hasIsAbilityToggleOff();
    }

    @Override
    public boolean isAbilityToggleOff() {
        return e.getIsAbilityToggleOff();
    }

    @Override
    public boolean hasAbilityLevel() {
        return e.hasAbilityLevel();
    }

    @Override
    public int getAbilityLevel() {
        return e.getAbilityLevel();
    }

    @Override
    public boolean hasLocationX() {
        return e.hasLocationX();
    }

    @Override
    public float getLocationX() {
        return e.getLocationX();
    }

    @Override
    public boolean hasLocationY() {
        return e.hasLocationY();
    }

    @Override
    public float getLocationY() {
        return e.getLocationY();
    }

    @Override
    public boolean hasGoldReason() {
        return e.hasGoldReason();
    }

    @Override
    public int getGoldReason() {
        return e.getGoldReason();
    }

    @Override
    public boolean hasTimestampRaw() {
        return e.hasTimestampRaw();
    }

    @Override
    public float getTimestampRaw() {
        return e.getTimestampRaw();
    }

    @Override
    public boolean hasModifierDuration() {
        return e.hasModifierDuration();
    }

    @Override
    public float getModifierDuration() {
        return e.getModifierDuration();
    }

    @Override
    public boolean hasXpReason() {
        return e.hasXpReason();
    }

    @Override
    public int getXpReason() {
        return e.getXpReason();
    }

    @Override
    public boolean hasLastHits() {
        return e.hasLastHits();
    }

    @Override
    public int getLastHits() {
        return e.getLastHits();
    }

    @Override
    public boolean hasAttackerTeam() {
        return e.hasAttackerTeam();
    }

    @Override
    public int getAttackerTeam() {
        return e.getAttackerTeam();
    }

    @Override
    public boolean hasTargetTeam() {
        return e.hasTargetTeam();
    }

    @Override
    public int getTargetTeam() {
        return e.getTargetTeam();
    }

    @Override
    public boolean hasObsWardsPlaced() {
        return e.hasObsWardsPlaced();
    }

    @Override
    public int getObsWardsPlaced() {
        return e.getObsWardsPlaced();
    }

    @Override
    public boolean hasAssistPlayers() {
        return e.getAssistPlayersCount() != 0;
    }

    @Override
    public List<Integer> getAssistPlayers() {
        return e.getAssistPlayersList();
    }

    @Override
    public boolean hasStackCount() {
        return e.hasStackCount();
    }

    @Override
    public int getStackCount() {
        return e.getStackCount();
    }

    @Override
    public boolean hasHiddenModifier() {
        return e.hasHiddenModifier();
    }

    @Override
    public boolean getHiddenModifier() {
        return e.getHiddenModifier();
    }

    @Override
    public boolean hasTargetBuilding() {
        return e.hasIsTargetBuilding();
    }

    @Override
    public boolean isTargetBuilding() {
        return e.getIsTargetBuilding();
    }

    @Override
    public boolean hasNeutralCampType() {
        return e.hasNeutralCampType();
    }

    @Override
    public int getNeutralCampType() {
        return e.getNeutralCampType();
    }

    @Override
    public boolean hasRuneType() {
        return e.hasRuneType();
    }

    @Override
    public int getRuneType() {
        return e.getRuneType();
    }

    @Override
    public boolean hasHealSave() {
        return e.hasIsHealSave();
    }

    @Override
    public boolean isHealSave() {
        return e.getIsHealSave();
    }

    @Override
    public boolean hasUltimateAbility() {
        return e.hasIsUltimateAbility();
    }

    @Override
    public boolean isUltimateAbility() {
        return e.getIsUltimateAbility();
    }

    @Override
    public boolean hasAttackerHeroLevel() {
        return e.hasAttackerHeroLevel();
    }

    @Override
    public int getAttackerHeroLevel() {
        return e.getAttackerHeroLevel();
    }

    @Override
    public boolean hasTargetHeroLevel() {
        return e.hasTargetHeroLevel();
    }

    @Override
    public int getTargetHeroLevel() {
        return e.getTargetHeroLevel();
    }

    @Override
    public boolean hasXpm() {
        return e.hasXpm();
    }

    @Override
    public int getXpm() {
        return e.getXpm();
    }

    @Override
    public boolean hasGpm() {
        return e.hasGpm();
    }

    @Override
    public int getGpm() {
        return e.getGpm();
    }

    @Override
    public boolean hasEventLocation() {
        return e.hasEventLocation();
    }

    @Override
    public int getEventLocation() {
        return e.getEventLocation();
    }

    @Override
    public boolean hasTargetSelf() {
        return e.hasTargetIsSelf();
    }

    @Override
    public boolean isTargetSelf() {
        return e.getTargetIsSelf();
    }

    @Override
    public boolean hasDamageType() {
        return e.hasDamageType();
    }

    @Override
    public int getDamageType() {
        return e.getDamageType();
    }

    @Override
    public boolean hasInvisibilityModifier() {
        return e.hasInvisibilityModifier();
    }

    @Override
    public boolean isInvisibilityModifier() {
        return e.getInvisibilityModifier();
    }

    @Override
    public boolean hasDamageCategory() {
        return e.hasDamageCategory();
    }

    @Override
    public int getDamageCategory() {
        return e.getDamageCategory();
    }

    @Override
    public boolean hasNetworth() {
        return e.hasNetworth();
    }

    @Override
    public int getNetworth() {
        return e.getNetworth();
    }

    public String toString() {
        return e.toString();
    }

}
