/**
 * 
 */
package com.prj.bundle.wrapper;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * @author iasl
 *
 */
public class DifferentialContextAttributes {

	private LinkedList<String> referenceFeature;
	private LinkedHashMap<String, Double> differentialAttributes;
	/**
	 * 
	 */
	public DifferentialContextAttributes() {
		
	}
	
	public DifferentialContextAttributes(
			Entry<LinkedList<String>, LinkedHashMap<String, Double>> bufferMapValue) {
		referenceFeature = new LinkedList<>(bufferMapValue.getKey());
		differentialAttributes = new LinkedHashMap<>(bufferMapValue.getValue());
		
	}

	public LinkedList<String> getReferenceFeature() {
		return referenceFeature;
	}

	public void setReferenceFeature(LinkedList<String> referenceFeature) {
		this.referenceFeature = referenceFeature;
	}

	public LinkedHashMap<String, Double> getDifferentialAttributes() {
		return differentialAttributes;
	}

	public void setDifferentialAttributes(LinkedHashMap<String, Double> differentialAttributes) {
		this.differentialAttributes = differentialAttributes;
	}
	
}
