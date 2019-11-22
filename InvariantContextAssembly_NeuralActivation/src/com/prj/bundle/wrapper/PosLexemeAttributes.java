/**
 * 
 */
package com.prj.bundle.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeMap;

/**
 * @author iasl
 *
 */
public class PosLexemeAttributes {

	public LinkedHashMap<String, Integer> triggerProximal;
	public LinkedHashMap<String, Integer> triggerDistant;
	public TreeMap<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> posContextFrame;
	public TreeMap<Integer, ArrayList<LinkedList<String>>> posVariantFrame;
	public HashMap<LinkedList<String>, Integer> subContextPool;
	public HashMap<LinkedList<String>, Integer> subVariantPool;
	
	/**
	 * 
	 */
	public PosLexemeAttributes() {

		triggerProximal = new LinkedHashMap<>();
		triggerDistant = new LinkedHashMap<>();
		posContextFrame = new TreeMap<>();
		posVariantFrame = new TreeMap<>();
		subContextPool = new HashMap<>();
		subVariantPool = new HashMap<>();
	}

	public LinkedHashMap<String, Integer> getTriggerProximal() {
		return triggerProximal;
	}

	public void setTriggerProximal(LinkedHashMap<String, Integer> triggerProximal) {
		this.triggerProximal = triggerProximal;
	}

	public LinkedHashMap<String, Integer> getTriggerDistant() {
		return triggerDistant;
	}

	public void setTriggerDistant(LinkedHashMap<String, Integer> triggerDistant) {
		this.triggerDistant = triggerDistant;
	}

	public TreeMap<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> getPosContextFrame() {
		return posContextFrame;
	}

	public void setPosContextFrame(TreeMap<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> posContextFrame) {
		this.posContextFrame = posContextFrame;
	}

	public HashMap<LinkedList<String>, Integer> getSubContextPool() {
		return subContextPool;
	}

	public void setSubContextPool(HashMap<LinkedList<String>, Integer> subContextPool) {
		this.subContextPool = subContextPool;
	}

	public TreeMap<Integer, ArrayList<LinkedList<String>>> getPosVariantFrame() {
		return posVariantFrame;
	}

	public void setPosVariantFrame(TreeMap<Integer, ArrayList<LinkedList<String>>> posVariantFrame) {
		this.posVariantFrame = posVariantFrame;
	}

	public HashMap<LinkedList<String>, Integer> getSubVariantPool() {
		return subVariantPool;
	}

	public void setSubVariantPool(HashMap<LinkedList<String>, Integer> subVariantPool) {
		this.subVariantPool = subVariantPool;
	}
	
}
