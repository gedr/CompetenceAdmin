package minos.resource.managers;

import java.util.HashMap;
import java.util.Map;

public class Resources {
	private Map<Integer, Object> map;
	
	private Resources() { 
		map = new HashMap<>();
	}
	
	private static class Holder {
		private static final Resources INSTANCE = new Resources();
	}
	
	public static Resources getInstance() {
		return Holder.INSTANCE;
	}
	
	public Object get(Integer id) {
		return map.get(id);
	}
	
	public void put(Integer id, Object res) {
		map.put(id, res);
	}
	
	public boolean containsResource(Integer id) {
		return map.containsKey(id);
	}
	
	public void clear() {
		map.clear();
	}
}
