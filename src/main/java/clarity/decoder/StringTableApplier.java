package clarity.decoder;


import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.model.Entity;

import com.dota2.proto.DotaModifiers.CDOTAModifierBuffTableEntry;
import com.dota2.proto.DotaModifiers.DOTA_MODIFIER_ENTRY_TYPE;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public enum StringTableApplier {

    DEFAULT {
        @Override
        public void apply(Match match, String tableName, int index, String key, ByteString value) {
            match.getStringTables().forName(tableName).set(index, key, value);
        }
    },
    MODIFIERS {
        @Override
        public void apply(Match match, String tableName, int index, String key, ByteString value) {
            try {
                CDOTAModifierBuffTableEntry msg = CDOTAModifierBuffTableEntry.parseFrom(value); 
                int entityIndex = msg.getParent() & 0x7FF;
                int modifierIndex = msg.getIndex();
                CDOTAModifierBuffTableEntry prev = match.getModifiers().get(entityIndex, modifierIndex);
                Entity parent = match.getEntities().getByIndex(entityIndex);
                Entity caster = match.getEntities().getByEHandle(msg.getCaster());
                String mName = "NULL";
                if (msg.getEntryType() == DOTA_MODIFIER_ENTRY_TYPE.DOTA_MODIFIER_ENTRY_TYPE_ACTIVE) {
                    match.getModifiers().set(entityIndex, modifierIndex, msg);
                    mName = match.getStringTables().forName("ModifierNames").getNameByIndex(msg.getModifierClass());
                    log.debug("{} {} [entityIdx={}, index={}, class={}, parent={}, caster={}]",
                        match.getReplayTimeAsString(),
                        msg.getEntryType(),
                        entityIndex,
                        modifierIndex,
                        mName,
                        parent == null ? "NOT_FOUND" : parent.getDtClass().getDtName(),
                        caster == null ? "NOT_FOUND" : caster.getDtClass().getDtName()
                    );
                } else if (prev != null) {
                    match.getModifiers().remove(entityIndex, modifierIndex);
                    mName = match.getStringTables().forName("ModifierNames").getNameByIndex(prev.getModifierClass()); 
                    log.debug("{} {} [entityIdx={}, index={}, class={}, parent={}]",
                        match.getReplayTimeAsString(),
                        msg.getEntryType(),
                        entityIndex,
                        modifierIndex,
                        mName,
                        parent == null ? "NOT_FOUND" : parent.getDtClass().getDtName()
                    );
                } else {
                    log.debug("{} DOTA_MODIFIER_ENTRY_TYPE_REMOVED_NOT_EXISTING [entityIdx={}, index={}, class={}]",
                        match.getReplayTimeAsString(),
                        entityIndex,
                        modifierIndex,
                        "NOT_FOUND"
                    );
                }
                log.trace(msg.toString());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException("can't parse modifier update");
            }
        }
    };
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(StringTableApplier.class);
    
    public static StringTableApplier forName(String name) {
        if ("ActiveModifiers".equals(name)) {
            return MODIFIERS;
        } else {
            return DEFAULT;
        }
    }
    
    public abstract void apply(Match match, String tableName, int index, String key, ByteString value);
}
