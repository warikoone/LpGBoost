/**
 * 
 */
package com.prj.bundle.preprocessing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeMap;

/**
 * @author neha
 *
 */
public class PopulateResources {

	protected LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>> proteinEntities;
	protected LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>> abstractCollection;
	protected LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>> relationCollection;
	
	
	public LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> getProteinEntities() {
		return proteinEntities;
	}
	public void setProteinEntities(LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> proteinEntities) {
		this.proteinEntities = proteinEntities;
	}
	public LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> getAbstractCollection() {
		return abstractCollection;
	}
	public void setAbstractCollection(LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> abstractCollection) {
		this.abstractCollection = abstractCollection;
	}
	public LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> getRelationCollection() {
		return relationCollection;
	}
	public void setRelationCollection(LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> relationCollection) {
		this.relationCollection = relationCollection;
	}
	
	
}
