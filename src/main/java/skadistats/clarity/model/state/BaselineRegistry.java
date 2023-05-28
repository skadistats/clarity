package skadistats.clarity.model.state;

import com.google.protobuf.ByteString;
import skadistats.clarity.model.StringTable;

public class BaselineRegistry {

    private final StringTable baselineTable;
    private final int entityCount;
    private final int classCount;
    private final int[] classBaselineIdx;
    private final EntityState[] baselineStateCache;
    private final int[][] entityBaselineDtClass;
    private final int[] entityAlternateBaselineIdx;
    private final EntityState[][] entityBaselineStateCache;

    public BaselineRegistry(StringTable baselineTable, int entityCount) {
        this.baselineTable = baselineTable;

        this.entityCount = entityCount;
        this.classCount = baselineTable.getMaxEntries() != null ? baselineTable.getMaxEntries() : 4096;

        this.classBaselineIdx = new int[classCount];
        this.baselineStateCache = new EntityState[classCount];
        for (int i = 0; i < classCount; i++) {
            classBaselineIdx[i] = -1;
        }

        this.entityAlternateBaselineIdx = new int[entityCount];
        this.entityBaselineDtClass = new int[entityCount][2];
        this.entityBaselineStateCache = new EntityState[entityCount][2];
        for (int i = 0; i < entityCount; i++) {
            entityBaselineDtClass[i][0] = -1;
            entityBaselineDtClass[i][1] = -1;
            this.entityAlternateBaselineIdx[i] = -1;
        }
    }

    public void clear() {
        for (int i = 0; i < classCount; i++) {
            baselineStateCache[i] = null;
        }
        for (int i = 0; i < entityCount; i++) {
            entityBaselineDtClass[i][0] = -1;
            entityBaselineDtClass[i][1] = -1;
            entityAlternateBaselineIdx[i] = -1;
            entityBaselineStateCache[i][0] = null;
            entityBaselineStateCache[i][1] = null;
        }
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

    public EntityState getEntityBaselineState(int entityIdx, int baseline, int dtClassId) {
        // FIXME: not sure whether to ignore entity baselines when an alternate baseline is found
        // FIXME: do so for now, remove next line if entity baseline should be preferred
        if (entityAlternateBaselineIdx[entityIdx] != -1) return null;
        if (entityBaselineDtClass[entityIdx][baseline] != dtClassId) return null;
        return entityBaselineStateCache[entityIdx][baseline];
    }

    public void updateEntityBaseline(int iFrom, int entityIdx, int dtClassId, EntityState state) {
        int iTo = 1 - iFrom;
        entityBaselineDtClass[entityIdx][iTo] = dtClassId;
        entityBaselineStateCache[entityIdx][iTo] = state;
    }

    public void switchEntityBaselines(int iFrom) {
        int iTo = 1 - iFrom;
        for (int j = 0; j < entityCount; j++) {
            entityBaselineDtClass[j][iTo] = entityBaselineDtClass[j][iFrom];
            entityBaselineStateCache[j][iTo] = entityBaselineStateCache[j][iFrom];
        }
    }

}
