package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is made to provide memory efficient vector functionality.
 * It store only values different than defaultValue that can be set by the user.
 * This way, if a vector contains a lot of similar value, this class will stop
 * allocating memory for them.
 *
 * @param <K>
 * @param <V>
 */
public class Vector<K,V> implements Cloneable{

	private HashMap<K, V> vectorData;
	private V defaultValue;
	private Set<K> keysPool=null;
	
	public Vector(HashMap<K, V> vectorData, V defaultValue, Set<K> keysPool) {
		this.vectorData = vectorData;
		this.defaultValue=defaultValue;
		setKeysPool(keysPool);
	}
	public Vector(V defaultValue, Set<K> keysPool) {
		this(new HashMap<>(), defaultValue, keysPool);
	}
	
	public Object clone() {
		@SuppressWarnings("unchecked")
		Vector<K,V> newVector = new Vector<>((HashMap<K, V>)vectorData.clone(),defaultValue, keysPool);
		return newVector;
	}
	/**
	 * returns direct pointer to the internal object. Modify with caution
	 * @return
	 */
	public Set<K> getKeysPool(){
		return this.keysPool;
	}
	/**
	 * alternate name for getKeysPool
	 * @return
	 */
	public Set<K> keySet(){
		return getKeysPool();
	}
	/**
	 * keys pool is a list that contains all possible entries for the vector,
	 * default value is null. If it is not null, getEntryValue will throw exception
	 * in case the requested entry is doesn't exist. If it was null, getEntryValue
	 * will always return defaultValue in case the entry doesn't exist
	 * @param keysPool
	 */
	public void setKeysPool(Set<K> keysPool) {
		this.keysPool=keysPool;
	}
	
	public static Set<Integer> convertKeysPool(ArrayList<String> keysPool) {
		//ArrayList<String> a = new ArrayList<String>(keysPool);
		ArrayList<String> a = keysPool;
		ArrayList<Integer> result = new ArrayList<>();
		for(String str:a) {
			result.add(Integer.parseInt(str));
		}
		return new HashSet<Integer>(result);
	}
	/**
	 * set the value in case it is not equal to the default value
	 * @param key
	 * @param value
	 */
	public void put(K key, V value) {
		if(value.equals(defaultValue)) {
			if(vectorData.containsKey(key))vectorData.remove(key);
			return;
		}
		vectorData.put(key, value);
	}
	public V get(K key) {
		if(keysPool!=null) {
			if(keysPool.contains(key)==false)
				throw new RuntimeException("requested entry '"+key.toString()+"' out of bounds of vector's entries set.");
		}
		if(vectorData.containsKey(key)==false)return defaultValue;
		return vectorData.get(key);
	}
	public void remove(K key) {
		vectorData.remove(key);
	}
}
