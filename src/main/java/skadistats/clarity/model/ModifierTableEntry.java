package skadistats.clarity.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.dota2.proto.DotaModifiers.CDOTAModifierBuffTableEntry;

public class ModifierTableEntry {
	private Map<Integer, Object> data;
	
	  public static final Map<String, Integer> fields;
	    static {
	        Map<String, Integer> new_fields = new HashMap<String, Integer>();
	        new_fields.put("entry_type", 1);
	        new_fields.put("parent", 2);
	        new_fields.put("index", 3);
	        new_fields.put("serial_num", 4);
	        new_fields.put("modifier_class", 5);
	        new_fields.put("ability_level", 6);
	        new_fields.put("stack_count", 7);
	        new_fields.put("creation_time", 8);
	        new_fields.put("duration", 9);
	        new_fields.put("caster", 10);
	        new_fields.put("ability", 11);
	        new_fields.put("armor", 12);
	        new_fields.put("fade_time", 13);
	        new_fields.put("channel_time", 14);
	        new_fields.put("v_start", 15);
	        new_fields.put("v_end", 16);
	        new_fields.put("portal_loop_appear", 17);
	        new_fields.put("portal_loop_disappear", 18);
	        new_fields.put("hero_loop_appear", 19);
	        new_fields.put("hero_loop_disappear", 20);
	        new_fields.put("movement_speed", 21);
	        new_fields.put("aura", 22);
	        new_fields.put("activity", 23);
	        new_fields.put("damage", 24);
	        new_fields.put("range", 25);
	        new_fields.put("dd_modifier_index", 26);
	        new_fields.put("dd_ability_index", 27);
	        fields = Collections.unmodifiableMap(new_fields);
	    }
	    
	    public static final Map<Integer, String> fieldNames;
	    static {
	        Map<Integer, String> new_fieldNames = new HashMap<Integer, String>();
	        for(Map.Entry<String, Integer> field : fields.entrySet())
	        	new_fieldNames.put(field.getValue(), field.getKey());
	        fieldNames = Collections.unmodifiableMap(new_fieldNames);
	    }
	
	public ModifierTableEntry(CDOTAModifierBuffTableEntry buffTableEntry){
		
		data = new HashMap<Integer, Object>();
		data.put(fields.get("entry_type"), buffTableEntry.getEntryType().getValueDescriptor().getName());
		data.put(fields.get("parent"), buffTableEntry.getParent());
		data.put(fields.get("index"), buffTableEntry.getIndex());
		data.put(fields.get("serial_num"), buffTableEntry.getSerialNum());
		
		if(buffTableEntry.hasModifierClass())
			data.put(fields.get("modifier_class"), buffTableEntry.getModifierClass());

		if(buffTableEntry.hasAbilityLevel())
			data.put(fields.get("ability_level"), buffTableEntry.getAbilityLevel());
		
		if(buffTableEntry.hasStackCount())
			data.put(fields.get("stack_count"), buffTableEntry.getStackCount());
		
		if(buffTableEntry.hasCreationTime())
			data.put(fields.get("creation_time"), buffTableEntry.getCreationTime());
		
		if(buffTableEntry.hasDuration())
			data.put(fields.get("duration"), buffTableEntry.getDuration());
		
		if(buffTableEntry.hasCaster())
			data.put(fields.get("caster"), buffTableEntry.getCaster());
		
		if(buffTableEntry.hasAbility())
			data.put(fields.get("ability"), buffTableEntry.getAbility());
		
		if(buffTableEntry.hasArmor())
			data.put(fields.get("armor"), buffTableEntry.getArmor());
		
		if(buffTableEntry.hasFadeTime())
			data.put(fields.get("fade_time"), buffTableEntry.getFadeTime());
		
		if(buffTableEntry.hasChannelTime())
			data.put(fields.get("channel_time"), buffTableEntry.getChannelTime());
		
		if(buffTableEntry.hasVStart()){
			if(buffTableEntry.getVStart().hasZ()){
				Vector3f vec = new Vector3f();
				vec.x = buffTableEntry.getVStart().getX();
				vec.y = buffTableEntry.getVStart().getY();
				vec.z = buffTableEntry.getVStart().getZ();
				data.put(fields.get("v_start"), vec);
			}
			else{
				if(!buffTableEntry.getVStart().hasX() || !buffTableEntry.getVStart().hasY())
					System.out.println("Buff field vector is missing components?");
				
				Vector2f vec = new Vector2f();
				vec.x = buffTableEntry.getVStart().getX();
				vec.y = buffTableEntry.getVStart().getY();
				data.put(fields.get("v_start"), vec);			
			}
		}
		
		if(buffTableEntry.hasVEnd()){
			if(buffTableEntry.getVStart().hasZ()){
				Vector3f vec = new Vector3f();
				vec.x = buffTableEntry.getVStart().getX();
				vec.y = buffTableEntry.getVStart().getY();
				vec.z = buffTableEntry.getVStart().getZ();
				data.put(fields.get("v_end"), vec);
			}
			else{
				if(!buffTableEntry.getVStart().hasX() || !buffTableEntry.getVStart().hasY())
					System.out.println("Buff field vector is missing components?");
				
				Vector2f vec = new Vector2f();
				vec.x = buffTableEntry.getVStart().getX();
				vec.y = buffTableEntry.getVStart().getY();
				data.put(fields.get("v_end"), vec);			
			}

		}

		if(buffTableEntry.hasPortalLoopAppear())
			data.put(fields.get("portal_loop_appear"), buffTableEntry.getPortalLoopAppear());
		
		if(buffTableEntry.hasPortalLoopDisappear())
			data.put(fields.get("portal_loop_disappear"), buffTableEntry.getPortalLoopDisappear());
		
		if(buffTableEntry.hasHeroLoopAppear())
			data.put(fields.get("hero_loop_appear"), buffTableEntry.getHeroLoopAppear());
		
		if(buffTableEntry.hasHeroLoopDisappear())
			data.put(fields.get("hero_loop_disappear"), buffTableEntry.getHeroLoopDisappear());
		
		if(buffTableEntry.hasMovementSpeed())
			data.put(fields.get("movement_speed"), buffTableEntry.getMovementSpeed());
		
		if(buffTableEntry.hasAura())
			data.put(fields.get("aura"), buffTableEntry.getAura());
		
		if(buffTableEntry.hasActivity())
			data.put(fields.get("activity"), buffTableEntry.getActivity());
		
		if(buffTableEntry.hasDamage())
			data.put(fields.get("damage"), buffTableEntry.getDamage());
		
		if(buffTableEntry.hasRange())
			data.put(fields.get("range"), buffTableEntry.getRange());
		
		if(buffTableEntry.hasDdModifierIndex())
			data.put(fields.get("dd_modifier_index"), buffTableEntry.getDdModifierIndex());
		
		if(buffTableEntry.hasDdAbilityIndex())
			data.put(fields.get("dd_ability_index"), buffTableEntry.getDdAbilityIndex());
	}
	
	public Object getField(String field){
		return data.get(fields.get(field));
	}
	
	public boolean hasField(String field){
		return data.containsKey(fields.get(field));
	}
	
	public String toString(){
		String result ="Modifier: [";
		Iterator<Map.Entry<Integer, Object>> it = data.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, Object> field = it.next();
			result += fieldNames.get(field.getKey())+": "+field.getValue().toString();
			if(it.hasNext())
				result += ", ";
		}
		result += "]";
		return result;
	}
}
