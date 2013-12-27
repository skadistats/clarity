package clarity.model;

import com.dota2.proto.Netmessages.CSVCMsg_GameEventList.descriptor_t;
import com.dota2.proto.Netmessages.CSVCMsg_GameEventList.key_t;

public class GameEventDescriptor {

    private final descriptor_t descriptor;
    
    public GameEventDescriptor(descriptor_t descriptor) {
        this.descriptor = descriptor;
    }
    
    public int getId() {
        return descriptor.getEventid();
    }
    
    public String getName() {
        return descriptor.getName();
    }
    
    public int getKeyCount() {
        return descriptor.getKeysCount();
    }

    public key_t getKeys(int index) {
        return descriptor.getKeys(index);
    }

    
    
    
    
    
}
