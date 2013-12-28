package clarity.decoder;

import clarity.match.Match;

import com.dota2.proto.DotaModifiers.CDOTAModifierBuffTableEntry;
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
                match.getModifiers().set(index, msg);
                //System.out.println(String.format("modifier changed at %s\n%s", index, msg));
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException("can't parse modifier update");
            }
        }
    };
    
    public static StringTableApplier forName(String name) {
        if ("ActiveModifiers".equals(name)) {
            return MODIFIERS;
        } else {
            return DEFAULT;
        }
    }
    
    public abstract void apply(Match match, String tableName, int index, String key, ByteString value);
}
