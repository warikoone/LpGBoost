/**
 * 
 */
package com.prj.bundle.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeMap;

/**
 * @author iasl
 *
 */
public class OriginalLexemeAttributes {
	
	public LinkedHashMap<String, Integer> triggerProximal;
	public LinkedHashMap<String, Integer> triggerDistant;
	public TreeMap<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> orgLexemeContextFrame;
	public TreeMap<Integer, ArrayList<LinkedList<String>>> orgLexemeVariantFrame;
	public HashMap<LinkedList<String>, Integer> subContextPool;
	public HashMap<LinkedList<String>, Integer> subVariantPool;
	
	public OriginalLexemeAttributes() {

		triggerProximal = new LinkedHashMap<>();
		triggerDistant = new LinkedHashMap<>();
		orgLexemeContextFrame = new TreeMap<>();
		orgLexemeVariantFrame = new TreeMap<>();
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

	public TreeMap<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> getOrgLexemeContextFrame() {
		return orgLexemeContextFrame;
	}

	public void setOrgLexemeContextFrame(
			TreeMap<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> orgLexemeContextFrame) {
		this.orgLexemeContextFrame = orgLexemeContextFrame;
	}

	public HashMap<LinkedList<String>, Integer> getSubContextPool() {
		return subContextPool;
	}

	public void setSubContextPool(HashMap<LinkedList<String>, Integer> subContextPool) {
		this.subContextPool = subContextPool;
	}

	public TreeMap<Integer, ArrayList<LinkedList<String>>> getOrgLexemeVariantFrame() {
		return orgLexemeVariantFrame;
	}

	public void setOrgLexemeVariantFrame(TreeMap<Integer, ArrayList<LinkedList<String>>> orgLexemeVariantFrame) {
		this.orgLexemeVariantFrame = orgLexemeVariantFrame;
	}

	public HashMap<LinkedList<String>, Integer> getSubVariantPool() {
		return subVariantPool;
	}

	public void setSubVariantPool(HashMap<LinkedList<String>, Integer> subVariantPool) {
		this.subVariantPool = subVariantPool;
	}
	
}
