package clarity.model;

import java.util.Map;

public class Entity {

	private final Integer index;
	private final Integer serial;
	private final Integer cls;
	private final Map<Integer, Object> state;
	
	public Entity(Integer index, Integer serial, Integer cls, Map<Integer, Object> state) {
		this.index = index;
		this.serial = serial;
		this.cls = cls;
		this.state = state;
	}
	
	public void updateFrom(Entity from) {
		for (Map.Entry<Integer, Object> e : from.state.entrySet()) {
			state.put(e.getKey(), e.getValue());
		}
	}

	public Integer getIndex() {
		return index;
	}

	public Integer getSerial() {
		return serial;
	}

	public Integer getCls() {
		return cls;
	}
	
}
