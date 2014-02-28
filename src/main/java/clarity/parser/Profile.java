package clarity.parser;

import java.util.HashSet;
import java.util.Set;

import com.dota2.proto.Demo;
import com.dota2.proto.DotaUsermessages;
import com.dota2.proto.Netmessages;
import com.dota2.proto.Networkbasetypes;

public class Profile {

    public static Profile NET_TICK = new Profile()
        .append(
            Netmessages.CNETMsg_Tick.class
        );

    public static Profile REPLAY_TIME = new Profile()
        .append(
            Netmessages.CSVCMsg_ServerInfo.class
        );
    
    public static Profile STRING_TABLES = new Profile()
        .append(
            Netmessages.CSVCMsg_CreateStringTable.class,
            Netmessages.CSVCMsg_UpdateStringTable.class
        );

    public static Profile SEND_TABLES = new Profile()
        .append(
            Demo.CDemoClassInfo.class,
            Demo.CDemoSyncTick.class,
            Netmessages.CSVCMsg_SendTable.class
        );
    
    public static Profile VOICE_DATA = new Profile()
        .append(
            Netmessages.CSVCMsg_VoiceInit.class,
            Netmessages.CSVCMsg_VoiceData.class
        );

    public static Profile BASELINES = new Profile()
        .dependsOn(STRING_TABLES);

    public static Profile TRANSIENT_DATA = new Profile()
        .dependsOn(NET_TICK);
    
    public static Profile USERMESSAGE_CONTAINER = new Profile()
        .append(
            Networkbasetypes.CSVCMsg_UserMessage.class
        );

    public static Profile ENTITIES = new Profile()
        .dependsOn(SEND_TABLES)
        .dependsOn(BASELINES)
        .append(
            Netmessages.CSVCMsg_PacketEntities.class
        );

    public static Profile TEMP_ENTITIES = new Profile()
        .dependsOn(TRANSIENT_DATA)
        .dependsOn(SEND_TABLES)
        .append(
            Netmessages.CSVCMsg_TempEntities.class
        );

    public static Profile MODIFIERS = new Profile()
        .dependsOn(ENTITIES)
        .dependsOn(STRING_TABLES);
    
    public static Profile GAME_EVENTS = new Profile()
        .append(
            Netmessages.CSVCMsg_GameEventList.class,
            Networkbasetypes.CSVCMsg_GameEvent.class
        );
    
    public static Profile PROJECTILES = new Profile()
        .dependsOn(ENTITIES)
        .dependsOn(USERMESSAGE_CONTAINER)
        .append(
            DotaUsermessages.CDOTAUserMsg_CreateLinearProjectile.class,
            DotaUsermessages.CDOTAUserMsg_DestroyLinearProjectile.class,
            DotaUsermessages.CDOTAUserMsg_DodgeTrackingProjectiles.class
        );
    
    public static Profile CHAT_MESSAGES = new Profile()
    .dependsOn(TRANSIENT_DATA)
    .dependsOn(USERMESSAGE_CONTAINER)
    .append(
        DotaUsermessages.CDOTAUserMsg_ChatEvent.class
    );
    
    public static Profile ALL = new Profile() {
        @Override
        public boolean contains(Class<?> clazz) {
            return true;
        }
    };
    
    private final Set<Class<?>> protoClasses = new HashSet<Class<?>>();

    public Profile append(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            protoClasses.add(clazz);
        }
        return this;
    }

    public Profile dependsOn(Profile other) {
        for (Class<?> clazz : other.protoClasses) {
            protoClasses.add(clazz);
        }
        return this;
    }

    public boolean contains(Class<?> clazz) {
        return protoClasses.contains(clazz);
    }

}
