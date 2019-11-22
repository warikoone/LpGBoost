/**
 * 
 */
package com.prj.bundle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ejml.data.Matrix;
import org.ejml.simple.SimpleMatrix;

import com.prj.bundle.metrics.ChiSquareTest;
import com.prj.bundle.metrics.LogLikelihoodTest;
import com.prj.bundle.wrapper.OriginalLexemeAttributes;
import com.prj.bundle.wrapper.PosLexemeAttributes;

/**
 * @author iasl
 *
 */
public class LexicalAppraiser {
	
	private int rowSize;
	private int colSize;

	/**
	 * 
	 */
	public LexicalAppraiser() {
		rowSize = 2;
		colSize = 2;
	}
	
	private LinkedHashMap<Integer, Integer> findComplementStatus(String currentWord,
			LinkedHashMap<String, LinkedHashMap<Integer, Integer>> bufferMap) {
		
		LinkedHashMap<Integer, Integer> tier1BufferMap = new LinkedHashMap<>();
		Iterator<Map.Entry<String, LinkedHashMap<Integer, Integer>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<String, LinkedHashMap<Integer, Integer>> tier1MapValue = tier1Itr.next();
			/**
			 * Row 2 event of confusion matrix
			 */
			if(!tier1MapValue.getKey().equals(currentWord)){
				Iterator<Map.Entry<Integer, Integer>> tier2Itr = 
						tier1MapValue.getValue().entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<Integer, Integer> tier2MapValue = tier2Itr.next();
					int status = 0;
					if(tier1BufferMap.containsKey(tier2MapValue.getKey())){
						status = tier1BufferMap.get(tier2MapValue.getKey());
					}
					status = status+tier2MapValue.getValue();
					tier1BufferMap.put(tier2MapValue.getKey(), status);
				}
				
			}
		}
		
		return(tier1BufferMap);
	}
	
	private LinkedList<Entry<String, Double>> compareMapEntries(
			LinkedList<Entry<String, Double>> linkedList) {
		
		Collections.sort(linkedList, new Comparator<Map.Entry<String, Double>>() {
			// ascending order
			@Override
			public int compare(Entry<String, Double> currItem, Entry<String, Double> nextItem) {
				return (currItem.getValue().compareTo(nextItem.getValue()));
			}
		});
		return(linkedList);
	}

	public LinkedHashMap<String, Double> createOrgLexemeContingencyMatrix(
			HashSet<String> corpusVocab, OriginalLexemeAttributes orgLexInstance) {
		
		LinkedHashMap<String, Double> tier3BufferMap = new LinkedHashMap<>();
		LinkedHashMap<String, LinkedHashMap<Integer, Integer>> tier1BufferMap = new LinkedHashMap<>(); 
		Iterator<String> tier1Itr = corpusVocab.iterator();
		while(tier1Itr.hasNext()){
			String currentWord = tier1Itr.next();
			/**
			 * Screen unsuitable terms for significance appraisal
			 * Row 1 event of confusion matrix
			 */
			LinkedHashMap<Integer, Integer> tier2BufferMap = new LinkedHashMap<>();
			if((!currentWord.matches("PROTEINT[\\d+&&[^0]]")) 
					&& (!currentWord.matches("\\,|\\.|\\;|\\?|\\!|\\:|\\\"|\\'"))){
				// proximal word association
				int proximalFrequency = 0; 
				if(orgLexInstance.triggerProximal.containsKey(currentWord)){
					proximalFrequency = orgLexInstance.triggerProximal.get(currentWord);
				}
				tier2BufferMap.put(1, proximalFrequency);
				// distant word association
				int distantFrequency = 0; 
				if(orgLexInstance.triggerDistant.containsKey(currentWord)){
					distantFrequency = orgLexInstance.triggerDistant.get(currentWord);
				}
				tier2BufferMap.put(0, distantFrequency);
				tier1BufferMap.put(currentWord, tier2BufferMap);
			}
		}
		SimpleMatrix tier1BufferMatrix = new SimpleMatrix(rowSize, colSize);
		Iterator<Map.Entry<String, LinkedHashMap<Integer, Integer>>> tier2Itr = 
				tier1BufferMap.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<String, LinkedHashMap<Integer, Integer>> tier2MapValue = tier2Itr.next();
			LinkedHashMap<Integer, Integer> tier2BufferMap = 
					findComplementStatus(tier2MapValue.getKey(), tier1BufferMap);
			for(int i=0,j=(colSize-1);i<colSize;i++,j--){
				tier1BufferMatrix.setColumn(i, 0, tier2MapValue.getValue().get(j), tier2BufferMap.get(j));
			}
			//System.out.println("\n\t"+tier2MapValue.getKey()+"\t"+tier2MapValue.getValue()+"\t"+tier2BufferMap);
			//System.out.println("\n\t"+tier1BufferMatrix);
			ChiSquareTest chiTestInstance = new ChiSquareTest(tier1BufferMatrix);
			double returnTestStat = chiTestInstance.callChiSquareTestEvaluator();
			if(returnTestStat != (double)-1){
				if(returnTestStat > new Double(10.828)){
					//System.out.println("\n\t"+tier2MapValue.getKey()+"\t"+returnTestStat);
					tier3BufferMap.put(tier2MapValue.getKey(), returnTestStat);
				}
			}else{
				/*
				System.out.println("\n\t"+tier2MapValue.getKey()+"\t"+tier2MapValue.getValue()+"\t"+tier2BufferMap);
				System.out.println("\n\t"+tier1BufferMatrix);
				*/
			}
			/**
			LogLikelihoodTest llrTestInstance = new LogLikelihoodTest(tier1BufferMatrix);
			returnTestStat = llrTestInstance.callLLRTestEvaluator();
			if(returnTestStat != (double)-1){
				//System.out.println("\n\t"+tier2MapValue.getKey()+"\t"+returnTestStat);
				if(returnTestStat > new Double(10.828)){
					tier3BufferMap.put(tier2MapValue.getKey(), returnTestStat);
				}
			}**/
		}
		// default value
		tier3BufferMap.put("@", new Double(1));
		tier3BufferMap.put("NA#", new Double(10.827));
		
		List<Map.Entry<String, Double>> tier1ResultListMap = 
				compareMapEntries(new LinkedList<>(tier3BufferMap.entrySet()));
		Double maxValue = tier1ResultListMap.get(tier1ResultListMap.size()-1).getValue();
		tier3BufferMap.clear();
		Iterator<Map.Entry<String, Double>> tier3Itr = tier1ResultListMap.iterator();
		Double rankIndex = new Double(1);
		while(tier3Itr.hasNext()){
			Map.Entry<String, Double> tier3MapValue = tier3Itr.next();
			Double revisedAppraise = ((tier3MapValue.getValue()/maxValue));
			revisedAppraise = rankIndex+1;
			//System.out.println("\n\t>>"+tier3MapValue.getKey()+"\t"+revisedAppraise);
			tier3BufferMap.put(tier3MapValue.getKey(), revisedAppraise);
			rankIndex++;
		}
		//System.out.println("\n\t>>"+tier3BufferMap.size());
		//System.exit(0);
		
		return(tier3BufferMap);
		
	}

	public LinkedHashMap<String, Double> createPosLexemeContingencyMatrix(
			HashSet<String> corpusVocab, PosLexemeAttributes posLexInstance) {

		LinkedHashMap<String, Double> tier3BufferMap = new LinkedHashMap<>();
		LinkedHashMap<String, LinkedHashMap<Integer, Integer>> tier1BufferMap = new LinkedHashMap<>(); 
		Iterator<String> tier1Itr = corpusVocab.iterator();
		while(tier1Itr.hasNext()){
			String currentWord = tier1Itr.next();
			/**
			 * Screen unsuitable terms for significance appraisal
			 */
			LinkedHashMap<Integer, Integer> tier2BufferMap = new LinkedHashMap<>();
			if((!currentWord.matches("PROTEINT[\\d+&&[^0]]")) 
					&& (!currentWord.matches("\\,|\\.|\\;|\\?|\\!|\\:|\\\"|\\'"))){
				// proximal word association
				int proximalFrequency = 0; 
				if(posLexInstance.triggerProximal.containsKey(currentWord)){
					proximalFrequency = posLexInstance.triggerProximal.get(currentWord);
				}
				tier2BufferMap.put(1, proximalFrequency);
				// distant word association
				int distantFrequency = 0; 
				if(posLexInstance.triggerDistant.containsKey(currentWord)){
					distantFrequency = posLexInstance.triggerDistant.get(currentWord);
				}
				tier2BufferMap.put(0, distantFrequency);
				tier1BufferMap.put(currentWord, tier2BufferMap);
			}
		}
		SimpleMatrix tier1BufferMatrix = new SimpleMatrix(rowSize, colSize);
		Iterator<Map.Entry<String, LinkedHashMap<Integer, Integer>>> tier2Itr = 
				tier1BufferMap.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<String, LinkedHashMap<Integer, Integer>> tier2MapValue = tier2Itr.next();
			LinkedHashMap<Integer, Integer> tier2BufferMap = 
					findComplementStatus(tier2MapValue.getKey(), tier1BufferMap);
			for(int i=0,j=(colSize-1);i<colSize;i++,j--){
				tier1BufferMatrix.setColumn(i, 0, tier2MapValue.getValue().get(j), tier2BufferMap.get(j));
			}
			//System.out.println("\n\t"+tier2MapValue.getKey()+"\t"+tier2MapValue.getValue()+"\t"+tier2BufferMap);
			//System.out.println("\n\t"+tier1BufferMatrix);
			ChiSquareTest chiTestInstance = new ChiSquareTest(tier1BufferMatrix);
			double returnTestStat = chiTestInstance.callChiSquareTestEvaluator();
			if(returnTestStat != (double)-1){
				if(returnTestStat > new Double(10.828)){
					//System.out.println("\n\t"+tier2MapValue.getKey()+"\t"+returnTestStat);
					tier3BufferMap.put(tier2MapValue.getKey(), returnTestStat);
				}
			}
			/**
			LogLikelihoodTest llrTestInstance = new LogLikelihoodTest(tier1BufferMatrix);
			returnTestStat = llrTestInstance.callLLRTestEvaluator();
			if(returnTestStat != (double)-1){
				if(returnTestStat > new Double(10.828)){
					//System.out.println("\n\t"+tier2MapValue.getKey()+"\t"+returnTestStat);
					tier3BufferMap.put(tier2MapValue.getKey(), returnTestStat);
				}
			}
			**/
		}
		// default value
		tier3BufferMap.put("@", new Double(1));
		tier3BufferMap.put("NA#", new Double(10.827));
		
		List<Map.Entry<String, Double>> tier1ResultListMap = 
				compareMapEntries(new LinkedList<>(tier3BufferMap.entrySet()));
		Double maxValue = tier1ResultListMap.get(tier1ResultListMap.size()-1).getValue();
		tier3BufferMap.clear();
		Iterator<Map.Entry<String, Double>> tier3Itr = tier1ResultListMap.iterator();
		Double rankIndex = new Double(1);
		while(tier3Itr.hasNext()){
			Map.Entry<String, Double> tier3MapValue = tier3Itr.next();
			Double revisedAppraise = ((tier3MapValue.getValue()/maxValue)+0.5);
			revisedAppraise = rankIndex+1;
			//System.out.println("\n\t>>"+tier3MapValue.getKey()+"\t"+revisedAppraise);
			tier3BufferMap.put(tier3MapValue.getKey(), revisedAppraise);
			rankIndex++;
		}
		//System.out.println("\n\t>>"+tier3BufferMap.size());
		//System.exit(0);
		return(tier3BufferMap);
		
	}

}
