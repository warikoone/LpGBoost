/**
 * 
 */
package com.prj.bundle.utility;

import java.util.LinkedHashMap;

/**
 * @author iasl
 *
 */
public class DictionaryUtility {

	public DictionaryUtility() {
		
	}
	
	public static LinkedHashMap<Object, Object> updateDictionaryVolume(
			LinkedHashMap<Object, Object> bufferMap, Object keyValue){
		
		Object valueSize = 0;
		if(bufferMap.containsKey(keyValue)){
			valueSize = bufferMap.get(keyValue);
		}
		valueSize = (int)valueSize + 1;
		bufferMap.put(keyValue, valueSize);
		
		return(bufferMap);
	}

}
