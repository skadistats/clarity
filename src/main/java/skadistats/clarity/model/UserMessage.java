package skadistats.clarity.model;

import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;

import skadistats.clarity.match.Match;


public class UserMessage {

    private final Descriptor descriptor;
    private final Object[] state;

    public static UserMessage build(com.google.protobuf.GeneratedMessage message, Match match){
    	Descriptor desc = match.getDescriptors().forName(message.getDescriptorForType().getName());
    	if(desc == null){
    		String[] keys = new String[message.getDescriptorForType().getFields().size()];
    		for (FieldDescriptor d :message.getDescriptorForType().getFields()) {
    			keys[d.getIndex()] = d.getName();
            }
    		desc = new Descriptor(message.getDescriptorForType().getName(), keys);
    		match.getDescriptors().add(desc);
    	}
    	UserMessage result = new UserMessage(desc);
    	for (FieldDescriptor d :message.getDescriptorForType().getFields()) {
    		if(d.isRepeated()){
    			Object[] field = new Object[message.getRepeatedFieldCount(d)];
    			for(int i = 0; i< message.getRepeatedFieldCount(d); ++i){
    				if(d.getType().name().equals("MESSAGE")){
    					field[i] = build((GeneratedMessage) message.getRepeatedField(d, i), match);
    				}
    				else{
    					field[i] = message.getRepeatedField(d, i);
    				}
    			}
    			result.set(d.getIndex(), field);
    		}
    		else if(d.getType().name().equals("MESSAGE")){
				result.set(d.getIndex(), build((GeneratedMessage) message.getField(d), match));
			}
    		else if(d.getType().name().equals("ENUM")){
				result.set(d.getIndex(),((EnumValueDescriptor)message.getField(d)).getName());
			}
			else{
				result.set(d.getIndex(), message.getField(d));
			}
        }
    	return result;
    }
    
    public UserMessage(Descriptor descriptor) {
        this.descriptor = descriptor;
        this.state = new Object[descriptor.getKeys().length];
    }
    
    public void set(int index, Object value) {
        this.state[index] = value;
    }
    
    @SuppressWarnings("unchecked")
	public <T> T getProperty(int index) {
        return (T) state[index];
    }

    @SuppressWarnings("unchecked")
	public <T> T getProperty(String key) {
        Integer index = descriptor.getIndexForKey(key);
        if (index == null) {
            throw new IllegalArgumentException("key not found for this GameEvent");
        }
        return (T) state[index.intValue()];
    }

    public String getName() {
        return this.descriptor.getName();
    }
	
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < state.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(descriptor.getKeys()[i]);
            buf.append("=");
            buf.append(state[i]);
        }
        return String.format("UserMessage [name=%s, %s]", descriptor.getName(), buf.toString());
    }
    
}
