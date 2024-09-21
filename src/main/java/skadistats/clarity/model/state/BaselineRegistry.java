package skadistats.clarity.model.state;

import com.google.protobuf.ByteString;
import skadistats.clarity.model.StringTable;

public class BaselineRegistry {

    private final StringTable baselineTable;
    private final int entityCount;
    private final int classCount;
    private final int[] classBaselineIdx;
    private final EntityState[] baselineStateCache;
    private final int[] entityAlternateBaselineIdx;
    private final EntityState[][] entityBaselineStateCache;

    public BaselineRegistry(StringTable baselineTable, int entityCount) {
        this.baselineTable = baselineTable;

        this.entityCount = entityCount;
        this.classCount = baselineTable.getMaxEntries() != null ? baselineTable.getMaxEntries() : 4096;

        this.classBaselineIdx = new int[classCount];
        this.baselineStateCache = new EntityState[classCount];
        for (var i = 0; i < classCount; i++) {
            classBaselineIdx[i] = -1;
        }

        this.entityAlternateBaselineIdx = new int[entityCount];
        this.entityBaselineStateCache = new EntityState[entityCount][2];
        for (var i = 0; i < entityCount; i++) {
            this.entityAlternateBaselineIdx[i] = -1;
        }
    }

    public void clear() {
        for (var i = 0; i < classCount; i++) {
            baselineStateCache[i] = null;
        }
        for (var i = 0; i < entityCount; i++) {
            clearEntity(i);
        }
    }

    public void clearEntity(int entityIdx) {
        entityAlternateBaselineIdx[entityIdx] = -1;
        entityBaselineStateCache[entityIdx][0] = null;
        entityBaselineStateCache[entityIdx][1] = null;
    }

    public void updateClassBaselineIndex(int dtClassId, int baselineIdx) {
        classBaselineIdx[dtClassId] = baselineIdx;
    }

    public void updateEntityAlternateBaselineIndex(int entityIdx, int baselineIdx) {
        entityAlternateBaselineIdx[entityIdx] = baselineIdx;
    }

    public void markClassBaselineDirty(int baselineIdx) {
        baselineStateCache[baselineIdx] = null;
    }

    public ByteString getClassBaselineData(int baselineIdx) {
        return baselineTable.getValueByIndex(baselineIdx);
    }

    public int getClassBaselineIndex(int dtClassId, int entityIdx) {
        if (entityAlternateBaselineIdx[entityIdx] != -1) {
            return entityAlternateBaselineIdx[entityIdx];
        } else {
            return classBaselineIdx[dtClassId];
        }
    }

    public EntityState getClassBaselineState(int baselineIdx) {
        return baselineStateCache[baselineIdx];
    }

    public void setClassBaselineState(int baselineIdx, EntityState state) {
        baselineStateCache[baselineIdx] = state;
    }

    public EntityState getEntityBaselineState(int entityIdx, int baseline) {
        // FIXME: not sure whether to ignore entity baselines when an alternate baseline is found
        // FIXME: do so for now, remove next line if entity baseline should be preferred
        if (entityAlternateBaselineIdx[entityIdx] != -1) return null;
        return entityBaselineStateCache[entityIdx][baseline];
    }

    public void updateEntityBaseline(int iFrom, int entityIdx, EntityState state) {
        var iTo = 1 - iFrom;
        entityBaselineStateCache[entityIdx][iTo] = state;
    }

    public void switchEntityBaselines(int iFrom) {
        var iTo = 1 - iFrom;
        for (var j = 0; j < entityCount; j++) {
            entityBaselineStateCache[j][iTo] = entityBaselineStateCache[j][iFrom];
        }
    }

}
