/**
 * 
 */
package com.prj.bundle.representation;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.print.DocFlavor.STRING;
import javax.sound.midi.Soundbank;

import org.omg.PortableInterceptor.INACTIVE;

import com.jmcejuela.bio.jenia.JeniaTagger;
import com.jmcejuela.bio.jenia.common.Sentence;
import com.jmcejuela.bio.jenia.common.Token;
import com.prj.bundle.model.FrameFractioner;
import com.prj.bundle.model.GeometricProjection;
import com.prj.bundle.utility.RegexUtility;


/**
 * @author iasl
 *
 */
public class InvariantContextRepresentation{

	private TreeMap<Integer, LinkedHashMap<Integer, 
	HashMap<LinkedList<String>, LinkedList<Double>>>> invariantClusters;
	private Integer windowDimension;
	private String normTriggerTerm;
	private String regularTerm;
	private TreeMap<Integer, Double> baselineWeights;
	private TreeMap<Integer, Double> finalWeights;
	private TreeMap<Integer, TreeMap<Integer, Double>> learnedWeights;
	private Properties systemProperties;
	private TreeMap<Integer, LinkedHashMap<String, Integer>> errorBufferMap;
	private TreeMap<Integer, ArrayList<LinkedList<String>>> errorBufferLList;
	
	/**
	 * @param bufferMap 
	 * @param iconFeatureSequenceMap2 
	 * @param seedFrame 
	 * @param regularTriggerTerm 
	 * @param tier0LearnedWeights 
	 * @param tier2LearnedWeights 
	 * @param tier1LearnedWeights 
	 * @param normTriggerTerm 
	 * @throws IOException 
	 * 
	 */
	public InvariantContextRepresentation(
			TreeMap<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> 
			bufferMap, 
			Integer seedFrame, String triggerTerm, 
			String regularTriggerTerm, TreeMap<Integer, TreeMap<Integer, Double>> tier3LearnedWeights, 
			TreeMap<Integer, Double> tier2LearnedWeights, 
			TreeMap<Integer, Double> tier1LearnedWeights) throws IOException {
		
		invariantClusters = bufferMap;
		windowDimension = seedFrame;
		normTriggerTerm = triggerTerm;
		regularTerm = regularTriggerTerm;
		learnedWeights = tier3LearnedWeights;
		finalWeights = tier2LearnedWeights;
		baselineWeights = tier1LearnedWeights;
		this.systemProperties = new Properties();
        InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
        errorBufferMap = new TreeMap<>();
        errorBufferLList = new TreeMap<>();
	}
	
	private Double layeredAlignment(LinkedList<String> patternLList, 
			LinkedList<String> compareLList, Integer triggerIndex) {
		
		//System.out.println("\n\t"+patternLList+"\t"+compareLList);
		Double matchStatus = new Double(-1);
		int maxWindow = windowDimension, minWindow = 0;
		maxWindow = (int) Math.ceil(0.6 * windowDimension.doubleValue());
		if(maxWindow == windowDimension){
			maxWindow = windowDimension-1;
		}
		minWindow = (windowDimension-maxWindow);
		double thresholdScore = (maxWindow *(Math.log(Math.pow(10, 1)))
				+ (minWindow * (Math.log(Math.pow(10, -2)))));
		double contextMatchScore = 0;
		double matchScore = 0;
		for(int i=0;i<patternLList.size();i++){
			double indexMatch = Math.log(Math.pow(10, 1));
			int diffIndex = (triggerIndex-i);
			Double indexWeight = new Double(0).doubleValue();
			if(diffIndex > 0){
				diffIndex = (-1*diffIndex);
			}
			indexWeight = Math.pow(2, diffIndex);
			if(i == triggerIndex){
				if(!patternLList.get(i).equalsIgnoreCase(compareLList.get(i))){
					System.err.println("\n layeredAlignment() ~ InvariantContextRepresentation "
							+ "- Critical mismatch error "+compareLList+"\t"+patternLList);
					System.exit(0);
				}
			}
			if(!patternLList.get(i).contentEquals(compareLList.get(i))){
				if(("@".contentEquals(compareLList.get(i)))){
					indexMatch = Math.log(Math.pow(10, -2));
					//System.out.println("@>>"+indexMatch);
				}else if(("#".contentEquals(patternLList.get(i))) 
						&& (!"@".contentEquals(compareLList.get(i)))){
					indexMatch = Math.log(Math.pow(10, -1));
					//System.out.println("#>>"+indexMatch);
				}else{
					indexMatch = Math.log(Math.pow(10, -2));
					//System.out.println("POS>>"+indexMatch);
				}
			}
			//System.out.println("\t"+i+"::\t"+indexMatch+"\t"+indexWeight);
			matchScore =(matchScore + indexMatch);
			/**
			if(Double.compare(indexMatch, Math.log(Math.pow(10, 1))) < 0){
				indexMatch = indexWeight*indexMatch;
			}**/
			contextMatchScore = (contextMatchScore+indexMatch);
		}
		
		//System.out.println(">>"+contextMatchScore+"\t>>"+thresholdScore);
		int retVal = Double.compare(matchScore, thresholdScore);
		if((retVal > 0 ) || (retVal == 0)){
			matchStatus = contextMatchScore;
		}
		
		/**
		if(matchStatus.intValue() != -1){
			System.out.println("\n\t"+patternLList+"\t"+compareLList+"\t"+maxWindow);
			System.out.println("\t"+thresholdScore+"\t"+contextMatchScore+"\t"+matchStatus);
		}**/
		
		return(matchStatus);
	}
	
	private Double layeredAlignment(LinkedList<String> patternLList, 
			LinkedList<String> compareLList) {

		//System.out.println("\n\t"+patternLList+"\t"+compareLList);
		Double matchStatus = new Double(-1);
		int maxWindow = windowDimension, minWindow = 0, triggerIndex=0;
		maxWindow = (int) Math.ceil(0.6 * windowDimension.doubleValue());
		if(maxWindow == windowDimension){
			maxWindow = windowDimension-1;
		}
		minWindow = (windowDimension-maxWindow);
		Double thresholdScore = new Double(0).doubleValue();
		thresholdScore = (maxWindow *(Math.log(Math.pow(10, 1)))
				+ (minWindow * (Math.log(Math.pow(10, -1)))));
		Double contextMatchScore = new Double(0).doubleValue();
		double matchScore = 0;
		for(int i=0;i<patternLList.size();i++){
			double indexMatch = Math.log(Math.pow(10, 1));
			int diffIndex = (triggerIndex-i);
			Double indexWeight = new Double(0).doubleValue();
			if(diffIndex > 0){
				diffIndex = (-1*diffIndex);
			}
			indexWeight = Math.pow(2, diffIndex);
			if(!patternLList.get(i).contentEquals(compareLList.get(i))){
				if(("#".contentEquals(patternLList.get(i)))){
					indexMatch = Math.log(Math.pow(10, -2));
					//System.out.println("#>>"+indexMatch);
				}else{
					indexMatch = Math.log(Math.pow(10, -1));
					//System.out.println("POS>>"+indexMatch);
				}
			}
			//System.out.println("\t"+i+"::\t"+indexMatch+"\t"+indexWeight);
			matchScore =(matchScore + indexMatch);
			/**
			if(Double.compare(indexMatch, Math.log(Math.pow(10, 1))) < 0){
				indexMatch = indexWeight*indexMatch;
			}**/
			contextMatchScore = (contextMatchScore+indexMatch);
		}
		
		//System.out.println(">>"+contextMatchScore+"\t>>"+thresholdScore);
		int retVal = Double.compare(matchScore, thresholdScore);
		if((retVal > 0 ) || (retVal == 0)){
			matchStatus = contextMatchScore;
		}
		
		/**
		if(matchStatus.intValue() != -1){
			System.out.println("\n\t"+patternLList+"\t"+compareLList+"\t"+maxWindow);
			System.out.println("\t"+thresholdScore+"\t"+contextMatchScore+"\t"+matchStatus);
		}**/
				
		return(matchStatus);
	}
	
	private TreeMap<String, LinkedList<Double>> getContextBasedInvariantRepresentation(
			LinkedList<String> bufferLList, Integer triggerIndex, 
			TreeMap<String, LinkedList<Double>> tier1ResultBufferMap) {
		

		Iterator<Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> tier1Itr = 
				invariantClusters.get(triggerIndex).entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>> tier1MapValue = 
					tier1Itr.next();
			Iterator<Map.Entry<LinkedList<String>, LinkedList<Double>>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			Integer index = 1;
			while(tier2Itr.hasNext()){
				String featureId = triggerIndex.toString()
						+tier1MapValue.getKey().toString()+index.toString();
				LinkedList<Double> tier1BufferLList = new LinkedList<>(Collections.nCopies(windowDimension, 0.0));
				if(tier1ResultBufferMap.containsKey(featureId)){
					tier1BufferLList = tier1ResultBufferMap.get(featureId);
				}
				Map.Entry<LinkedList<String>, LinkedList<Double>> tier2MapValue = tier2Itr.next();
				Double matchScore = layeredAlignment(tier2MapValue.getKey(), bufferLList, triggerIndex); 
				if(-1 != matchScore.intValue()){
					for(int i=0;i<tier1BufferLList.size();i++){
						tier1BufferLList.set(i,
								(tier1BufferLList.get(i)+tier2MapValue.getValue().get(i)));
					}
					//System.out.println("\t"+tier1BufferLList);
					//System.exit(0);
					/**
					tier1BufferArray.add(tier2MapValue.getKey().stream()
							.reduce((curr, prev)->curr+" "+prev).get());**/
				}
				tier1ResultBufferMap.put(featureId, tier1BufferLList);
				index++;
			}
		}
		//System.out.println("\t"+triggerIndex+"\t"+tier1BufferMap);
		System.out.println("\t"+tier1ResultBufferMap.size());
		return(tier1ResultBufferMap);
		
	}	
	
	private TreeMap<String, LinkedList<Double>> getSequenceBasedInvariantRepresentation(
			LinkedList<String> bufferLList, Integer triggerIndex, 
			int contextSequenceIndex, int termFactor) {

		TreeMap<String, LinkedList<Double>> tier1ResultBufferMap = new TreeMap<>();
		LinkedHashMap<Integer, ArrayList<Double>> tier1BufferMap = new LinkedHashMap<>();
		Iterator<Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> tier1Itr = 
				invariantClusters.get(triggerIndex).entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>> tier1MapValue = 
					tier1Itr.next();
			Iterator<Map.Entry<LinkedList<String>, LinkedList<Double>>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<LinkedList<String>, LinkedList<Double>> tier2MapValue = tier2Itr.next();
				/**
				 * If terms match, then get the invariant score
				 */
				Double matchScore = layeredAlignment(tier2MapValue.getKey(), bufferLList, triggerIndex); 
				if(-1 != matchScore.intValue()){
					for(int i=0;i<tier2MapValue.getValue().size();i++){
						ArrayList<Double> tier1BufferArray = new ArrayList<>();
						if(tier1BufferMap.containsKey(i)){
							tier1BufferArray = tier1BufferMap.get(i);
						}
						tier1BufferArray.add(tier2MapValue.getValue().get(i));
						tier1BufferMap.put(i, tier1BufferArray);
					}
				}
			}
		}
		//System.out.println("\t\t>>"+tier1BufferMap);
		LinkedList<Double> tier1BufferLList = new LinkedList<>();
		Iterator<ArrayList<Double>> tier3Itr = tier1BufferMap.values().iterator();
		while(tier3Itr.hasNext()){
			ArrayList<Double> tier2BufferArray = new ArrayList<>(tier3Itr.next());
			Double avgScore = (tier2BufferArray.stream()
					.reduce((prev,curr)->prev+curr)).get()
					/new Double(tier2BufferArray.size());
			tier1BufferLList.add(avgScore);
		}
		//System.out.println("\t"+tier1BufferLList);
		if(tier1BufferLList.isEmpty()){
			tier1BufferLList = new LinkedList<>(
					Collections.nCopies(windowDimension, 0.0));
			System.out.println("\n\t"+bufferLList);
			System.exit(0);
		}
		
		tier1BufferLList = new LinkedList<>(tier1BufferLList.stream()
				.map((currTerm) -> (currTerm*termFactor)).collect(Collectors.toList()));
		
		/**
		LinkedList<Double> tier2BufferLList = new LinkedList<>();
		tier2BufferLList.add((tier1BufferLList.stream().reduce((x,y)->x+y).get()/new Double(tier1BufferLList.size())));
		**/
		
		/*if(contextSequenceIndex >= windowDimension){
			tier1BufferLList = new LinkedList<>(tier1BufferLList.stream().map(val -> val.doubleValue()*1000).collect(Collectors.toList()));
		}*/
		/**
		LinkedList<Double> tier2BufferLList = 
				new LinkedList<>(Arrays.asList((tier1BufferLList.stream().reduce((x,y)->x+y).get()/tier1BufferLList.size())));
		**/
		
		tier1ResultBufferMap.put(triggerIndex.toString(), tier1BufferLList);
		//System.out.println("\t"+triggerIndex+"\t"+tier1BufferLList);
		//System.out.println("\t"+tier1ResultBufferMap.size());
		return(tier1ResultBufferMap);
	}
	
	private boolean validateClusterIndex(TreeSet<String> bufferList,
			Integer contextId, Integer clusterId) {

		boolean retStatus = false;
		String patternTemplate = String.valueOf(contextId).concat("-"+String.valueOf(clusterId));
		if(bufferList.stream().filter((currVal) -> currVal.equals(patternTemplate))
				.collect(Collectors.toList()).size() > 0){
			retStatus = true;
		}
		
		return(retStatus);
	}

	
	
	private TreeMap<String, LinkedList<String>> getSequenceBasedInvariantContextRepresentation(
			LinkedList<String> bufferLList, Integer triggerIndex, 
			int contextSequenceIndex, int termFactor, 
			TreeSet<String> bufferList) {

		GeometricProjection projectionInstance = new GeometricProjection();
		boolean notVariant = projectionInstance.triggerEventPresent(bufferLList);
		//System.out.println("\t\t\t"+bufferLList+"\t"+notVariant);
		TreeMap<String, LinkedList<String>> tier1ResultBufferMap = new TreeMap<>();
		Iterator<Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> tier1Itr = 
				invariantClusters.get(triggerIndex).entrySet().iterator();
		TreeMap<Double, LinkedList<String>> tier1BufferMap = new TreeMap<>(); 
		TreeMap<Double, String> tier2BufferMap = new TreeMap<>();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>> tier1MapValue = 
					tier1Itr.next();
			if(!bufferList.isEmpty()){
				//System.out.println("\t>>>"+ bufferList);
				if(!validateClusterIndex(bufferList, (triggerIndex+1), tier1MapValue.getKey())){
					//System.out.println(" not present "+ (triggerIndex+1)+"-"+tier1MapValue.getKey());
					continue;
				}
			}
			Iterator<Map.Entry<LinkedList<String>, LinkedList<Double>>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<LinkedList<String>, LinkedList<Double>> tier2MapValue = tier2Itr.next();
				/**
				 * If terms match, then get the invariant score
				 */
				Double matchScore = new Double(0).doubleValue();
				if(notVariant){
					matchScore = layeredAlignment(
							tier2MapValue.getKey(), bufferLList, triggerIndex);					
				}else{
					matchScore = layeredAlignment(
							tier2MapValue.getKey(), bufferLList);
				}
				int matchValue = Double.compare(matchScore, new Double(0).doubleValue());
				if(matchValue > 0){
					Integer bufferValue = triggerIndex+1;
					tier2BufferMap.put(matchScore, 
							bufferValue.toString()+"-"+tier1MapValue.getKey().toString());
				}
			}
		}
		//tier1ResultBufferMap.put(triggerIndex.toString(), tier1BufferMap.firstEntry().getValue());
		LinkedList<String> tier3BufferLList = null;
		if((!notVariant) && (tier2BufferMap.isEmpty())){
			//System.out.println("\t"+bufferLList);
			Integer bufferIndex = (triggerIndex+1);
			tier2BufferMap.put(-1.0, bufferIndex.toString()+"-0");
		}
		//System.out.println("\t"+tier2BufferMap);
		tier3BufferLList = new LinkedList<>(
				Arrays.asList(tier2BufferMap.descendingMap().firstEntry().getValue()));
		
		tier1ResultBufferMap.put(triggerIndex.toString(), tier3BufferLList);
		return(tier1ResultBufferMap);
	}
	
	private String getSequenceBasedInvariantContextRepresentation(
			LinkedList<String> bufferLList, Integer triggerBufferIndex, TreeSet<String> bufferList) {

		String tier1BufferResultString = null;
		GeometricProjection projectionInstance = new GeometricProjection();
		boolean notTriggerTemplate = projectionInstance.triggerEventPresent(bufferLList);
		if(notTriggerTemplate){
			for(int i=0;i<bufferLList.size();i++){
				if((i != triggerBufferIndex) && (bufferLList.get(i).equals(normTriggerTerm))){
					bufferLList.set(i, "PROTEINT");
				}
			}
		}
		//System.out.println("\t"+bufferLList+"\t"+notTriggerTemplate+"\t"+triggerBufferIndex);
		ArrayList<Integer> tier1BufferList = new ArrayList<>();
		if(triggerBufferIndex == -1){
			tier1BufferList = new ArrayList<>(invariantClusters.keySet().stream()
					.filter((currVal) -> currVal >= windowDimension)
					.collect(Collectors.toList()));
		}else{
			tier1BufferList.add(triggerBufferIndex);
		}
		Iterator<Integer> tier0Itr = tier1BufferList.iterator();
		int contextSize = tier1BufferList.size(), contextCount=0;;
		while(tier0Itr.hasNext()){
			Integer triggerIndex = tier0Itr.next();
			Iterator<Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> tier1Itr = 
					invariantClusters.get(triggerIndex).entrySet().iterator();
			TreeMap<Double, String> tier2BufferMap = new TreeMap<>();
			TreeMap<Double, String> tier0BufferMap = new TreeMap<>();
			while(tier1Itr.hasNext()){
				Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>> tier1MapValue = 
						tier1Itr.next();
				if(!bufferList.isEmpty()){
					//System.out.println("\t>>>"+ bufferList);
					if(!validateClusterIndex(bufferList, (triggerIndex+1), tier1MapValue.getKey())){
						//System.out.println(" not present "+ (triggerIndex+1)+"-"+tier1MapValue.getKey());
						//System.out.println("\t"+tier1MapValue.getValue().entrySet().iterator().next());
						continue;
					}
				}
				Iterator<Map.Entry<LinkedList<String>, LinkedList<Double>>> tier2Itr = 
						tier1MapValue.getValue().entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<LinkedList<String>, LinkedList<Double>> tier2MapValue = tier2Itr.next();
					/**
					 * If terms match, then get the invariant score
					 */
					Double matchScore = new Double(0).doubleValue();
					if(notTriggerTemplate){
						matchScore = layeredAlignment(
								tier2MapValue.getKey(), bufferLList, triggerIndex);					
					}else{
						matchScore = layeredAlignment(
								tier2MapValue.getKey(), bufferLList);
					}
					int matchValue = Double.compare(matchScore, new Double(0).doubleValue());
					if(matchValue > 0){
						Integer bufferValue = triggerIndex+1;
						tier2BufferMap.put(matchScore, 
								bufferValue.toString()+"-"+tier1MapValue.getKey().toString());
						tier0BufferMap.put(matchScore, 
								tier2MapValue.getKey().stream().reduce((x,y)->x+","+y).get().toString());
					}
				}
			}
			
			/**
			if(tier2BufferMap.isEmpty()){
				System.out.println("\t no match >> "+bufferLList+"\t"+triggerIndex);
				System.exit(0);
			}**/
			
			LinkedList<String> tier3BufferLList = null;
			if((!notTriggerTemplate) && (tier2BufferMap.isEmpty())){
				//System.out.println("\t"+bufferLList);
				Integer bufferIndex = (triggerIndex+1);
				tier2BufferMap.put(-1.0, bufferIndex.toString()+"-0");
			}
			if((tier2BufferMap.isEmpty())){
				//System.out.println("\t unmapped>>"+bufferLList);
				Integer bufferIndex = (triggerIndex+1);
				tier2BufferMap.put(-1.0, bufferIndex.toString()+"-0");
			}
			contextCount++;
			
			//System.out.println("\t"+tier0BufferMap.descendingMap()+"\n"+tier2BufferMap.descendingMap());
			tier3BufferLList = new LinkedList<>(
					Arrays.asList(tier2BufferMap.descendingMap().firstEntry().getValue()));
			
			tier1BufferResultString = tier3BufferLList.peekFirst();
			
			if((!tier3BufferLList.get(0).split("-")[1].contentEquals("0"))
					|| (contextCount == contextSize)){
				break;
			}
		}
		
		return(tier1BufferResultString);
	}

	public ArrayList<LinkedList<String>> callVariantFrameConfiguration(Integer seedFrame,
			ArrayList<String> extendedArray) throws IOException {

		//System.out.println("\t\t"+extendedArray);
		FrameFractioner frameInstance = new FrameFractioner();
		ArrayList<LinkedList<String>> tier1ResultBufferList = new ArrayList<>();
		ArrayList<Integer> extendedArrayIndex =  (ArrayList<Integer>) 
				IntStream.range(0, extendedArray.size())
				.boxed().collect(Collectors.toList());
		
		ArrayList<Integer> triggerIndicesArray = new ArrayList<>();
		Iterator<Integer> tier1Itr = extendedArrayIndex.iterator();
		while(tier1Itr.hasNext()){
			Integer tier1IntergerValue = tier1Itr.next();
			if(extendedArray.get(tier1IntergerValue).contentEquals(normTriggerTerm)){
				for(int i=0;i<seedFrame-1;i++){
					triggerIndicesArray.add(tier1IntergerValue-i);
					triggerIndicesArray.add(tier1IntergerValue+i);
				}
			}
		}
		for(int startIndex=0;startIndex<(extendedArrayIndex.size()-seedFrame);startIndex++){
			//System.out.println("\t\t"+extendedArray.get(startIndex));
			int endIndex = (startIndex+seedFrame);
			LinkedList<Integer> tier1BufferLList = new LinkedList<>(
					IntStream.range(startIndex, endIndex).boxed().collect(Collectors.toList()));
			LinkedList<String> tier2BufferLList = new LinkedList<>(
					tier1BufferLList.stream()
					.map((val)->extendedArray.get(val)).collect(Collectors.toList()));
			if(!frameInstance.triggerEventPresent(tier2BufferLList)){
				if(tier1BufferLList.stream()
						.filter((val) -> triggerIndicesArray.contains(val))
						.collect(Collectors.toList()).size() == 0){
					//System.out.println("\t"+tier2BufferLList);
					tier1ResultBufferList.add(tier2BufferLList);
				}
			}
		}
		
		return(tier1ResultBufferList);
	}

	public TreeMap<Integer, TreeMap<String, LinkedList<Double>>> 
	mapToSequenceInvariantRepresentation(LinkedList<String> bufferLList) {

		//System.out.println("\t"+bufferLList);
		int contextSequenceIndex = 0;
		TreeMap<Integer, TreeMap<String, LinkedList<Double>>> tier1BufferMap = new TreeMap<>();
		ArrayList<Integer> tier1BufferArray = new ArrayList<>();
		for(int i=0;i<bufferLList.size();i++){
			String tier1String = bufferLList.get(i);
			if(tier1String.contentEquals(normTriggerTerm)){
				tier1BufferArray.add(i);
			}
		}
		
		for(int i=0;i<tier1BufferArray.size();i++){
			//System.out.println("\n\t"+i);
			int termFactor = 1;
			if(i>0){
				termFactor = 1;
			}
			int triggerIndex = tier1BufferArray.get(i);
			int bufferTriggerIndex = (windowDimension-1);
			for(int startIndex=(triggerIndex-(windowDimension-1));startIndex<(triggerIndex+1);startIndex++){
				int endIndex = (startIndex+windowDimension);
				LinkedList<String> tier1BufferLList = 
						new LinkedList<>(bufferLList.subList(startIndex, endIndex));
				/**
				 * Representations allowed only for contexts with trigger words
				 */
				if(tier1BufferLList.contains(normTriggerTerm)){
					int currentIndex = 0;
					while(currentIndex < tier1BufferLList.size()){
						if((tier1BufferLList.get(currentIndex).contentEquals(normTriggerTerm))
								&& (currentIndex != bufferTriggerIndex)){
							tier1BufferLList.set(currentIndex, "NN");
						}
						currentIndex++;
					}
					//System.out.println("\t"+tier1BufferLList);
					TreeMap<String, LinkedList<Double>> tier2BufferMap = new TreeMap<>();
					if(tier1BufferMap.containsKey(contextSequenceIndex)){
						tier2BufferMap = tier1BufferMap.get(contextSequenceIndex);
					}
					tier2BufferMap = getSequenceBasedInvariantRepresentation(
							tier1BufferLList, bufferTriggerIndex, contextSequenceIndex, termFactor);
					//if(bufferTriggerIndex != 2){
						tier1BufferMap.put(contextSequenceIndex, tier2BufferMap);
					//}
					contextSequenceIndex++;
					bufferTriggerIndex--;
				}
			}
		}
		
		/**
		Iterator<Map.Entry<Integer, TreeMap<String, LinkedList<Double>>>> t1 = 
				tier1BufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, TreeMap<String, LinkedList<Double>>> t1V = t1.next();
			System.out.println("\t"+t1V.getKey()+"\t"+t1V.getValue());
		}
		System.exit(0);**/
		
		return(tier1BufferMap);
	}

	public TreeMap<Integer, TreeMap<String, LinkedList<String>>> 
	mapToSequenceInvariantContextRepresentation(
			LinkedList<String> bufferLList, TreeSet<String> bufferList){
		
		//System.out.println("\t"+bufferLList);
		int contextSequenceIndex = 0;
		TreeMap<Integer, TreeMap<String, LinkedList<String>>> tier1BufferMap = new TreeMap<>();
		ArrayList<Integer> tier1BufferArray = new ArrayList<>();
		for(int i=0;i<bufferLList.size();i++){
			String tier1String = bufferLList.get(i);
			if(tier1String.contentEquals(normTriggerTerm)){
				tier1BufferArray.add(i);
			}
		}
		
		for(int i=0;i<tier1BufferArray.size();i++){
			//System.out.println("\n\t"+i);
			int termFactor = 1;
			if(i>0){
				termFactor = 1;
			}
			int triggerIndex = tier1BufferArray.get(i);
			int bufferTriggerIndex = (windowDimension-1);
			for(int startIndex=(triggerIndex-(windowDimension-1));startIndex<(triggerIndex+1);startIndex++){
				int endIndex = (startIndex+windowDimension);
				LinkedList<String> tier1BufferLList = 
						new LinkedList<>(bufferLList.subList(startIndex, endIndex));
				/**
				 * Representations allowed only for contexts with trigger words
				 */
				if(tier1BufferLList.contains(normTriggerTerm)){
					int currentIndex = 0;
					while(currentIndex < tier1BufferLList.size()){
						if((tier1BufferLList.get(currentIndex).contentEquals(normTriggerTerm))
								&& (currentIndex != bufferTriggerIndex)){
							tier1BufferLList.set(currentIndex, "PROTEINT");
						}
						currentIndex++;
					}
					//System.out.println("\t"+tier1BufferLList);
					TreeMap<String, LinkedList<String>> tier2BufferMap = new TreeMap<>();
					if(tier1BufferMap.containsKey(contextSequenceIndex)){
						tier2BufferMap = tier1BufferMap.get(contextSequenceIndex);
					}
					tier2BufferMap = getSequenceBasedInvariantContextRepresentation(
							tier1BufferLList, bufferTriggerIndex, contextSequenceIndex,
							termFactor, bufferList);
					//if(bufferTriggerIndex == 0){
						tier1BufferMap.put(contextSequenceIndex, tier2BufferMap);
						contextSequenceIndex++;
					//}
					
					bufferTriggerIndex--;
				}
			}
		}
		
		/**
		Iterator<Map.Entry<Integer, TreeMap<String, LinkedList<String>>>> t1 = 
				tier1BufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, TreeMap<String, LinkedList<String>>> t1V = t1.next();
			System.out.println("\t"+t1V.getKey()+"\t"+t1V.getValue());
		}
		System.exit(0);**/
				
		return(tier1BufferMap);
	}

	public TreeMap<Integer, TreeMap<String, LinkedList<String>>> 
	mapToSequenceInvariantDifferentialRepresentation(
			LinkedList<String> bufferLList, int currentIndex, 
			HashMap<String, Integer> contextPositionUpdateMap, 
			TreeSet<String> bufferList) throws IOException {

		TreeMap<Integer, TreeMap<String, LinkedList<String>>> tier1BufferMap = new TreeMap<>();
		//System.out.println(">>"+bufferLList);
		ArrayList<LinkedList<String>> tier1ResultBufferList = callVariantFrameConfiguration(
				windowDimension, new ArrayList<>(bufferLList));
		//System.out.println("\t\t"+tier1ResultBufferList);
		if(!tier1ResultBufferList.isEmpty()){
			for(LinkedList<String> tier1BufferLList: tier1ResultBufferList){
				//System.out.println("\t\t"+tier1BufferLList);
				int contextIndex = -1;
				TreeMap<String, LinkedList<String>> tier2BufferMap = new TreeMap<>();
				if(contextPositionUpdateMap.containsKey(tier1BufferLList.get(0))){
					contextIndex = contextPositionUpdateMap.get(tier1BufferLList.get(0));
					if(tier1BufferMap.containsKey(currentIndex)){
						tier2BufferMap = tier1BufferMap.get(currentIndex);
					}
					tier2BufferMap = getSequenceBasedInvariantContextRepresentation(
							tier1BufferLList, contextIndex, 0, 1, bufferList);
				}else{
					Iterator<Map.Entry<String, Integer>> tier1Itr = 
							contextPositionUpdateMap.entrySet().iterator();
					while(tier1Itr.hasNext()){
						tier2BufferMap.clear();
						contextIndex = tier1Itr.next().getValue();
						if(tier1BufferMap.containsKey(currentIndex)){
							tier2BufferMap = tier1BufferMap.get(currentIndex);
						}
						tier2BufferMap = getSequenceBasedInvariantContextRepresentation(
								tier1BufferLList, contextIndex, 0, 1, bufferList);
						//System.out.println("\t"+tier2BufferMap);
						if(!tier2BufferMap.get(String.valueOf(contextIndex))
								.get(0).split("-")[1].contentEquals("0")){
							break;
						}
					}
				}
				if(!tier2BufferMap.get(String.valueOf(contextIndex)).get(0)
						.equals(String.valueOf(contextIndex+1)+"-0")){
					tier1BufferMap.put(currentIndex, tier2BufferMap);
					currentIndex++;
				}
			}
		}
		if((tier1BufferMap.isEmpty()) || (tier1BufferMap.size() < windowDimension)){
			for(int i=0;i<windowDimension;i++){
				TreeMap<String, LinkedList<String>> tier2BufferMap = new TreeMap<>();
				Integer bufferIndex = windowDimension+(i+1);
				tier2BufferMap.put(bufferIndex.toString(), new LinkedList<>(
						Arrays.asList(bufferIndex.toString()+"-0")));
				if(tier1BufferMap.size() < windowDimension){
					tier1BufferMap.put(currentIndex, tier2BufferMap);
				}else{
					break;
				}
				currentIndex++;
				System.err.println("null error in variant>> "+"\n\t"+bufferLList
						+"\n\t"+tier1BufferMap);
			}
		}
		
		/**
		Iterator<Map.Entry<Integer, TreeMap<String, LinkedList<String>>>> t1 = 
				tier1BufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, TreeMap<String, LinkedList<String>>> t1V = t1.next();
			System.out.println("\t"+t1V.getKey()+"\t"+t1V.getValue());
		}
		System.exit(0);**/
		
		return (tier1BufferMap);
	}
	
	private LinkedList<String> triggerTermTrimmer(LinkedList<String> bufferList) {

		//System.out.println("initial>>"+bufferList);
		ArrayList<Integer> triggerIndex = new ArrayList<>(
				IntStream.range(1, windowDimension+1).boxed().collect(Collectors.toList()));
		ArrayList<Integer> rangeArray = new ArrayList<>();
		for(int i=0;i<bufferList.size();i++){
			int currContext = Integer.parseInt(bufferList.get(i).split("\\-")[0]);
			if(triggerIndex.contains(currContext)){
				rangeArray.add(i);
			}
		}
		if(rangeArray.size() != 2){
			System.err.println("critical error ~ triggerTermTrimmer()");
		}else{
			if(rangeArray.get(0) >= 2){
				rangeArray.set(0, (rangeArray.get(0)-2));
			}
			if(rangeArray.get(1) < bufferList.size()-2){
				rangeArray.set(1, (rangeArray.get(1)+2));
			}
			bufferList = new LinkedList<>(bufferList.subList(
					rangeArray.get(0), (rangeArray.get(1)+1)));
		}
		//System.out.println("trimmer>>"+bufferList);
		return(bufferList);
	}
	
	public TreeMap<Integer, LinkedList<String>> 
	mapToSequenceInvariantContextRepresentation(
			LinkedList<String> bufferLList,
			HashMap<String, Integer> contextPositionUpdateMap, TreeSet<String> bufferList){
		
		//System.out.println("\t"+bufferLList);
		LinkedList<Integer> bufferIndexLList = new LinkedList<>(
				IntStream.range(0, bufferLList.size()).boxed().collect(Collectors.toList()));
		ArrayList<Integer> tier1BufferList = new ArrayList<>(); 
		for(int i=0;i<bufferLList.size();i++){
			String tier1String = bufferLList.get(i);
			if(tier1String.contentEquals(normTriggerTerm)){
				for(int j=(windowDimension-1);j>=0;j--){
					tier1BufferList.add((i-j));
				}
			}
		}
		
		ArrayList<LinkedList<Integer>> tier2BufferList = new ArrayList<>();
		for(int i=0;i<(tier1BufferList.size()-windowDimension);i++){
			for(int j = windowDimension;j<tier1BufferList.size();j++){
				LinkedList<Integer> tier1BufferLList = new LinkedList<>();
				tier1BufferLList.add(tier1BufferList.get(i));
				tier1BufferLList.add(tier1BufferList.get(j));
				tier2BufferList.add(tier1BufferLList);
			}
		}
		
		TreeMap<Integer, LinkedList<String>> tier1ResultBufferMap = new TreeMap<>();
		for(int i=0;i<tier2BufferList.size();i++){
			LinkedList<Integer> tier2BufferLList = new LinkedList<>(tier2BufferList.get(i));
			LinkedList<String> iconFeatureMap = new LinkedList<>();
			//System.out.println("\t**************************"+tier2BufferLList+"\t**************");
			for(int startIndex=0;startIndex<(bufferIndexLList.size()-windowDimension);startIndex++){
				int bufferTriggerIndex = 0;
				int endIndex = (startIndex+windowDimension);
				int threshold = (startIndex+windowDimension-1);
				LinkedList<Integer> tier4BufferLList = new LinkedList<>(
						tier2BufferLList.stream()
						.filter((currVal) -> threshold >= currVal)
						.collect(Collectors.toList()));
				//System.out.println("\t"+startIndex+"\t>>"+tier4BufferLList);
				LinkedList<Integer> tier3BufferLList = new LinkedList<>();
				boolean triggerTermInstance = false;
				if(!tier4BufferLList.isEmpty()){
					Iterator<Integer> tier1Itr = tier2BufferLList.iterator();
					while(tier1Itr.hasNext()){
						Integer tier1IntegerValue = tier1Itr.next();
						Iterator<Integer> tier2Itr = tier4BufferLList.iterator();
						while(tier2Itr.hasNext()){
							if(tier1IntegerValue == tier2Itr.next()){
								triggerTermInstance = true;
								//removedIndex 
								startIndex = tier1IntegerValue;
								tier1Itr.remove();
								break;
								//tier2Itr.remove();
							}
						}
						break;
					}
					endIndex = (startIndex+windowDimension);
				}
				tier3BufferLList = new LinkedList<>(
						bufferIndexLList.subList(startIndex, endIndex));
				
				LinkedList<String> tier1BufferResultLList = new LinkedList<>(
						tier3BufferLList.stream()
						.map((index) -> bufferLList.get(index)).collect(Collectors.toList()));
				if(triggerTermInstance){
					//bufferTriggerIndex = tier1BufferResultLList.indexOf(normTriggerTerm);
					bufferTriggerIndex = tier1BufferResultLList.lastIndexOf(normTriggerTerm);
					//startIndex = endIndex-1;
				}else{
					if(contextPositionUpdateMap.containsKey(tier1BufferResultLList.get(0))){
						bufferTriggerIndex = contextPositionUpdateMap
								.get(tier1BufferResultLList.get(0));
					}else{
						bufferTriggerIndex = -1;
					}
				}
				//System.out.println("\t"+tier1BufferResultLList+"\t"+triggerTermInstance);
				
				if((tier1BufferResultLList.stream()
						.filter((currVal) -> currVal.matches("@"))
						.collect(Collectors.toList()).size() > 0) && (!triggerTermInstance)){
					continue;
				}
				
				if((tier1BufferResultLList.contains(normTriggerTerm)) && (!triggerTermInstance)){
					continue;
				}
				//System.out.println("\t>>"+tier1BufferResultLList+"\t"+bufferTriggerIndex);
				String tier1BufferResultString = getSequenceBasedInvariantContextRepresentation(
						tier1BufferResultLList, bufferTriggerIndex, bufferList);
				//System.out.println("match \t::"+tier1BufferResultString);
				iconFeatureMap.add(tier1BufferResultString);
			}
			iconFeatureMap = triggerTermTrimmer(iconFeatureMap);
			tier1ResultBufferMap.put(i, iconFeatureMap);
			break;
		}
		
		/**
		Iterator<Map.Entry<Integer, LinkedList<String>>> t1 = 
				tier1ResultBufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, LinkedList<String>> t1V = t1.next();
			System.out.println("\t"+t1V.getKey()+"\t"+t1V.getValue());
		}
		System.exit(0);**/
				
		return(tier1ResultBufferMap);
	}
	
	private ArrayList<LinkedList<String>> triggerOrgTermTrimmer(
			ArrayList<LinkedList<String>> iconFeatureMap) {
		
		for(int i=0;i<iconFeatureMap.size();i++){
			if(iconFeatureMap.get(i).contains(normTriggerTerm)){
				int startIndex = i;
				if(i >= 2){
					startIndex = i-2;
				}
				iconFeatureMap = new ArrayList<>(
						iconFeatureMap.subList(startIndex, iconFeatureMap.size()));
				break;
			}
		}
		
		for(int i=iconFeatureMap.size()-1;i>=0;i--){
			if(iconFeatureMap.get(i).contains(normTriggerTerm)){
				int endIndex = i;
				if(i < iconFeatureMap.size()-2){
					endIndex = i+2;
				}
				iconFeatureMap = new ArrayList<>(
						iconFeatureMap.subList(0, endIndex+1));
				break;
			}
		}
		
		//System.out.println(">>"+iconFeatureMap);
		return(iconFeatureMap);
	}

	public TreeMap<Integer, ArrayList<LinkedList<String>>> 
	mapToSequencePOSContextRepresentation(
			LinkedList<String> bufferLList){
		
		//System.out.println("\t"+bufferLList);
		LinkedList<Integer> bufferIndexLList = new LinkedList<>(
				IntStream.range(0, bufferLList.size()).boxed().collect(Collectors.toList()));
		ArrayList<Integer> tier1BufferList = new ArrayList<>(); 
		for(int i=0;i<bufferLList.size();i++){
			String tier1String = bufferLList.get(i);
			if(tier1String.contentEquals(normTriggerTerm)){
				for(int j=(windowDimension-1);j>=0;j--){
					tier1BufferList.add((i-j));
				}
			}
		}
		
		ArrayList<LinkedList<Integer>> tier2BufferList = new ArrayList<>();
		for(int i=0;i<(tier1BufferList.size()-windowDimension);i++){
			for(int j = windowDimension;j<tier1BufferList.size();j++){
				LinkedList<Integer> tier1BufferLList = new LinkedList<>();
				tier1BufferLList.add(tier1BufferList.get(i));
				tier1BufferLList.add(tier1BufferList.get(j));
				tier2BufferList.add(tier1BufferLList);
			}
		}
		
		TreeMap<Integer, ArrayList<LinkedList<String>>> tier1ResultBufferMap = new TreeMap<>();
		for(int i=0;i<tier2BufferList.size();i++){
			LinkedList<Integer> tier2BufferLList = new LinkedList<>(tier2BufferList.get(i));
			ArrayList<LinkedList<String>> iconFeatureMap = new ArrayList<>();
			//System.out.println("\t**************************"+tier2BufferLList+"\t**************");
			for(int startIndex=0;startIndex<(bufferIndexLList.size()-windowDimension);startIndex++){
				int bufferTriggerIndex = 0;
				int endIndex = (startIndex+windowDimension);
				int threshold = (startIndex+windowDimension-1);
				LinkedList<Integer> tier4BufferLList = new LinkedList<>(
						tier2BufferLList.stream()
						.filter((currVal) -> threshold >= currVal)
						.collect(Collectors.toList()));
				//System.out.println("\t"+startIndex+"\t>>"+tier4BufferLList);
				LinkedList<Integer> tier3BufferLList = new LinkedList<>();
				boolean triggerTermInstance = false;
				if(!tier4BufferLList.isEmpty()){
					Iterator<Integer> tier1Itr = tier2BufferLList.iterator();
					while(tier1Itr.hasNext()){
						Integer tier1IntegerValue = tier1Itr.next();
						Iterator<Integer> tier2Itr = tier4BufferLList.iterator();
						while(tier2Itr.hasNext()){
							if(tier1IntegerValue == tier2Itr.next()){
								triggerTermInstance = true;
								//removedIndex 
								startIndex = tier1IntegerValue;
								tier1Itr.remove();
								break;
								//tier2Itr.remove();
							}
						}
						break;
					}
					endIndex = (startIndex+windowDimension);
				}
				tier3BufferLList = new LinkedList<>(
						bufferIndexLList.subList(startIndex, endIndex));
				
				LinkedList<String> tier1BufferResultLList = new LinkedList<>(
						tier3BufferLList.stream()
						.map((index) -> bufferLList.get(index)).collect(Collectors.toList()));
				
				if(triggerTermInstance){
					//bufferTriggerIndex = tier1BufferResultLList.indexOf(normTriggerTerm);
					bufferTriggerIndex = tier1BufferResultLList.lastIndexOf(normTriggerTerm);
					//startIndex = endIndex-1;
				}
				
				if((tier1BufferResultLList.stream()
						.filter((currVal) -> currVal.matches("@"))
						.collect(Collectors.toList()).size() > 0) && (!triggerTermInstance)){
					continue;
				}
				
				if((tier1BufferResultLList.contains(normTriggerTerm)) && (!triggerTermInstance)){
					continue;
				}
				for(int j=0;j<tier1BufferResultLList.size();j++){
					if((bufferTriggerIndex != j) 
							&& (tier1BufferResultLList.get(j).equals(normTriggerTerm))){
						tier1BufferResultLList.set(j, "PROTEINT");
					}
				}
				iconFeatureMap.add(tier1BufferResultLList);
				//System.out.println("\t"+tier1BufferResultLList+"\t"+triggerTermInstance);
			}
			iconFeatureMap = triggerOrgTermTrimmer(iconFeatureMap);
			tier1ResultBufferMap.put(i, iconFeatureMap);
			break;
		}
		
		/**
		Iterator<Map.Entry<Integer, LinkedList<String>>> t1 = 
				tier1ResultBufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, LinkedList<String>> t1V = t1.next();
			System.out.println("\t"+t1V.getKey()+"\t"+t1V.getValue());
		}
		System.exit(0);**/
				
		return(tier1ResultBufferMap);
	}
	
	
	public TreeMap<Integer, ArrayList<LinkedList<String>>> 
	mapToSequenceOriginalContextRepresentation(
			LinkedList<String> bufferLList){
		
		//System.out.println("\t"+bufferLList);
		LinkedList<Integer> bufferIndexLList = new LinkedList<>(
				IntStream.range(0, bufferLList.size()).boxed().collect(Collectors.toList()));
		ArrayList<Integer> tier1BufferList = new ArrayList<>(); 
		for(int i=0;i<bufferLList.size();i++){
			String tier1String = bufferLList.get(i);
			if(tier1String.contentEquals(normTriggerTerm)){
				for(int j=(windowDimension-1);j>=0;j--){
					tier1BufferList.add((i-j));
				}
			}
		}
		
		ArrayList<LinkedList<Integer>> tier2BufferList = new ArrayList<>();
		for(int i=0;i<(tier1BufferList.size()-windowDimension);i++){
			for(int j = windowDimension;j<tier1BufferList.size();j++){
				LinkedList<Integer> tier1BufferLList = new LinkedList<>();
				tier1BufferLList.add(tier1BufferList.get(i));
				tier1BufferLList.add(tier1BufferList.get(j));
				tier2BufferList.add(tier1BufferLList);
			}
		}
		
		TreeMap<Integer, ArrayList<LinkedList<String>>> tier1ResultBufferMap = new TreeMap<>();
		for(int i=0;i<tier2BufferList.size();i++){
			LinkedList<Integer> tier2BufferLList = new LinkedList<>(tier2BufferList.get(i));
			ArrayList<LinkedList<String>> iconFeatureMap = new ArrayList<>();
			//System.out.println("\t**************************"+tier2BufferLList+"\t**************");
			for(int startIndex=0;startIndex<(bufferIndexLList.size()-windowDimension);startIndex++){
				int bufferTriggerIndex = 0;
				int endIndex = (startIndex+windowDimension);
				int threshold = (startIndex+windowDimension-1);
				LinkedList<Integer> tier4BufferLList = new LinkedList<>(
						tier2BufferLList.stream()
						.filter((currVal) -> threshold >= currVal)
						.collect(Collectors.toList()));
				//System.out.println("\t"+startIndex+"\t>>"+tier4BufferLList);
				LinkedList<Integer> tier3BufferLList = new LinkedList<>();
				boolean triggerTermInstance = false;
				if(!tier4BufferLList.isEmpty()){
					Iterator<Integer> tier1Itr = tier2BufferLList.iterator();
					while(tier1Itr.hasNext()){
						Integer tier1IntegerValue = tier1Itr.next();
						Iterator<Integer> tier2Itr = tier4BufferLList.iterator();
						while(tier2Itr.hasNext()){
							if(tier1IntegerValue == tier2Itr.next()){
								triggerTermInstance = true;
								//removedIndex 
								startIndex = tier1IntegerValue;
								tier1Itr.remove();
								break;
								//tier2Itr.remove();
							}
						}
						break;
					}
					endIndex = (startIndex+windowDimension);
				}
				tier3BufferLList = new LinkedList<>(
						bufferIndexLList.subList(startIndex, endIndex));
				
				LinkedList<String> tier1BufferResultLList = new LinkedList<>(
						tier3BufferLList.stream()
						.map((index) -> bufferLList.get(index)).collect(Collectors.toList()));
				
				if(triggerTermInstance){
					//bufferTriggerIndex = tier1BufferResultLList.indexOf(normTriggerTerm);
					bufferTriggerIndex = tier1BufferResultLList.lastIndexOf(normTriggerTerm);
					//startIndex = endIndex-1;
				}
				
				if((tier1BufferResultLList.stream()
						.filter((currVal) -> currVal.matches("@"))
						.collect(Collectors.toList()).size() > 0) && (!triggerTermInstance)){
					continue;
				}
				
				if((tier1BufferResultLList.contains(normTriggerTerm)) && (!triggerTermInstance)){
					continue;
				}
				
				for(int j=0;j<tier1BufferResultLList.size();j++){
					if((bufferTriggerIndex != j) 
							&& (tier1BufferResultLList.get(j).equals(normTriggerTerm))){
						tier1BufferResultLList.set(j, "PROTEINT");
					}
				}
				
				iconFeatureMap.add(tier1BufferResultLList);
				//System.out.println("\t"+tier1BufferResultLList+"\t"+triggerTermInstance);
			}
			iconFeatureMap = triggerOrgTermTrimmer(iconFeatureMap);
			tier1ResultBufferMap.put(i, iconFeatureMap);
			break;
		}
		
		/**
		Iterator<Map.Entry<Integer, LinkedList<String>>> t1 = 
				tier1ResultBufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, LinkedList<String>> t1V = t1.next();
			System.out.println("\t"+t1V.getKey()+"\t"+t1V.getValue());
		}
		System.exit(0);**/
				
		return(tier1ResultBufferMap);
	}
	

	public ArrayList<LinkedList<String>> generateClusterFeaturePairs(
			TreeMap<Integer, TreeMap<String, LinkedList<String>>> bufferMap) {
		
		ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>();
		for(int i=0;i<(bufferMap.size()-windowDimension);i++){
			for(int j = windowDimension;j<bufferMap.size();j++){
				LinkedList<String> tier1BufferLList = new LinkedList<>();
				tier1BufferLList.add(bufferMap.get(i).firstEntry().getValue().peekFirst());
				tier1BufferLList.add(bufferMap.get(j).firstEntry().getValue().peekFirst());
				tier1BufferList.add(tier1BufferLList);
			}
		}
		
		/**
		Iterator<LinkedList<String>> t1Itr = tier1BufferList.iterator();
		while(t1Itr.hasNext()){
			LinkedList<String> t1V = t1Itr.next();
			System.out.println("\t"+t1V);
		}
		System.exit(0);**/
		
		return tier1BufferList;

	}
	
	public ArrayList<LinkedList<String>> addVariantFeaturePairsToCluster(
			ArrayList<LinkedList<String>> bufferList,
			TreeMap<Integer, TreeMap<String, LinkedList<String>>> bufferMap) {

		//System.out.println("\t>>"+bufferList);
		//System.out.println("\t>>"+bufferMap);
		ArrayList<LinkedList<String>> tier1ResultBufferList = new ArrayList<>();
		Iterator<LinkedList<String>> tier1Itr = bufferList.iterator();
		while(tier1Itr.hasNext()){
			LinkedList<String> tier1LListValue = tier1Itr.next();
			ArrayList<Integer> tier0BufferList = new ArrayList<>(bufferMap.keySet());
			for(int startIndex=0;startIndex<(tier0BufferList.size()-(windowDimension-1));startIndex++){
				int endIndex = startIndex+windowDimension;
				LinkedList<String> tier1BufferLList = new LinkedList<>(tier1LListValue);
				Iterator<Integer> tier2Itr = tier0BufferList.subList(startIndex, endIndex).iterator();
				while(tier2Itr.hasNext()){
					Integer tier2IntegerValue = tier2Itr.next();
					Iterator<Map.Entry<String, LinkedList<String>>> tier3Itr = 
							bufferMap.get(tier2IntegerValue).entrySet().iterator();
					while(tier3Itr.hasNext()){
						Map.Entry<String, LinkedList<String>> tier3MapValue = tier3Itr.next();
						Iterator<String> tier4Itr = tier3MapValue.getValue().iterator();
						while(tier4Itr.hasNext()){
							tier1BufferLList.add(tier4Itr.next());
						}
					}
				}
				//System.out.println(">>>"+tier1BufferLList);
				tier1ResultBufferList.add(tier1BufferLList);
			}
		}
		/**
		Iterator<LinkedList<String>> t1Itr = tier1ResultBufferList.iterator();
		while(t1Itr.hasNext()){
			LinkedList<String> t1V = t1Itr.next();
			System.out.println("\t"+t1V);
		}
		System.exit(0);**/
		
		return(tier1ResultBufferList);
	}

	private Double edgeVarainceEvaluation(LinkedList<String> bufferLList) {
	
		ArrayList<Double> tier1BufferList = new ArrayList<>();
		Iterator<String> tier1Itr = bufferLList.iterator();
		while(tier1Itr.hasNext()){
			String tier1StringValue = tier1Itr.next();
			String[] groupInfo = tier1StringValue.split("-");
			int contextIndex = Integer.parseInt(groupInfo[0]);
			int clusterId = Integer.parseInt(groupInfo[1]);
			if(invariantClusters.containsKey(contextIndex-1)){
				if(invariantClusters.get(contextIndex-1).containsKey(clusterId)){
					Iterator<LinkedList<Double>> tier2Itr = invariantClusters
							.get(contextIndex-1).get(clusterId)
							.values().iterator();
					ArrayList<Double> tier2BufferList = new ArrayList<>();
					while(tier2Itr.hasNext()){
						LinkedList<Double> tier2LList = tier2Itr.next();
						tier2BufferList.add(tier2LList.stream()
								.reduce((x,y) -> x+y).get()/tier2LList.size());
					}
					tier1BufferList.add(tier2BufferList.stream()
							.reduce((x, y) -> x+y).get()/tier2BufferList.size());
				}
			}else{
				tier1BufferList.add(new Double(0).doubleValue());
			}
		}
		Double varianceScore = new Double(0).doubleValue(); 
		Collections.sort(tier1BufferList);
		for(int i=0;i<tier1BufferList.size()-1;i++){
			for(int j=i+1;j<tier1BufferList.size();j++){
				varianceScore = varianceScore + 
						(tier1BufferList.get(i)/tier1BufferList.get(j));
			}
		}
		
		return(varianceScore);
	}
	
	private LinkedList<Entry<LinkedList<String>, Double>> compareMapEntries(
			LinkedList<Entry<LinkedList<String>, Double>> linkedList, int order) {
		
		if(order == 1){
			Collections.sort(linkedList, new Comparator<Map.Entry<LinkedList<String>, Double>>() {
				// descending order
				@Override
				public int compare(Entry<LinkedList<String>, Double> currItem, 
						Entry<LinkedList<String>, Double> nextItem) {
					return (nextItem.getValue().compareTo(currItem.getValue()));
				}
			});
		}else if(order == 2){
			Collections.sort(linkedList, new Comparator<Map.Entry<LinkedList<String>, Double>>() {
				// ascending order
				@Override
				public int compare(Entry<LinkedList<String>, Double> currItem, 
						Entry<LinkedList<String>, Double> nextItem) {
					return (currItem.getValue().compareTo(nextItem.getValue()));
				}
			});
		}
		return(linkedList);
	}
	
	
	private ArrayList<LinkedList<Double>> retrieveInvariantContextVectors(
			LinkedList<String> bufferLList) {

		ArrayList<LinkedList<Double>> tier1ResultBufferList = new ArrayList<>();
		Iterator<String> tier1Itr = bufferLList.iterator();
		Double weight = new Double(0.0);
		while(tier1Itr.hasNext()){
			String tier1StringValue = tier1Itr.next();
			LinkedList<Double> tier1ResultBufferLList = new LinkedList<>();
			int clusterSize = 0;
			if(tier1StringValue.contains("-")){
				String[] groupInfo = tier1StringValue.split("\\-");
				int contextIndex = Integer.parseInt(groupInfo[0]);
				int clusterId = Integer.parseInt(groupInfo[1]);
				//int contextClusterSize = invariantClusters.get(contextIndex-1).size();
				if(invariantClusters.containsKey(contextIndex-1)){
					if(invariantClusters.get(contextIndex-1).containsKey(clusterId)){
						Iterator<LinkedList<Double>> tier2Itr = invariantClusters
								.get(contextIndex-1)
								.get(clusterId).values().iterator();
						while(tier2Itr.hasNext()){
							LinkedList<Double> tier2LList = tier2Itr.next();
							if(tier1ResultBufferLList.isEmpty()){
								tier1ResultBufferLList.addAll(tier2LList);
							}else{
								for(int i=0;i<tier1ResultBufferLList.size();i++){
									tier1ResultBufferLList.set(i,
											(tier1ResultBufferLList.get(i)+tier2LList.get(i)));
								}
							}
							clusterSize++;
						}
						
						Double customClusterWeight = (new Double(clusterId));
						customClusterWeight = (customClusterWeight * new Double(contextIndex));
						for(int i=0;i<tier1ResultBufferLList.size();i++){
							double customClusterScore = (tier1ResultBufferLList.get(i)/clusterSize);
							customClusterScore = customClusterScore*customClusterWeight;
							tier1ResultBufferLList.set(i,customClusterScore);
						}
					}else{
						tier1ResultBufferLList = new LinkedList<>(
								Collections.nCopies(windowDimension, 0.0));
					}
				}else{
					tier1ResultBufferLList = new LinkedList<>(
							Collections.nCopies(windowDimension, 0.0));
				}
				
				tier1ResultBufferList.add(tier1ResultBufferLList);
			}else{
				weight = Double.parseDouble(tier1StringValue);
				//System.out.println("\t weight>>"+weight);
			}
		}
		for(int i=0;i<tier1ResultBufferList.size();i++){
			LinkedList<Double> tier1BufferLList = tier1ResultBufferList.get(i);
			for(int j=0;j<tier1BufferLList.size();j++){
				if(weight !=0.0){
					tier1BufferLList.set(j, (tier1BufferLList.get(j)*weight));
				}else{
					System.err.println("\t weight marked zero "+weight);
					System.exit(0);
				}
			}
			tier1ResultBufferList.set(i, tier1BufferLList);
		}
		
		return(tier1ResultBufferList);
	}
	
	private ArrayList<LinkedList<Double>> retreiveOptimalContextEdgeFeature(
			List<Entry<LinkedList<String>, Double>> bufferListMap) {

		ArrayList<LinkedList<Double>> tier1ResultBufferList  = new ArrayList<>();
		ListIterator<Map.Entry<LinkedList<String>, Double>> tier1Itr = 
				bufferListMap.listIterator();
		while(tier1Itr.hasNext()){
			Map.Entry<LinkedList<String>, Double> tier1MapValue = tier1Itr.next();
			//System.out.println(">>>"+tier1MapValue.getKey()+"\t>>"+tier1MapValue.getValue());
			LinkedList<String> tier1BufferLList = tier1MapValue.getKey();
			if(!("1".equals(tier1BufferLList.get(tier1BufferLList.size()-1)))){
				tier1ResultBufferList.addAll( 
						retrieveInvariantContextVectors(tier1BufferLList));
				break;
			}
		}
		if(tier1ResultBufferList.isEmpty()){
			tier1ResultBufferList = retrieveInvariantContextVectors(
					bufferListMap.get(0).getKey());	
		}
		
		//System.out.println(">>>"+tier1ResultBufferList);
		
		return(tier1ResultBufferList);
	}
	
	private boolean if_FeatureOverlap(LinkedList<LinkedList<String>> commonFeatures,
			LinkedList<String> bufferLList) {

		boolean retVal = false;
		Iterator<LinkedList<String>> tier1Itr = commonFeatures.iterator();
		while(tier1Itr.hasNext()){
			LinkedList<String> tier1LList = tier1Itr.next();
			int count=0;
			for(int i=0;i<tier1LList.size();i++){
				if(tier1LList.get(i).matches(RegexUtility.patternBuilder(bufferLList.get(i)))){
					count++;
				}
			}
			if(count == tier1LList.size()){
				retVal = true;
				//System.out.println("\t>>"+tier1LList+"\t"+bufferLList);
				break;
			}
		}
		return(retVal);
	}

	private int getIconClusterStats(String bufferString, int index) {

		ArrayList<String> tier1BufferList = new ArrayList<>(
				Arrays.asList(bufferString.split("\\-")));
		return(Integer.parseInt(tier1BufferList.get(index)));
	}
	
	private ArrayList<LinkedList<String>> screenTargetCluster(ArrayList<LinkedList<String>> bufferList, 
			int listIndex, int templateIndex, String bufferString) {

		Integer clusterId = getIconClusterStats(bufferString, templateIndex);
		ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>(bufferList.stream()
				.filter((currList) -> clusterId == getIconClusterStats(
						currList.get(listIndex), templateIndex))
				.collect(Collectors.toList()));
		
		/**
		if((templateIndex == 1) && (tier1BufferList.isEmpty())){
			tier1BufferList = new ArrayList<>(bufferList);
		}**/
		
		/**
		Iterator<LinkedList<String>> t1Itr = tier1BufferList.iterator();
		while(t1Itr.hasNext()){
			System.out.println("\t"+t1Itr.next()+"\t"+bufferString+"\t"+listIndex);
		}**/
		
		return(tier1BufferList);
	}
	
	private LinkedList<Double> compileClusterInvarianceVector(Integer contextId, Integer clusterId) {

		//System.out.println("\t"+contextId+"\t"+clusterId);
		LinkedList<Double> tier1BufferResultLList = new LinkedList<>();
		boolean iconClusterNotPresent = true;
		if(invariantClusters.containsKey(contextId)){
			if(invariantClusters.get(contextId).containsKey(clusterId)){
				iconClusterNotPresent = false;
				Iterator<Map.Entry<LinkedList<String>, LinkedList<Double>>> tier1Itr = 
						invariantClusters.get(contextId).get(clusterId).entrySet().iterator();
				int clusterSize = invariantClusters.get(contextId).get(clusterId).size();
				while(tier1Itr.hasNext()){
					Map.Entry<LinkedList<String>, LinkedList<Double>> tier1MapValue = tier1Itr.next();
					if(tier1BufferResultLList.isEmpty()){
						tier1BufferResultLList = new LinkedList<>(tier1MapValue.getValue());
					}else{
						for(int i=0;i<tier1BufferResultLList.size();i++){
							tier1BufferResultLList.set(i, 
									(tier1BufferResultLList.get(i)+tier1MapValue.getValue().get(i)));
						}
					}
				}
				for(int j=0;j<tier1BufferResultLList.size();j++){
					tier1BufferResultLList.set(j,(tier1BufferResultLList.get(j)/clusterSize));
				}
			}
		}
		
		if(iconClusterNotPresent){
			tier1BufferResultLList = new LinkedList<>(
					Collections.nCopies(windowDimension, new Double(0).doubleValue()));
		}
		
		return(tier1BufferResultLList);
	}

	private double compareClusterInvariance(String targetString, String compareString, 
			LinkedList<String> posLList) {
		
		//System.out.println("\t"+targetString+"\t"+compareString+"\t"+posLList);
		Integer targetContextId = (getIconClusterStats(targetString, 0)-1);
		Integer targetClusterId = getIconClusterStats(targetString, 1);
		Integer compareContextId = (getIconClusterStats(compareString, 0)-1);
		Integer compareClusterId = getIconClusterStats(compareString, 1);
		double rmsError = 10000;
		if((targetContextId == compareContextId)){
			/**
			 * Compare using invariance deviation
			 */
			LinkedList<Double> targetLList = compileClusterInvarianceVector(
					targetContextId, targetClusterId);
			LinkedList<Double> compareLList = compileClusterInvarianceVector(
					compareContextId, compareClusterId);
			ArrayList<Double> tier1BufferList = new ArrayList<>();
			Double invarianceThreshold = new Double(1).doubleValue();
			
			for(int i=0;i<targetLList.size();i++){
				boolean variantEqual = false;
				Double invarianceConfidence = new Double(0).doubleValue();
				Double numVal = targetLList.get(i);
				Double denomVal = compareLList.get(i);
				int matchValue = Double.compare(numVal, denomVal);
				if(matchValue > 0){
					numVal = compareLList.get(i);
					denomVal = targetLList.get(i);
				}else if(matchValue == 0){
					invarianceConfidence = new Double(1).doubleValue();
					variantEqual = true;
				}
				if(!variantEqual){
					invarianceConfidence = (numVal/denomVal);
				}
				double invarianceDeviation = (invarianceThreshold - invarianceConfidence); 
				invarianceDeviation = Math.pow(invarianceDeviation, 2);
				tier1BufferList.add(invarianceDeviation);
			}
			if(!tier1BufferList.isEmpty()){
				rmsError = tier1BufferList.stream()
						.reduce((prev,curr)-> prev+curr).get().doubleValue();
				rmsError = Math.sqrt((rmsError/Integer.valueOf(tier1BufferList.size()).doubleValue()));
			}
			if(Double.compare(rmsError, new Double(0.0)) > 0){
				rmsError = new Double(3).doubleValue();
				//System.out.println("invar \t"+targetString+"\t>>"+compareString+"\t"+rmsError);
				//System.exit(0);
			}
			//System.out.println("invar \t"+targetString+"\t>>"+compareString+"\t"+rmsError);
		}
		if((targetContextId != compareContextId) || (Double.compare(rmsError, new Double(3).doubleValue()) == 0)){
			/**
			 * Compare using sequence alignment
			 */
			Double absoluteVariance = new Double(3).doubleValue();
			LinkedList<String> targetLList = posLList;
			LinkedList<String> compareLList = new LinkedList<>(
					Collections.nCopies(windowDimension, "@"));
			if(invariantClusters.containsKey(compareContextId)){
				if(invariantClusters.get(compareContextId).containsKey(compareClusterId)){
					Iterator<LinkedList<String>> tier1Itr = invariantClusters.get(compareContextId)
							.get(compareClusterId).keySet().iterator();
					LinkedList<Double> clusterVariance = new LinkedList<>();
					while(tier1Itr.hasNext()){
						compareLList = tier1Itr.next();
						Double pointwiseScore = new Double(0).doubleValue();
						for(int i=0;i<targetLList.size();i++){
							if(!targetLList.get(i).equals(compareLList.get(i))){
								pointwiseScore = pointwiseScore + new Double(1/3).doubleValue();
							}
						}
						Double threshold = new Double(2/3).doubleValue();
						int compVal = Double.compare(pointwiseScore, threshold);
						if(compVal < 0){
							clusterVariance.add(pointwiseScore);
						}else{
							clusterVariance.add(absoluteVariance);
						}
					}
					Collections.sort(clusterVariance);
					rmsError = clusterVariance.getFirst();
				}else{
					rmsError = absoluteVariance;
				}
			}else{
				rmsError = absoluteVariance;
			}
			//System.out.println("\t"+targetString+"\t"+targetLList+"\t"+compareString+"\t"+compareLList);
			//System.out.println("align \t"+targetLList+"\t"+compareLList+"\t"+rmsError);
		}
		if(rmsError == new Double(10000).doubleValue()){
			System.out.println("error rms");
			System.exit(0);
		}
		//System.out.println("\t>>"+compareString+"\t>>"+rmsError);
		return(rmsError);
	}
	
	

	private LinkedHashMap<LinkedList<String>, Double> retrieveOptimalCluster(
			LinkedList<String> bufferLList, 
			ArrayList<LinkedList<String>> bufferList, 
			LinkedHashMap<LinkedList<String>, Double> tier1BufferResultMap) {

		/**
		System.out.println("\t 1. \t"+bufferLList);
		System.out.println("\t 2. \t"+bufferList.size());
		Iterator<LinkedList<String>> itr = bufferList.iterator();
		while(itr.hasNext()){
			System.out.println("\t 2.1 \t"+itr.next());
			break;
		}
		System.out.println("\t 3. \t"+tier1BufferResultMap);**/
		LinkedHashMap<LinkedList<String>, Double> tier1BufferMap = new LinkedHashMap<>();
		Iterator<LinkedList<String>> tier1Itr = bufferList.iterator();
		while(tier1Itr.hasNext()){
			LinkedList<String> tier1LListValue = tier1Itr.next();
			Double invarianceErrorMeasure = new Double(0).doubleValue();
			for(int i=0;i<bufferLList.size();i++){
				String targetString = bufferLList.get(i);
				String compareString = tier1LListValue.get(i);
				//invarianceErrorMeasure = invarianceErrorMeasure + compareClusterInvariance(targetString, compareString);
			}
			tier1BufferMap.put(tier1LListValue, invarianceErrorMeasure);
		}
		List<Map.Entry<LinkedList<String>, Double>> tier1BufferListMap = 
				new LinkedList<>(tier1BufferMap.entrySet());
		if(tier1BufferResultMap.isEmpty()){
			//System.out.println("ordered");
			tier1BufferListMap = compareMapEntries(new LinkedList<>(tier1BufferMap.entrySet()), 2);
		}
		
		//System.out.println("\t>>>"+tier1BufferListMap);
		if(tier1BufferListMap.isEmpty()){
			System.err.println("Critical error retrieveOptimalCluster() "+bufferLList);
			System.exit(0);
		}else{
			int startIndex = 0;
			Entry<LinkedList<String>, Double> tier1BufferEntry = tier1BufferListMap.get(startIndex);
			LinkedHashMap<LinkedList<String>, Double> tier2BufferMap = new LinkedHashMap<>();
			if(startIndex+1 < tier1BufferListMap.size()){
				tier2BufferMap = new LinkedHashMap<>(
						tier1BufferListMap.subList(startIndex+1, tier1BufferListMap.size()).stream()
						.collect(Collectors.toMap((currList) -> currList.getKey(), 
								(currList) -> currList.getValue())));
			}
			
			boolean recursiveLoop = false;
			if(tier1BufferResultMap.isEmpty()){
				recursiveLoop = true;
			}else{
				if(tier1BufferEntry.getValue() < tier1BufferResultMap
						.entrySet().iterator().next().getValue()){
					tier1BufferResultMap = new LinkedHashMap<>();
					recursiveLoop = true;
				}
			}
			
			if(recursiveLoop){
				tier1BufferResultMap.put(
						tier1BufferEntry.getKey(), tier1BufferEntry.getValue());
				if(!tier2BufferMap.isEmpty()){
					retrieveOptimalCluster(tier1BufferEntry.getKey(), 
							new ArrayList<>(tier2BufferMap.keySet()), tier1BufferResultMap);
				}
			}
		}

		return(tier1BufferResultMap);
	}

	private Double featureValueOptimization(Double baseScore, double penalityCost) {

		baseScore = baseScore + (baseScore*penalityCost);
		return(baseScore);
	}
	
	private void adjustLearningWeights(Integer instanceType, Integer classType, 
			double reductionFactor) {
		
		//System.out.println("\t1. "+instanceType+"\t2. "+classType+"\t3. "+reductionFactor);
		//System.out.println("\t check "+baselineWeights+"\t again:::"+learnedWeights);
		Double bufferValue = new Double(0).doubleValue();
		if(baselineWeights.containsKey(classType)){
			if(classType != instanceType){
				Double factor = new Double(100).doubleValue();
				bufferValue = baselineWeights.get(classType)*factor;
				bufferValue = (bufferValue - reductionFactor)/factor;
			}else{
				Iterator<Map.Entry<Integer, Double>> tier1Itr = 
						learnedWeights.get(instanceType).entrySet().iterator();
				while(tier1Itr.hasNext()){
					Map.Entry<Integer, Double> tier1MapValue = tier1Itr.next();
					if(tier1MapValue.getKey() != classType){
						bufferValue = bufferValue + tier1MapValue.getValue();
					}
				}
				bufferValue = (new Double(1).doubleValue() - bufferValue);
			}
			//System.out.println("\t updated:::"+bufferValue);
		}
		
		boolean statusUpdate = false;
		TreeMap<Integer, Double> tier1LearnedWeights = new TreeMap<>();
		if(learnedWeights.containsKey(instanceType)){
			tier1LearnedWeights = learnedWeights.get(instanceType);
			//System.out.println("here 1:::"+tier1LearnedWeights);
			if(tier1LearnedWeights.containsKey(classType)){
				// compare the previous values
				if(classType != instanceType){
					//System.out.println("here 2:::"+bufferValue);
					if((tier1LearnedWeights.get(classType) > bufferValue) 
							|| (tier1LearnedWeights.get(classType) == new Double(0).doubleValue())){
						//System.out.println("here 3:::"+bufferValue);
						statusUpdate = true;
						tier1LearnedWeights.put(classType, bufferValue);
					}
				}else{
					//System.out.println("here 4:::"+bufferValue);
					if((tier1LearnedWeights.get(classType) < bufferValue) 
							|| (tier1LearnedWeights.get(classType) == new Double(0.0))){
						tier1LearnedWeights.put(classType, bufferValue);
						statusUpdate = true;
					}
				}
			}else{
				// put the default calculated values
				statusUpdate = true;
				tier1LearnedWeights.put(classType, bufferValue);
			}
			
		}else{
			statusUpdate = true;
			tier1LearnedWeights.put(classType, bufferValue);
		}
		if(statusUpdate){
			System.out.println("\t update >>"+instanceType+"\t\t"+tier1LearnedWeights);
		}
		learnedWeights.put(instanceType, tier1LearnedWeights);
	}
	
	private LinkedList<Double> approximateIconEdgeFeatureScore(LinkedList<String> bufferLList,
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferResultMap, 
			Integer instanceType) {

		System.out.println("\t current>>"+bufferLList);
		LinkedList<Double> tier1BufferResultLList = new LinkedList<>();
		TreeMap<Integer, Double> tier0BufferResultMap = new TreeMap<>();
		TreeMap<Integer, Double> tier1BufferResultMap = new TreeMap<>();
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier1Itr = 
				bufferResultMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			double penalityCost = new Double(0);
			double featureValueScore = new Double(0);
			double invarScore = new Double(0);
			boolean incompleteMatch = false;
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1MapValue = 
					tier1Itr.next();
			System.out.println("\t currInstance>>>"+tier1MapValue.getKey());
			LinkedHashMap<LinkedList<String>, Double> tier1BufferMap = new LinkedHashMap<>();
			LinkedHashMap<LinkedList<String>, Double> tier0BufferMap = tier1MapValue.getValue();
			if(tier0BufferMap.containsKey(bufferLList)){
				featureValueScore = featureValueOptimization(
						tier0BufferMap.get(bufferLList), penalityCost);
				tier0BufferResultMap.put(tier1MapValue.getKey(), invarScore);
				tier1BufferResultMap.put(tier1MapValue.getKey(), featureValueScore);
			}else{
				incompleteMatch = true;
				ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>(tier0BufferMap.keySet());
				int indexScore = 0;
				HashSet<LinkedList<String>> tier1BufferSet = new HashSet<>();
				for(int i=0;i<bufferLList.size();i++){
					String tier1BufferString = bufferLList.get(i);
					ArrayList<LinkedList<String>> tier2BufferList =  new ArrayList<>();
					for(int j=0;j<2;j++){
						/**
						 * Identify target sequences starting with the same sequence 
						 * as in selected feature set
						 */
						tier2BufferList = screenTargetCluster(tier1BufferList, i, j, tier1BufferString);
						if(!tier2BufferList.isEmpty()){
							tier1BufferList = new ArrayList<>(tier2BufferList);
						}else{
							break;
						}
					}
					if(!tier2BufferList.isEmpty()){
						tier1BufferSet.addAll(tier2BufferList);
						indexScore++;
					}
				}
				//System.exit(0);
				if(indexScore > 0){
					tier1BufferMap = retrieveOptimalCluster(
							bufferLList, new ArrayList<>(tier1BufferSet), tier1BufferMap);
				}else{
					System.out.println("\t whole cluster bufferSet >> "+tier1BufferSet);
					/**
					System.exit(0);
					tier1BufferMap= retrieveOptimalCluster(bufferLList, 
							new ArrayList<>(tier0BufferMap.keySet()), tier1BufferMap);
							**/
				}
			}
			if(incompleteMatch && (!tier1BufferMap.isEmpty())){
				Iterator<Map.Entry<LinkedList<String>, Double>> tier2Itr = 
						tier1BufferMap.entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<LinkedList<String>, Double> tier2MapValue = tier2Itr.next();
					if(tier0BufferMap.containsKey(tier2MapValue.getKey())){
						penalityCost = tier2MapValue.getValue();
						penalityCost = new Double(0).doubleValue();
						featureValueScore = featureValueOptimization(
								tier0BufferMap.get(tier2MapValue.getKey()), penalityCost);
						invarScore = tier2MapValue.getValue();
						break;
					}
				}
				tier0BufferResultMap.put(tier1MapValue.getKey(), invarScore);
				tier1BufferResultMap.put(tier1MapValue.getKey(), featureValueScore);
			}
			//System.out.println("\t"+tier1MapValue.getKey()+"\t"+invarScore+"\t"+featureValueScore);
		}
		/**
		 * Compare the feature variance
		 */
		if(instanceType != 0){
			Double invarianceThreshold = tier0BufferResultMap.get(instanceType);
			
			if(invarianceThreshold != new Double(0).doubleValue()){
				TreeMap<Integer, Double> tier2BufferResultMap = new TreeMap<>();
				Iterator<Map.Entry<Integer, Double>> tier3Itr = 
						tier0BufferResultMap.entrySet().iterator();
				while(tier3Itr.hasNext()){
					Map.Entry<Integer, Double> tier3MapValue = tier3Itr.next();
					if(tier3MapValue.getKey() != instanceType){
						if(tier3MapValue.getValue() < invarianceThreshold){
							tier2BufferResultMap.put(tier3MapValue.getKey(), 
									(invarianceThreshold-tier3MapValue.getValue()));
						}
					}
				}
				
				Iterator<Integer> tier4Itr = tier0BufferResultMap.keySet().iterator();
				double reductionFactor = new Double(0);
				while(tier4Itr.hasNext()){
					Integer classType = tier4Itr.next();
					reductionFactor = new Double(0);
					// weight update for other than "instanceType" class
					if(classType != instanceType){
						if(!tier2BufferResultMap.isEmpty()){
							if(tier2BufferResultMap.containsKey(classType)){
								reductionFactor = tier2BufferResultMap.get(classType);
							}
						}
						adjustLearningWeights(instanceType, classType, reductionFactor);
					}
				}
				// same class weight update
				reductionFactor = new Double(0);
				adjustLearningWeights(instanceType, instanceType, reductionFactor);
			}
			
			Iterator<Map.Entry<Integer, Double>> tier5Itr = null;
			TreeMap<Integer, Double> tier4BufferMap = new TreeMap<>();
			if(invarianceThreshold == new Double(0).doubleValue()){
				Iterator<Integer> tier6Itr = tier0BufferResultMap.keySet().iterator();
				while(tier6Itr.hasNext()){
					Integer currInstance = tier6Itr.next();
					if(currInstance == instanceType){
						tier4BufferMap.put(currInstance, new Double(1).doubleValue());
					}else{
						tier4BufferMap.put(currInstance, new Double(0).doubleValue());
					}
				}
				tier5Itr = tier4BufferMap.entrySet().iterator();
				System.out.println("\t1. >>"+tier4BufferMap);
			}else{
				if(tier0BufferResultMap.size() == learnedWeights.get(instanceType).size()){
					tier5Itr = learnedWeights.get(instanceType).entrySet().iterator();
					System.out.println("\t2. >>"+learnedWeights.get(instanceType));
				}else{
					Iterator<Integer> tier6Itr = tier0BufferResultMap.keySet().iterator();
					while(tier6Itr.hasNext()){
						Integer currInstance = tier6Itr.next();
						if(currInstance == instanceType){
							tier4BufferMap.put(currInstance, new Double(1).doubleValue());
						}else{
							tier4BufferMap.put(currInstance, new Double(0).doubleValue());
						}
					}
					tier5Itr = tier4BufferMap.entrySet().iterator();
					System.out.println("\t3. >>"+tier4BufferMap);
				}
			}
			
			Double weightedScore = new Double(0);
			while(tier5Itr.hasNext()){
				Map.Entry<Integer, Double> tier5MapValue = tier5Itr.next();
				weightedScore = weightedScore + 
						(tier1BufferResultMap.get(tier5MapValue.getKey())*tier5MapValue.getValue());
				
			}
			tier1BufferResultLList.add(weightedScore);
		}else{
			//System.out.println("\t final ::"+finalWeights);
			
			TreeMap<Integer, Double> tier3BufferMap = new TreeMap<>();
			Double invarianceThreshold = tier0BufferResultMap.firstEntry().getValue();
			Iterator<Map.Entry<Integer, Double>> tier3Itr = 
					tier0BufferResultMap.entrySet().iterator();
			while(tier3Itr.hasNext()){
				Map.Entry<Integer, Double> tier3MapValue = tier3Itr.next();
				if(tier3MapValue.getValue() >= invarianceThreshold){
					invarianceThreshold = tier3MapValue.getValue();
				}
				if(tier3MapValue.getValue() == new Double(0).doubleValue()){
					invarianceThreshold = tier3MapValue.getValue();
					tier3BufferMap.put(tier3MapValue.getKey(), new Double(1).doubleValue());
					break;
				}
			}
			//System.out.println(">>>>"+invarianceThreshold);
			
			TreeMap<Integer, Double> tier2BufferResultMap = new TreeMap<>();
			tier3Itr = tier0BufferResultMap.entrySet().iterator();
			while(tier3Itr.hasNext()){
				Map.Entry<Integer, Double> tier3MapValue = tier3Itr.next();
				if(tier3MapValue.getValue() != invarianceThreshold){
					if(invarianceThreshold == new Double(0).doubleValue()){
						tier3BufferMap.put(tier3MapValue.getKey(), new Double(0).doubleValue());
					}else{
						tier2BufferResultMap.put(tier3MapValue.getKey(), 
								(invarianceThreshold - tier3MapValue.getValue()));
					}
				}
			}
			//System.out.println("\ttier2>>"+tier2BufferResultMap);
			Iterator<Map.Entry<Integer, Double>> tier5Itr = 
					finalWeights.entrySet().iterator();
			if(!tier2BufferResultMap.isEmpty()){
				Integer invarianceThresholdClass = 0;
				Double reductionScore = new Double(0).doubleValue();
				while(tier5Itr.hasNext()){
					Map.Entry<Integer, Double> tier5MapValue = tier5Itr.next();
					Double factor = new Double(100).doubleValue();
					Double bufferValue = new Double(0).doubleValue();
					if(tier2BufferResultMap.containsKey(tier5MapValue.getKey())){
						double reductionFactor = tier2BufferResultMap.get(tier5MapValue.getKey());
						bufferValue = tier5MapValue.getValue()*factor;
						bufferValue = (bufferValue+reductionFactor)/factor;
						reductionScore = reductionScore+bufferValue;
						tier3BufferMap.put(tier5MapValue.getKey(), bufferValue);
					}else{
						invarianceThresholdClass = tier5MapValue.getKey();
					}
				}
				if(invarianceThresholdClass != 0){
					tier3BufferMap.put(invarianceThresholdClass, 
							(new Double(1).doubleValue()-reductionScore));
				}
			}
			

			/**
			Double difValue = Math.abs((reductionScore-tier3BufferMap.get(invarianceThresholdClass))) 
					* (new Double(100).doubleValue());
			if(difValue.doubleValue() <= new Double(5).doubleValue()){
				Double leastMargin = new Double(0.4).doubleValue();
				Iterator<Map.Entry<Integer, Double>> tier6Itr = 
						tier3BufferMap.entrySet().iterator();
				while(tier6Itr.hasNext()){
					Map.Entry<Integer, Double> tier6MapValue = tier6Itr.next();
					if(tier6MapValue.getKey() == invarianceThresholdClass){
						tier3BufferMap.put(invarianceThresholdClass, leastMargin);
					}else{
						Double updatedScore = ((new Double(1.0)-leastMargin) /
								new Double(tier3BufferMap.size()-1).doubleValue());
						tier3BufferMap.put(tier6MapValue.getKey(), updatedScore);
					}
				}
				//System.out.println("\t proximity >>"+difValue+"\t"+tier3BufferMap);
			}**/
			
			System.out.println("\t test updated>>"+tier3BufferMap);
			
			tier5Itr = tier3BufferMap.entrySet().iterator();
			Double weightedScore = new Double(0);
			while(tier5Itr.hasNext()){
				Map.Entry<Integer, Double> tier5MapValue = tier5Itr.next();
				weightedScore = weightedScore + 
						(tier1BufferResultMap.get(tier5MapValue.getKey())*tier5MapValue.getValue());
			}
			tier1BufferResultLList.add(weightedScore);
		}
		
		return(tier1BufferResultLList);
	}
	

	public TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> 
	generateEdgeBasedContextFeatures( 
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferResultMap, 
			TreeMap<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>> bufferMap, 
			String modelPhase) {

		int sentCount = 1;
		TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> tier1ResultBufferMap = 
				new TreeMap<>();
		Iterator<Map.Entry<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			HashMap<String, ArrayList<LinkedList<Double>>> tier2ResultBufferMap = 
					new HashMap<>();
			Map.Entry<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>> tier1MapValue = 
					tier1Itr.next();
			Integer instanceType = tier1MapValue.getKey();
			Iterator<Map.Entry<String, ArrayList<LinkedList<String>>>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<String, ArrayList<LinkedList<String>>> tier2MapValue = tier2Itr.next();
				//if(tier2MapValue.getKey().equals("AIMed_d30@4#1")){
				System.out.println("\n\t"+tier1MapValue.getKey()+"\t"+tier2MapValue.getKey()+"\t"+sentCount);
				sentCount++;
				LinkedHashMap<LinkedList<String>, Double> tier1BufferMap = new LinkedHashMap<>();
				HashSet<LinkedList<String>> tier2LList = new HashSet<>(tier2MapValue.getValue());
				tier1BufferMap = new LinkedHashMap<>(tier2LList.stream()
						.collect(Collectors.toMap(currList -> currList, 
								currList -> edgeVarainceEvaluation(currList))));
				System.out.println("\t>>"+tier1BufferMap);
				List<Map.Entry<LinkedList<String>, Double>> tier1BufferListMap =
						compareMapEntries(new LinkedList<>(tier1BufferMap.entrySet()), 2);
				int index = 0; 
				ArrayList<LinkedList<Double>> tier4LList =  new ArrayList<>();
				Iterator<Map.Entry<LinkedList<String>, Double>> tier31Itr = 
						tier1BufferListMap.iterator();
				while(tier31Itr.hasNext()){
					Map.Entry<LinkedList<String>, Double> tier31MapValue = tier31Itr.next();
					//System.out.println("\t"+tier31MapValue.getKey());
					if(modelPhase.equals("test")){
						instanceType = 0;
					}
					tier4LList.add(
							approximateIconEdgeFeatureScore(tier31MapValue.getKey(), 
									bufferResultMap, instanceType));
					/**
					if(bufferResultMap.containsKey(tier31MapValue.getKey())){
						tier4LList.add(new LinkedList<>(
								Arrays.asList(bufferResultMap.get(tier31MapValue.getKey()))));
						System.out.println("val\t"+bufferResultMap.get(tier31MapValue.getKey()));
					}**/
					index++;
					if(index == (2*windowDimension)){
						break;
					}
				}
				if(tier4LList.size() != (windowDimension*2)){
					System.out.println("\t window size"+tier4LList);
					System.exit(0);
				}
				//retreiveOptimalContextEdgeFeature(tier1BufferListMap);
				//System.out.println("\t\t"+tier4LList);
				if(tier2ResultBufferMap.containsKey(tier2MapValue.getKey())){
					System.err.println("repeat>>"+tier2MapValue.getKey());
				}
				tier2ResultBufferMap.put(tier2MapValue.getKey(), tier4LList);
				//}
			}
			tier1ResultBufferMap.put(tier1MapValue.getKey(), tier2ResultBufferMap);
		}
		if(modelPhase.equals("training")){
			Iterator<Map.Entry<Integer, TreeMap<Integer, Double>>> tier4Itr = 
					learnedWeights.entrySet().iterator();
			while(tier4Itr.hasNext()){
				Map.Entry<Integer, TreeMap<Integer, Double>> tier4MapValue = tier4Itr.next();
				Iterator<Map.Entry<Integer, Double>> tier5Itr = 
						tier4MapValue.getValue().entrySet().iterator();
				while(tier5Itr.hasNext()){
					Map.Entry<Integer, Double> tier5MapValue = tier5Itr.next();
					Double currScore = new Double(0);
					if(finalWeights.containsKey(tier5MapValue.getKey())){
						currScore = finalWeights.get(tier5MapValue.getKey());
					}
					currScore = currScore + tier5MapValue.getValue();
					finalWeights.put(tier5MapValue.getKey(), currScore);
				}
			}
			finalWeights = new TreeMap<>(finalWeights.entrySet().stream()
					.collect(Collectors.toMap(currVal -> currVal.getKey(), 
							currVal -> currVal.getValue()/learnedWeights.size())));
			System.out.println(" final weights>>"+finalWeights);
		}
		return(tier1ResultBufferMap);
	}

	public TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> 
	addTermBasedVariantFeatures(
			TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> bufferResultMap,
			TreeMap<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>> bufferMap) {

		Iterator<Map.Entry<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>> tier1MapValue = 
					tier1Itr.next();
			HashMap<String, ArrayList<LinkedList<Double>>> tier1ResultBufferMap = 
					bufferResultMap.get(tier1MapValue.getKey());
			Iterator<Map.Entry<String, ArrayList<LinkedList<String>>>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<String, ArrayList<LinkedList<String>>> tier2MapValue = 
						tier2Itr.next();
				ArrayList<LinkedList<Double>> tier1ResultBufferList = 
						tier1ResultBufferMap.get(tier2MapValue.getKey());
				LinkedList<Double> tier1BufferLList  = new LinkedList<>();
				Iterator<LinkedList<String>> tier3Itr = tier2MapValue.getValue().iterator();
				int variantSize = 0;
				while(tier3Itr.hasNext()){
					ArrayList<LinkedList<Double>> tier2BufferList  = new ArrayList<>();
					LinkedList<String> tier3LList = tier3Itr.next();
					// add weight (default)
					tier3LList.add("1");
					tier2BufferList = retrieveInvariantContextVectors(tier3LList);
					Iterator<LinkedList<Double>> tier4Itr = tier2BufferList.iterator();
					while(tier4Itr.hasNext()){
						LinkedList<Double> tier4LList = tier4Itr.next();
						/**
						 * add respective weights together to generate new differential
						 */
						if(tier1BufferLList.isEmpty()){
							tier1BufferLList.addAll(tier4LList);
							break;
						}else{
							for(int i=0;i<tier4LList.size();i++){
								double indexValue = tier1BufferLList.get(i)+tier4LList.get(i);
								tier1BufferLList.set(i, indexValue);
							}
						}
						variantSize++;
					}
				}
				/**
				for(int i=0;i<tier1BufferLList.size();i++){
					double bufferValue = tier1BufferLList.get(i);
					if(tier1BufferLList.get(i) != 0){
						//bufferValue = Math.log(bufferValue);
						bufferValue = (bufferValue/variantSize);
						tier1BufferLList.set(i, bufferValue);
					}
				}**/
				//double sumVal = tier1BufferLList.stream().reduce((x,y) -> (x+y)).get();
				tier1ResultBufferList.add(tier1BufferLList);
				//tier1ResultBufferList.add(new LinkedList<>(Arrays.asList(sumVal)));
				tier1ResultBufferMap.put(tier2MapValue.getKey(), tier1ResultBufferList);
			}
			bufferResultMap.put(tier1MapValue.getKey(), tier1ResultBufferMap);
		}
		return bufferResultMap;
	}
	
	private LinkedList<LinkedList<String>> extractPairedCoRefContexts(
			LinkedList<String> bufferLList, 
			TreeMap<Integer, ArrayList<Integer>> bufferMap) {
		
		LinkedList<LinkedList<String>> featureBufferLList = new LinkedList<>();
		
		int coRefRangeLimit = 3;//windowDimension-1; 
		HashSet<LinkedList<Integer>> tier1BufferSet = new HashSet<>();
		ArrayList<Integer> tier1BufferList = new ArrayList<>(bufferMap.keySet());
		Iterator<Map.Entry<Integer, ArrayList<Integer>>> tier1Itr = bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<Integer>> tier1MapValue = tier1Itr.next();
			Iterator<Integer> tier2Itr = tier1MapValue.getValue().iterator();
			while(tier2Itr.hasNext()){
				Integer tier2IntegerValue = tier2Itr.next();
				LinkedList<Integer> tier1BufferLList = new LinkedList<>();
				//if(!tier1BufferList.contains(tier2IntegerValue)){
				if(tier2IntegerValue != tier1MapValue.getKey()){
					tier1BufferLList.add(tier2IntegerValue);
					//tier1BufferLList.add(tier1MapValue.getKey());
					tier1BufferLList.addAll(tier1BufferList);
					Collections.sort(tier1BufferLList);
				}else{
					//tier1BufferLList.add(tier1MapValue.getKey());
				}
				//System.out.println(">pair>"+tier1BufferLList);
				//if(!tier1BufferSet.contains(tier1BufferLList)){
					//tier1BufferSet.add(tier1BufferLList);
					
					LinkedList<String> tier2BufferLList = new LinkedList<>();
					if(tier1BufferLList.size() == coRefRangeLimit){
						Iterator<Integer> tier3Itr = tier1BufferLList.iterator();
						while(tier3Itr.hasNext()){
							Integer tier3IntegerValue = tier3Itr.next();
							if((tier3IntegerValue < 0) || (tier3IntegerValue >= bufferLList.size())){
								tier2BufferLList.add("0-0");
							}else{
								tier2BufferLList.add(bufferLList.get(tier3IntegerValue));
							}
						}
					}else if(!tier1BufferLList.isEmpty()){
						System.err.println("critical error in pairing ~ extractPairedCoRefContexts()");
					}
					if(!tier2BufferLList.isEmpty()){
						featureBufferLList.add(tier2BufferLList);
					}
				//}
				
			}
		}
		
		return(featureBufferLList);
	}
	
	private LinkedList<LinkedList<String>> findTriggerFeatureBlocks(
			LinkedList<String> bufferLList, int corefHopSize) {
		
		ArrayList<Integer> triggerSelectRange = (ArrayList<Integer>) 
				IntStream.range(1, windowDimension+1).boxed().collect(Collectors.toList());
		
		LinkedList<LinkedList<String>> featureBufferLList = new LinkedList<>();
		TreeMap<Integer, ArrayList<Integer>> tier1BufferMap = new TreeMap<>();
		int triggerIndex=0;
		Iterator<String> tier1Itr = bufferLList.iterator();
		while(tier1Itr.hasNext()){
			Integer tier1BufferInteger = Integer.parseInt(tier1Itr.next().split("\\-")[0]);
			if(triggerSelectRange.contains(tier1BufferInteger)){
				int startHop = (triggerIndex-corefHopSize);
				/**
				if(startHop < 0){
					startHop = 0;
				}**/
				int endHop = (triggerIndex+(corefHopSize+1));
				/**
				if(endHop > bufferLList.size()){
					endHop = bufferLList.size();
				}**/
				ArrayList<Integer> tier1BufferList = (ArrayList<Integer>) 
						IntStream.range(startHop, endHop).boxed().collect(Collectors.toList());
				//System.out.println("\t"+triggerIndex+"\t"+tier1BufferList);
				tier1BufferMap.put(triggerIndex, tier1BufferList);
			}
			triggerIndex++;
		}
		if(tier1BufferMap.size() != 2){
			System.err.println("Critical error ~ findTriggerFeatureBlocks()"+bufferLList);
			System.exit(0);
		}else{
			featureBufferLList = extractPairedCoRefContexts(bufferLList, tier1BufferMap);
		}
		
		return(featureBufferLList);
	}

	private Integer variantContextPosition(LinkedList<String> bufferLList) {

		Integer variantSize = bufferLList.stream().filter((currVal) -> currVal.equals("0-0"))
				.collect(Collectors.toList()).size();
		variantSize = bufferLList.size()-variantSize;
		return (variantSize);
	}
	
	private HashSet<LinkedList<String>> screenForRelevantCluster(
			ArrayList<LinkedList<String>> bufferList,
			LinkedList<String> bufferLList) {

		HashSet<LinkedList<String>> tier1BufferResultSet = new HashSet<>();
		/**
		for(int i=0;i<bufferLList.size();i++){
			if(!bufferLList.get(i).equals("0-0")){
				int currVar = i;
				String currentFeature = bufferLList.get(i);
				tier1BufferResultSet.addAll(bufferList.stream()
						.filter((currList) -> currList.get(currVar).equals(currentFeature))
						.collect(Collectors.toList()));
			}
		}**/
		
		if(tier1BufferResultSet.isEmpty()){
			for(int i=0;i<bufferLList.size();i++){
				if(!bufferLList.get(i).equals("0-0")){
					int currVar = i;
					int contextId = getIconClusterStats(bufferLList.get(i), 0);
					tier1BufferResultSet.addAll(bufferList.stream()
							.filter((currList) -> contextId == getIconClusterStats(currList.get(currVar), 0))
							.collect(Collectors.toList()));
					/**
					tier1BufferResultSet.addAll(bufferList.stream()
							.map((currList) -> currList)
							.collect(Collectors.toList()));**/
				}
			}
		}
		
		return(tier1BufferResultSet);
	}
	
	private LinkedList<String> callGeniaTagger(String abstractString) {

		LinkedList<String> posTagList = new LinkedList<>();
		JeniaTagger.setModelsPath(systemProperties.getProperty("geniaModelFile"));
		Sentence baseForm = JeniaTagger.analyzeAll(abstractString, true);
		Iterator<Token> tokenItr = baseForm.iterator();
		while(tokenItr.hasNext()){
			Token currentToken = tokenItr.next();
			Matcher baseFormMatcher = Pattern.compile("TRIGGERPRI").matcher(currentToken.baseForm);
			if (baseFormMatcher.find()){
				currentToken.baseForm = "TRIGGERPRI";
				currentToken.pos=currentToken.baseForm;
			}
			posTagList.add(currentToken.pos);
		}
		return (posTagList);
	}
	
	private TreeMap<Integer, Double> optimalTemplateClass(
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferMap,
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferVariantMap) {
		
		Double selectionThreshold = new Double(100000);
		Integer selectedClass = 0;
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1MapValue = 
					tier1Itr.next();
			Double tier1DoubleValue = tier1MapValue.getValue().entrySet().iterator().next().getValue();
			int compVal = Double.compare(tier1DoubleValue, selectionThreshold);
			if(compVal < 0){
				selectionThreshold = tier1DoubleValue;
				selectedClass = tier1MapValue.getKey();
			}else if(compVal == 0){
				selectedClass = 2;
			}
		}

		TreeMap<Integer, Double> tier1BufferMap = new TreeMap<>();
		Double selectedScore = new Double(0).doubleValue();
		if(selectedClass == 2){
			Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier2Itr = 
					bufferMap.entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier2MapValue = 
						tier2Itr.next();
				Entry<LinkedList<String>, Double> tier3MapValue = 
						tier2MapValue.getValue().entrySet().iterator().next();
				if(bufferVariantMap.get(tier2MapValue.getKey()).containsKey(tier3MapValue.getKey())){
					selectedScore = selectedScore + bufferVariantMap
							.get(tier2MapValue.getKey()).get(tier3MapValue.getKey());
					//System.out.println("\t>>"+selectedScore);
				}
			}
			selectedScore = selectedScore/new Double(bufferMap.size());
		}else{
			Entry<LinkedList<String>, Double> tier3MapValue = 
					bufferMap.get(selectedClass).entrySet().iterator().next();
			if(bufferVariantMap.get(selectedClass).containsKey(tier3MapValue.getKey())){
				selectedScore = selectedScore + bufferVariantMap
						.get(selectedClass).get(tier3MapValue.getKey());
				//System.out.println("\t>>"+selectedScore);
			}
		}
		tier1BufferMap.put(selectedClass, selectedScore);
		//System.out.println("\t"+selectedScore+"\t"+selectedClass);
		
		return(tier1BufferMap);
	}
	
	private TreeMap<Integer, Double> sigmoidVariantScoring(ArrayList<Integer> bufferList) {

		Double totalScore = new Double(0).doubleValue();
		TreeMap<Integer, Double> tier1BufferMap = new TreeMap<>();
		for(int i=0;i<bufferList.size();i++){
			Double currScore = (Double) (1/(1+Math.exp(bufferList.get(i)*(-1))));
			tier1BufferMap.put(bufferList.get(i), currScore);
			totalScore = totalScore+currScore;
		}
		/**
		Double bufferScore = totalScore;
		tier1BufferMap = new TreeMap<>(tier1BufferMap.entrySet().stream()
				.collect(Collectors.toMap(currVal->currVal.getKey(), 
						currVal -> currVal.getValue()/bufferScore)));**/
		
		//System.out.println("\t"+tier1BufferMap);
		return(tier1BufferMap);
		
	}
	
	
	private TreeMap<Integer, Double> softmaxVariantScoring(ArrayList<Integer> bufferList) {

		Double totalScore = new Double(0).doubleValue();
		TreeMap<Integer, Double> tier1BufferMap = new TreeMap<>();
		for(int i=0;i<bufferList.size();i++){
			Double currScore = (Double) (Math.exp(bufferList.get(i)));
			tier1BufferMap.put(bufferList.get(i), currScore);
			totalScore = totalScore+currScore;
		}
		Double bufferScore = totalScore;
		tier1BufferMap = new TreeMap<>(tier1BufferMap.entrySet().stream()
				.collect(Collectors.toMap(currVal->currVal.getKey(), 
						currVal -> currVal.getValue()/bufferScore)));
		
		//System.out.println("\t"+tier1BufferMap);
		return(tier1BufferMap);
		
	}
	
	private LinkedList<Double> screenMaxValue(ArrayList<TreeMap<Integer, Double>> bufferList) {

		Iterator<TreeMap<Integer, Double>> tier1Itr = 
				bufferList.iterator();
		TreeMap<Integer, Integer> instanceCount = new TreeMap<>();
		while(tier1Itr.hasNext()){
			Integer tier1IntegerValue = tier1Itr.next().firstKey();
			int count = 0;
			if(instanceCount.containsKey(tier1IntegerValue)){
				count = instanceCount.get(tier1IntegerValue);
			}
			count++;
			instanceCount.put(tier1IntegerValue, count);
		}
		
		List<Map.Entry<Integer, Integer>> tier1BufferListMap =  
				new LinkedList<>(instanceCount.entrySet());
		Collections.sort(tier1BufferListMap, new Comparator<Map.Entry<Integer, Integer>>() {
			// descending order
			@Override
			public int compare(Entry<Integer, Integer> currItem, 
					Entry<Integer, Integer> nextItem) {
				return (nextItem.getValue().compareTo(currItem.getValue()));
			}
		});
		
		
		int classKey = tier1BufferListMap.get(0).getKey();
		if(classKey == 2 && tier1BufferListMap.get(0).getValue()==2){
			classKey = tier1BufferListMap.get(1).getKey();
		}
		ArrayList<Double> collectionArray = new ArrayList<>();
		tier1Itr = bufferList.iterator();
		while(tier1Itr.hasNext()){
			TreeMap<Integer, Double> tier1MapValue = tier1Itr.next();
			if(classKey == 2){
				collectionArray.add(tier1MapValue.firstEntry().getValue());
			}else{
				if(tier1MapValue.firstKey() == classKey){
					collectionArray.add(tier1MapValue.firstEntry().getValue());
				}
			}
		}
		LinkedList<Double> returnArray = new LinkedList<>();
		returnArray.add(Collections.max(collectionArray));
		
		/**
		Iterator<TreeMap<Integer, Double>> tier1Itr = 
				bufferList.iterator();
		ArrayList<Double> collectionArray = new ArrayList<>();
		while(tier1Itr.hasNext()){
			collectionArray.add(tier1Itr.next().firstEntry().getValue());
		}
		Collections.sort(collectionArray);
		LinkedList<Double> returnArray = new LinkedList<>();
		returnArray.addAll(collectionArray.subList(0, 2));**/
		
		return (returnArray);
	}
	
	private LinkedList<Double> maximizedWeight(
			ArrayList<TreeMap<Integer, Double>> selectedScreenList) {

		LinkedList<Double> tier1BufferLList = new LinkedList<>();
		
		Iterator<TreeMap<Integer, Double>> tier1Itr = selectedScreenList.iterator();
		while(tier1Itr.hasNext()){
			tier1BufferLList.add(tier1Itr.next().firstEntry().getValue());
		}
		
		/**
		int midPoint = selectedScreenList.size()/2;
		ArrayList<TreeMap<Integer, Double>> forwardList = new ArrayList<>(
				selectedScreenList.subList(0, midPoint));
		tier1BufferLList.addAll(screenMaxValue(forwardList));
		ArrayList<TreeMap<Integer, Double>> backwardList = new ArrayList<>(
				selectedScreenList.subList(midPoint, selectedScreenList.size()));
		tier1BufferLList.addAll(screenMaxValue(backwardList));
		**/
		return(tier1BufferLList);
	}
	
	private ArrayList<LinkedList<String>> learnPhrase(LinkedList<String> bufferLList) {

		int phraseWindow = 2*windowDimension;
		ArrayList<LinkedList<String>> phraseLList = new ArrayList<>();
		for(int i=0;i<bufferLList.size();){
			int startIndex = i, endIndex=(i+phraseWindow);
			//System.out.println(">>>"+bufferLList.size()+"\t"+startIndex+"\t"+endIndex);
			if(endIndex <= bufferLList.size()){
				//System.out.println(">>>"+bufferLList.size());
				LinkedList<String> tempLList = new LinkedList<>(
						bufferLList.subList(startIndex, endIndex));
				for(int j=0;j<(tempLList.size()-windowDimension);j++){
					int pStart = j,pEnd=j+windowDimension;
					phraseLList.add(new LinkedList<>(tempLList.subList(pStart, pEnd)));
				}
			}
			i = endIndex;
		}
		if(phraseLList.isEmpty()){
			System.err.println("crtitical error in phrase generation ~ learnPhrase()");
			System.exit(0);
		}

		return phraseLList;
	}

	private LinkedList<Double> getRelevantInvariantAttribute(
			LinkedList<String> bufferPhrase, int contextIndex) {

		LinkedList<Double> tier1BufferResultList = new LinkedList<>();
		if(contextIndex != -1){
			for(int i=0;i<bufferPhrase.size();i++){
				if((bufferPhrase.get(i).equals(normTriggerTerm)) && (contextIndex!=i)){
					bufferPhrase.set(i, "PROTEINT");
				}
			}
			Iterator<Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> tier1Itr = 
					invariantClusters.get(contextIndex).entrySet().iterator();
			TreeMap<Double, LinkedList<Double>> tier0BufferMap = new TreeMap<>();
			while(tier1Itr.hasNext()){
				Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>> tier1MapValue = 
						tier1Itr.next();
				Iterator<Map.Entry<LinkedList<String>, LinkedList<Double>>> tier2Itr = 
						tier1MapValue.getValue().entrySet().iterator();
				boolean findStatus = false;
				while(tier2Itr.hasNext()){
					Map.Entry<LinkedList<String>, LinkedList<Double>> tier2MapValue = tier2Itr.next();
					double matchScore = -1;
					if(tier2MapValue.getKey().equals(bufferPhrase)){
						//System.out.println("match>>"+tier2MapValue.getKey());
						tier0BufferMap.clear();
						tier1BufferResultList.addAll(tier2MapValue.getValue());
						findStatus = true;
						break;
					}else if(Arrays.asList(0,1,2).contains(contextIndex)){
						matchScore = layeredAlignment(
								bufferPhrase, tier2MapValue.getKey(), contextIndex);
					}else if(contextIndex > 2){
						matchScore = layeredAlignment(
								bufferPhrase, tier2MapValue.getKey());
					}
					int matchValue = Double.compare(matchScore, new Double(0).doubleValue());
					if(matchValue > 0){
						tier0BufferMap.put(matchScore, tier2MapValue.getValue());
						//System.out.println("turn>>");
					}
				}
				if(findStatus){
					break;
				}
			}
			LinkedList<Double> tier1BufferLList = new LinkedList<>();
			if(!tier0BufferMap.isEmpty()){
				//System.out.println("\t>>"+tier0BufferMap.size()+"\t"+tier1BufferResultList);
				tier1BufferLList = tier0BufferMap.descendingMap().firstEntry().getValue();
				tier1BufferResultList.addAll(tier1BufferLList);
			}
		}
		//System.out.println("\t retVal>>"+tier1BufferResultList);
		return tier1BufferResultList;
	}
	
	private double scorePhrase(LinkedList<Double> targetLList, LinkedList<Double> compareLList) {
	
		double rmsError = 10000;
		ArrayList<Double> tier1BufferList = new ArrayList<>();
		Double invarianceThreshold = new Double(1).doubleValue();
		for(int i=0;i<targetLList.size();i++){
			boolean variantEqual = false;
			Double invarianceConfidence = new Double(0).doubleValue();
			Double numVal = targetLList.get(i);
			Double denomVal = compareLList.get(i);
			int matchValue = Double.compare(numVal, denomVal);
			if(matchValue > 0){
				numVal = compareLList.get(i);
				denomVal = targetLList.get(i);
			}else if(matchValue == 0){
				invarianceConfidence = new Double(1).doubleValue();
				variantEqual = true;
			}
			if(!variantEqual){
				invarianceConfidence = (numVal/denomVal);
			}
			double invarianceDeviation = (invarianceThreshold - invarianceConfidence); 
			invarianceDeviation = Math.pow(invarianceDeviation, 2);
			tier1BufferList.add(invarianceDeviation);
		}
		if(!tier1BufferList.isEmpty()){
			rmsError = tier1BufferList.stream()
					.reduce((prev,curr)-> prev+curr).get().doubleValue();
			rmsError = Math.sqrt((rmsError/Integer.valueOf(tier1BufferList.size()).doubleValue()));
		}
		if(Double.compare(rmsError, new Double(0.10)) > 0){
			rmsError = new Double(3).doubleValue();
			//System.out.println("invar \t"+targetString+"\t>>"+compareString+"\t"+rmsError);
			//System.exit(0);
		}
		
		return(rmsError);
	}
	
	private double mapPhraseRelevance(ArrayList<LinkedList<String>> originalPhraseList,
			ArrayList<LinkedList<String>> comparePhraseList, 
			HashMap<String, Integer> contextPositionUpdateMap) {
		
		//System.out.println("\n\t****original>>"+originalPhraseList+"\n\tcompare"+comparePhraseList);
		if(originalPhraseList.size() != comparePhraseList.size()){
			System.err.println("phrase size error::"+originalPhraseList.size()+"\t"+comparePhraseList.size());
			System.exit(0);
		}
		TreeMap<Integer, Double> tier1BufferMap = new TreeMap<>();
		for(int i=0;i<originalPhraseList.size();i++){
			double rmsError = new Double(3).doubleValue();
			LinkedList<String> originalPhrase = originalPhraseList.get(i);
			LinkedList<String> comparePhrase = comparePhraseList.get(i);
			//System.out.println("\toriginal>>"+originalPhrase+"\tcompare"+comparePhrase);
			int orgContextIndex = -1;
			if(originalPhrase.contains(normTriggerTerm)){
				orgContextIndex = originalPhrase.lastIndexOf(normTriggerTerm);
			}else if(contextPositionUpdateMap.containsKey(originalPhrase.get(0))){
				orgContextIndex = contextPositionUpdateMap.get(originalPhrase.get(0));
			}
			int cmpContextIndex = -1;
			if(comparePhrase.contains(normTriggerTerm)){
				cmpContextIndex = comparePhrase.lastIndexOf(normTriggerTerm);
			}else if(contextPositionUpdateMap.containsKey(comparePhrase.get(0))){
				cmpContextIndex = contextPositionUpdateMap.get(comparePhrase.get(0));
			}
			if((orgContextIndex == cmpContextIndex) && (orgContextIndex != -1)){
				//System.out.println("contextIndex>>>>"+orgContextIndex+"\t"+cmpContextIndex);
				LinkedList<Double> targetLList = getRelevantInvariantAttribute(originalPhrase, orgContextIndex);
				LinkedList<Double> compareLList = getRelevantInvariantAttribute(comparePhrase, cmpContextIndex);
				if((!targetLList.isEmpty()) && (!compareLList.isEmpty())){
					//System.out.println(">>"+targetLList+"\t"+compareLList);
					rmsError = scorePhrase(targetLList, compareLList);
				}
			}
			tier1BufferMap.put(i, rmsError);
		}
		double returnScore = tier1BufferMap.values().stream().reduce((x,y)->x+y).get().doubleValue();
		//System.out.println("\t result>>"+tier1BufferMap);
		return(returnScore);
	}

	private TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> selectTemplateOptimization(
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferMap,
			LinkedList<String> variantPosLList, HashMap<String, Integer> contextPositionUpdateMap) {

		//System.out.println("\t varaint type>>"+variantPosLList);
		int startIndex = 0, endIndex = variantPosLList.size();
		ArrayList<LinkedList<String>> originalPhraseList = learnPhrase(variantPosLList);
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		Double bestScore = new Double(10000).doubleValue();
		TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1BufferMap = new TreeMap<>();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1MapValue = 
					tier1Itr.next();
			Iterator<Map.Entry<LinkedList<String>, Double>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			HashMap<LinkedList<String>, Double> tier2BufferMap = new HashMap<>();
			while(tier2Itr.hasNext()){
				ArrayList<LinkedList<String>> tier1BufferLList = new ArrayList<>();
				Map.Entry<LinkedList<String>, Double> tier2MapValue = tier2Itr.next();
				//System.out.println("\ntag****************"+tier2MapValue.getKey()+"\t>>"+tier1MapValue.getKey());
				Iterator<String> tier3Itr = tier2MapValue.getKey().iterator();
				while(tier3Itr.hasNext()){
					String tier3StringValue = tier3Itr.next();
					Integer compareContextId = (getIconClusterStats(tier3StringValue, 0)-1);
					Integer compareClusterId = getIconClusterStats(tier3StringValue, 1);
					ArrayList<LinkedList<String>> tier2BufferLList = new ArrayList<>();
					if(invariantClusters.containsKey(compareContextId)){
						if(invariantClusters.get(compareContextId).containsKey(compareClusterId)){
							Iterator<LinkedList<String>> tier4Itr = invariantClusters
									.get(compareContextId).get(compareClusterId).keySet().iterator();
							while(tier4Itr.hasNext()){
								LinkedList<String> tier4LListValue = tier4Itr.next();
								tier2BufferLList.add(tier4LListValue);
							}
						}else{
							tier2BufferLList.add(new LinkedList<>(Collections.nCopies(windowDimension, "#")));
						}
					}else{
						tier2BufferLList.add(new LinkedList<>(Collections.nCopies(windowDimension, "#")));
					}
					ArrayList<LinkedList<String>> tempLList = new ArrayList<>();
					for(int i=0;i<tier2BufferLList.size();i++){
						LinkedList<String> tier3LList = tier2BufferLList.get(i);
						//System.out.println("add>>>"+tier3LList);
						if(!tier1BufferLList.isEmpty()){
							for(int j=0;j<tier1BufferLList.size();j++){
								LinkedList<String> tier4LList = 
										new LinkedList<>(tier1BufferLList.get(j));
								//System.out.println("\t\t\tappend>>>"+tier4LList);
								tier4LList.addAll(tier3LList);
								//System.out.println("\t\t\tafter>>>"+tier4LList);
								tempLList.add(tier4LList);
							}
						}else{
							tempLList.add(tier3LList);
						}
					}
					tier1BufferLList = new ArrayList<>(tempLList);
				}
				LinkedList<Double> phraseLevelScore = new LinkedList<>();
				Iterator<LinkedList<String>> tier5Itr = tier1BufferLList.iterator();
				while(tier5Itr.hasNext()){
					LinkedList<String> tier5LListValue = tier5Itr.next();
					//System.out.println("\t>>"+tier5LListValue);
					LinkedList<String> compareList = new LinkedList<>(
							tier5LListValue.subList(startIndex, endIndex));
					ArrayList<LinkedList<String>> comparePhraseList = learnPhrase(compareList);
					double returnScore = mapPhraseRelevance(
							originalPhraseList, comparePhraseList, contextPositionUpdateMap);
					phraseLevelScore.add(returnScore);
					//System.out.println("\t"+originalPhraseList+"\n\t"+comparePhraseList+"\n\t::"+returnScore);
				}
				Collections.sort(phraseLevelScore);
				tier2BufferMap.put(tier2MapValue.getKey(), phraseLevelScore.get(0));
			}
			List<Map.Entry<LinkedList<String>, Double>> tier1BufferListMap = 
					compareMapEntries(new LinkedList<>(tier2BufferMap.entrySet()), 2);
			//System.out.println("\tbest>>"+tier1BufferListMap.get(0));
			
			LinkedHashMap<LinkedList<String>, Double> tempMap = new LinkedHashMap<>();
			tempMap.put(tier1BufferListMap.get(0).getKey(), tier1BufferListMap.get(0).getValue());
			if(Double.compare(tier1BufferListMap.get(0).getValue(), bestScore) < 0){
				bestScore = tier1BufferListMap.get(0).getValue();
				tier1BufferMap = new TreeMap<>();
				tier1BufferMap.put(tier1MapValue.getKey(), tempMap);
			}else if(Double.compare(tier1BufferListMap.get(0).getValue(), bestScore) == 0){
				bestScore = tier1BufferListMap.get(0).getValue();
				tier1BufferMap.put(tier1MapValue.getKey(), tempMap);
			}
		}
		//System.out.println("\t final>>"+tier1BufferMap);
		return(tier1BufferMap);
	}
	
	private void writeSequenceToFile(LinkedList<String> bufferList,
			int classInstance) {
		
		LinkedHashMap<String, Integer> tier1BufferMap = new LinkedHashMap<>();
		if(errorBufferMap.containsKey(classInstance)){
			tier1BufferMap = errorBufferMap.get(classInstance);
		}
		for(int i=0;i<bufferList.size();i++){
			//if(i==0){
			String tier1StringValue = bufferList.get(i);
			int patternCount = 0;
			if(tier1BufferMap.containsKey(tier1StringValue)){
				patternCount = tier1BufferMap.get(tier1StringValue);
			}
			patternCount++;
			tier1BufferMap.put(tier1StringValue, patternCount);
			//}
		}
		errorBufferMap.put(classInstance, tier1BufferMap);
		//System.out.println("\t>"+bufferList+"\t"+classInstance);
	}

	private LinkedList<Double> generateBiChannelFeatureVector(
			LinkedList<LinkedList<String>> bufferLList,
			LinkedList<LinkedList<String>> posBufferLList, 
			TreeMap<Integer, TreeMap<Integer, 
			LinkedHashMap<LinkedList<String>, Double>>> bufferMap, 
			int classInstance, HashMap<String, Integer> contextPositionUpdateMap, 
			int goldInstance) {

		boolean variantCovered = false;
		LinkedList<Double> defaultValue = new LinkedList<>();
		ArrayList<TreeMap<Integer, Double>> selectedScreenList = new ArrayList<>();
	
		TreeMap<Integer, Double> sigmoidWeightMap = sigmoidVariantScoring(
				new ArrayList<>(bufferMap.keySet()));
		/**
		TreeMap<Integer, Double> sigmoidWeightMap = softmaxVariantScoring(
				new ArrayList<>(bufferMap.keySet()));**/
		
		// compare each variant template from selected sentence
		Iterator<LinkedList<String>> tier2Itr = bufferLList.iterator();
		while(tier2Itr.hasNext()){
			LinkedList<String> tier2LListValue = tier2Itr.next();
			//System.out.println("\n\t Current>>"+tier2LListValue);
			// comparison from the selected variant index
			Integer variantIndex = variantContextPosition(tier2LListValue);
			Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier3Itr = 
					bufferMap.get(variantIndex).entrySet().iterator();
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier2BufferResultMap = 
					new TreeMap<>();
			LinkedList<String> variantPosLList = new LinkedList<>();
			for(int i=0;i<tier2LListValue.size();i++){
				if(!tier2LListValue.get(i).equals("0-0")){
					variantPosLList.addAll(posBufferLList.get(i));
				}
			}
			while(tier3Itr.hasNext()){
				Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier3MapValue = 
						tier3Itr.next();
				ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>(
						tier3MapValue.getValue().keySet());
				// screen for optimal templates from feature space
				HashSet<LinkedList<String>> tier1BufferResultSet = 
						screenForRelevantCluster(tier1BufferList, tier2LListValue);
				
				if(tier1BufferResultSet.isEmpty()){
					System.out.println("\n\t no suitable templates match>>"+tier2LListValue);
					System.exit(0);
				}
				if(!tier2BufferResultMap.isEmpty()){
					Iterator<LinkedList<String>> tier4Itr = 
							tier2BufferResultMap.firstEntry().getValue().keySet().iterator();
					while(tier4Itr.hasNext()){
						LinkedList<String> tier4LList = tier4Itr.next();
						if(tier1BufferResultSet.contains(tier4LList)){
							tier1BufferResultSet.remove(tier4LList);
						}
					}
				}
				
				
				/**
				Iterator<LinkedList<String>> t1Itr = tier1BufferResultSet.iterator();
				while(t1Itr.hasNext()){
					System.out.println("\t totalSet>>"+t1Itr.next());
				}**/
				
				
				// calculate optimal template representation
				Double varianceThreshold = new Double(0).doubleValue(); 
				for(int i=0;i<tier2LListValue.size();i++){
					if(!tier2LListValue.get(i).equals("0-0")){
						varianceThreshold = varianceThreshold + new Double(3);
					}
				}
				LinkedHashMap<LinkedList<String>, Double> tier1BufferMap = new LinkedHashMap<>();
				Iterator<LinkedList<String>> tier4Itr = tier1BufferResultSet.iterator();
				while(tier4Itr.hasNext()){
					LinkedList<String> tier4LListValue = tier4Itr.next();
					//System.out.println("t>>"+tier2LListValue+"\t c>>"+tier4LListValue);
					ArrayList<Double> tier2BufferList = new ArrayList<>();
					Double invarianceErrorMeasure = new Double(0).doubleValue();
					for(int i=0;i<tier2LListValue.size();i++){
						if(!tier2LListValue.get(i).equals("0-0")){
							String targetString = tier2LListValue.get(i);
							String compareString = tier4LListValue.get(i);
							double returnError = compareClusterInvariance(targetString, 
									compareString, posBufferLList.get(i)); 
							invarianceErrorMeasure = invarianceErrorMeasure 
									+ returnError;
							tier2BufferList.add(returnError);
						}else{
							tier2BufferList.add(new Double(0).doubleValue());
						}
					}
					//System.out.println("result*************"+tier2BufferList);
					tier1BufferMap.put(tier4LListValue, invarianceErrorMeasure);
					if(Double.compare(invarianceErrorMeasure, new Double(0).doubleValue())==0){
						break;
					}
				}
				
				List<Map.Entry<LinkedList<String>, Double>> tier1BufferListMap = 
						compareMapEntries(new LinkedList<>(tier1BufferMap.entrySet()), 2);

				LinkedHashMap<LinkedList<String>, Double> tier3BufferResultMap = 
						new LinkedHashMap<>();
				Iterator<Map.Entry<LinkedList<String>, Double>> tier5Itr = 
						tier1BufferListMap.iterator();
				while(tier5Itr.hasNext()){
					Map.Entry<LinkedList<String>, Double> tier5MapValue = tier5Itr.next();
					LinkedList<String> selectedList = tier5MapValue.getKey();
					Double minErrorScore = tier5MapValue.getValue();
					if(Double.compare(minErrorScore, varianceThreshold) < 0){
						// identified a viable template match
						tier3BufferResultMap.put(selectedList, minErrorScore);
						break;
					}
				}
				
				if(tier3BufferResultMap.isEmpty()){
					// no template match
					tier3BufferResultMap.put(new LinkedList<>(
							Collections.nCopies(tier2LListValue.size(), "0-0")) , varianceThreshold);
				}
				
				tier2BufferResultMap.put(tier3MapValue.getKey(), tier3BufferResultMap);
			}
			//System.out.println("\totimal>>"+tier2BufferResultMap);
			/**
			tier2BufferResultMap = selectTemplateOptimization(
					tier2BufferResultMap, variantPosLList, contextPositionUpdateMap);**/
			//System.out.println("\t postOptima>>"+tier2BufferResultMap);
			// compare across instances
			TreeMap<Integer, Double> selectedVal = optimalTemplateClass(
					tier2BufferResultMap, bufferMap.get(variantIndex)); 
			double selectedScore = selectedVal.firstEntry().getValue();
			if(variantIndex == 4 && (!variantCovered)){
				//System.out.println(">>>"+tier2BufferResultMap.get(selectedVal.firstKey()));
				if(tier2BufferResultMap.get(selectedVal.firstKey()) != null){
					variantCovered = true;
					LinkedList<String> tempBuffer = tier2BufferResultMap
							.get(selectedVal.firstKey()).keySet().iterator().next();
					ArrayList<LinkedList<String>> tempBufferArray = new ArrayList<>();
					if(errorBufferLList.containsKey(goldInstance)){
						tempBufferArray = errorBufferLList.get(goldInstance);
					}
					tempBufferArray.add(tempBuffer);
					errorBufferLList.put(goldInstance, tempBufferArray);
				}
			}
			
			//System.out.println(variantIndex+"\t"+tier2BufferResultMap.get(selectedVal.firstKey()));
			// update sigmoid weighted score
			selectedScore = (sigmoidWeightMap.get(variantIndex) * selectedScore);
			selectedVal.put(selectedVal.firstKey(), selectedScore);
			//System.out.println(">>>"+selectedVal);
			//if(variantIndex == 3){
				selectedScreenList.add(selectedVal);
			//}
		}
		//System.out.println("\t"+selectedScreenList);
		defaultValue = maximizedWeight(selectedScreenList);
		//System.out.println("\t"+bufferLList+"\n defaultValue>>"+defaultValue);
		//System.exit(0);
		
		return(defaultValue);
	}

	private TreeMap<Integer, LinkedList<String>> longestIconSequence(
			TreeMap<Integer, LinkedList<String>> bufferMap) {

		TreeMap<Integer, LinkedList<String>> tier1BufferMap = new TreeMap<>();
		int sizeCheck = 0;
		Iterator<Map.Entry<Integer, LinkedList<String>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, LinkedList<String>> tier1MapValue = tier1Itr.next();
			if(tier1MapValue.getValue().size() > sizeCheck){
				sizeCheck = tier1MapValue.getValue().size();
				tier1BufferMap.put(0, tier1MapValue.getValue());
				break;
			}
		}
		return(tier1BufferMap);
	}
	
	private LinkedList<String> appendPadding(LinkedList<String> bufferLList) {

		ArrayList<Integer> triggerIndexArray = (ArrayList<Integer>) IntStream
				.range(1, windowDimension+1).boxed().collect(Collectors.toList());
		Integer lastElement = Integer.parseInt(bufferLList.getLast().split("\\-")[0]);
		if(triggerIndexArray.contains(lastElement)){
			bufferLList.addLast("100-0");
		}else{
			bufferLList.addFirst("100-0");
		}
		return (bufferLList);
	}
	
	
	private LinkedList<LinkedList<String>> appendOrgPadding(
			LinkedList<LinkedList<String>> bufferLList) {

		LinkedList<String> tier1BufferList = bufferLList.get(bufferLList.size()-1);
		if(tier1BufferList.contains(normTriggerTerm)){
			bufferLList.addLast(new LinkedList<>(Collections.nCopies(windowDimension, "@")));
		}else{
			bufferLList.addFirst(new LinkedList<>(Collections.nCopies(windowDimension, "@")));
		}
		return (bufferLList);
	}
	
	private LinkedList<LinkedList<String>> forwardInteractionLoop(
			LinkedList<String> bufferLList, 
			LinkedList<LinkedList<String>> bufferResultList) {

		int midPoint = bufferLList.size()/2;
		for(int j=midPoint;j<=bufferLList.size();j++){
			LinkedList<String> tier1BufferLList = new LinkedList<>(bufferLList);
			for(int k=j;k<tier1BufferLList.size();k++){
				tier1BufferLList.set(k, "0-0");
			}
			bufferResultList.add(tier1BufferLList);
		}
		
		return(bufferResultList);
	}
	
	private LinkedList<LinkedList<String>> backwardInteractionLoop(
			LinkedList<String> bufferLList, 
			LinkedList<LinkedList<String>> bufferResultList) {

		int midPoint = bufferLList.size()/2;
		for(int j=midPoint;j<=bufferLList.size();j++){
			LinkedList<String> tier1BufferLList = new LinkedList<>(bufferLList);
			int endPoint = bufferLList.size()-j;
			for(int k=0;k<endPoint;k++){
				tier1BufferLList.set(k, "0-0");
			}
			bufferResultList.add(tier1BufferLList);
		}
		
		return(bufferResultList);
	}
	
	private LinkedList<String> extractIconFrame(LinkedList<String> bufferLList, 
			ArrayList<Integer> indexArray) {

		LinkedList<String> tier1BufferResultList = new LinkedList<>();
		for(int j=0;j<indexArray.size();j++){
			tier1BufferResultList.add(bufferLList.get(indexArray.get(j)));
		}
		while(tier1BufferResultList.size() < 4){
			tier1BufferResultList = appendPadding(tier1BufferResultList);
		}
		
		//System.out.println("tier1BufferResultList>>>"+tier1BufferResultList+"\t>>"+tier1BufferIndexArray);
		
		return (tier1BufferResultList);
	}
	
	private LinkedList<LinkedList<String>> extractOrgFrame(
			ArrayList<LinkedList<String>> bufferList,
			ArrayList<Integer> indexArray) {

		LinkedList<LinkedList<String>> tier1BufferResultList = new LinkedList<>();
		for(int j=0;j<indexArray.size();j++){
			tier1BufferResultList.add(bufferList.get(indexArray.get(j)));
		}
		
		while(tier1BufferResultList.size() < 4){
			tier1BufferResultList = appendOrgPadding(tier1BufferResultList);
		}
		
		return (tier1BufferResultList);
	}
	
	private ArrayList<Integer> call_relvantIconFrame(
			LinkedList<String> bufferLList) {

		LinkedList<String> tier1BufferResultList = new LinkedList<>();
		ArrayList<Integer> triggerIndexArray = (ArrayList<Integer>) IntStream
				.range(1, windowDimension+1).boxed().collect(Collectors.toList());
		ArrayList<Integer> tier1BufferArray = new ArrayList<>();
		for(int i=0;i<bufferLList.size();i++){
			if(triggerIndexArray.contains(Integer.parseInt(bufferLList.get(i).split("\\-")[0]))){
				tier1BufferArray.add(i);
			}
		}
		//System.out.println("\t>>"+tier1BufferArray);
		
		ArrayList<Integer> tier1BufferIndexArray = new ArrayList<>();
		tier1BufferIndexArray.add(tier1BufferArray.get(0));
		int range = tier1BufferArray.get(1)-tier1BufferArray.get(0);
		int i=1;
		//for(int i=1;i<=1;i++){
		if(tier1BufferArray.get(0) != 0){
			if(range > 2){
				if(tier1BufferArray.get(0)+i < bufferLList.size()){
					tier1BufferIndexArray.add(tier1BufferArray.get(0)+i);
				}
			}else{
				if(tier1BufferArray.get(0)-i >= 0){
					tier1BufferIndexArray.add(tier1BufferArray.get(0)-i);
				}
			}
		}else{
			if(range > 2){
				if(tier1BufferArray.get(0)+i < bufferLList.size()){
					tier1BufferIndexArray.add(tier1BufferArray.get(0)+i);
				}
			}
		}
		//}

		tier1BufferIndexArray.add(tier1BufferArray.get(1));
		//for(int i=1;i<=1;i++){
		if(tier1BufferArray.get(1) != bufferLList.size()-1){
			int addIndex = tier1BufferArray.get(1)-i;
			if(tier1BufferIndexArray.contains(addIndex)){
				addIndex = tier1BufferArray.get(1)+i;
			}
			if((addIndex < bufferLList.size()) && (addIndex >= 0)){
				tier1BufferIndexArray.add(addIndex);
			}
		}else{
			int addIndex = tier1BufferArray.get(1)-i;
			if(!tier1BufferIndexArray.contains(addIndex)){
				if((addIndex < bufferLList.size()) && (addIndex >= 0)){
				tier1BufferIndexArray.add(addIndex);
				}
			}
		}
		//}
		
		HashSet<Integer> tier1BufferSet = new HashSet<>(tier1BufferIndexArray);
		tier1BufferIndexArray = new ArrayList<>(tier1BufferSet);
		Collections.sort(tier1BufferIndexArray);
		
		//System.out.println("tier1BufferResultList>>>"+tier1BufferResultList+"\t>>"+tier1BufferIndexArray);

		return(tier1BufferIndexArray);
	}
	
	private LinkedList<LinkedList<String>> generateIconVariants(LinkedList<String> bufferLList) {

		LinkedList<LinkedList<String>> tier1BufferLList = new LinkedList<>();
		tier1BufferLList = forwardInteractionLoop(bufferLList, tier1BufferLList);
		tier1BufferLList = backwardInteractionLoop(bufferLList, tier1BufferLList);
		return(tier1BufferLList);
	}
	
	private LinkedList<Double> triggerPositionalVector(LinkedList<String> bufferLList) {

		ArrayList<Integer> triggerIndexArray = (ArrayList<Integer>) IntStream
				.range(1, windowDimension+1).boxed().collect(Collectors.toList());
		LinkedList<Double> tier1BufferLList = new LinkedList<>(
				Collections.nCopies(bufferLList.size(), 0.0));
		for(int i=0;i<bufferLList.size();i++){
			int currContext = getIconClusterStats(bufferLList.get(i), 0);
			if(triggerIndexArray.contains(currContext)){
				tier1BufferLList.set(i,new Double(1));
			}
		}
		return(tier1BufferLList);
	}
	
	private Double triggerRelationVector(LinkedList<String> bufferLList) {

		LinkedList<Double> tier1BufferLList = new LinkedList<>(
				Collections.nCopies(bufferLList.size(), 0.0));
		Double retValue = new Double(-1);
		for(int i=0;i<bufferLList.size();i++){
			String tier1StringValue = bufferLList.get(i);
			int contextId = getIconClusterStats(tier1StringValue, 0)-1;
			int clusterId = getIconClusterStats(tier1StringValue, 1);
			if(invariantClusters.containsKey(contextId)){
				if(invariantClusters.get(contextId).containsKey(clusterId)){
					Iterator<LinkedList<String>> tier1Itr = 
							invariantClusters.get(contextId).get(clusterId).keySet().iterator();
					while(tier1Itr.hasNext()){
						LinkedList<String> tier1LListValue = tier1Itr.next();
						if(tier1LListValue.stream().filter((currVal) -> currVal.matches("\\#"))
								.collect(Collectors.toList()).size() == 0){
							if(tier1LListValue.stream().filter((currVal) -> currVal.matches("VB.{1}")).collect(Collectors.toList()).size()>0){
								tier1BufferLList.set(i, new Double(100));
								retValue = new Double(1);
							}
							break;
						}
					}
					
				}
			}
		}
		
		return(retValue);
		
	}
	
	private LinkedList<Double> posSequenceVector(LinkedList<String> bufferLList, 
			HashMap<String, Integer> contextPositionUpdateMap, Integer cv) {

		LinkedList<String> posSequenceUnit = new LinkedList<>();
		LinkedList<Double> posSequenceContextVector = new LinkedList<>();
		LinkedList<String> defaultSequence = new LinkedList<>(
				Collections.nCopies(windowDimension, "@"));
		Double contextDoubleValue = new Double(0);
		for(int i=0;i<bufferLList.size();i++){
			String tier1StringValue = bufferLList.get(i);
			int contextId = getIconClusterStats(tier1StringValue, 0)-1;
			int clusterId = getIconClusterStats(tier1StringValue, 1);
			if(invariantClusters.containsKey(contextId)){
				if(invariantClusters.get(contextId).containsKey(clusterId)){
					Iterator<LinkedList<String>> tier1Itr = 
							invariantClusters.get(contextId).get(clusterId).keySet().iterator();
					while(tier1Itr.hasNext()){
						LinkedList<String> tier1LListValue = tier1Itr.next();
						//if(!tier1LListValue.contains(normTriggerTerm)){
						if(tier1LListValue.stream().filter((currVal) -> currVal.matches("\\#"))
								.collect(Collectors.toList()).size() == 0){
							for(int j=0;j<tier1LListValue.size();j++){
								String currPosTag = tier1LListValue.get(j);
								if(contextPositionUpdateMap.containsKey(currPosTag)){
									contextDoubleValue = new Double(
											contextPositionUpdateMap.get(currPosTag)+1);
									double clustervalue = new Double(clusterId)/new Double(100);
									contextDoubleValue = contextDoubleValue + clustervalue;
								}else if(currPosTag.equals(normTriggerTerm)){
									contextDoubleValue = new Double(contextId+1);
									double clustervalue = new Double(clusterId)/new Double(100);
									contextDoubleValue = contextDoubleValue + clustervalue;
								}else{
									contextDoubleValue = new Double(-100);
								}
								posSequenceContextVector.add(contextDoubleValue);
							}
							posSequenceUnit.addAll(tier1LListValue);
							break;
						}
						//}
					}
				}else{
					posSequenceUnit.addAll(defaultSequence);
					posSequenceContextVector.addAll(
							Collections.nCopies(windowDimension, new Double(-100)));
				}
			}else{
				posSequenceUnit.addAll(defaultSequence);
				posSequenceContextVector.addAll(
						Collections.nCopies(windowDimension, new Double(-100)));
			}
		}
		
		//System.out.println(">>"+posSequenceUnit+"\n\t"+posSequenceContextVector);
		//System.out.println(">>"+posSequenceUnit+"\t"+cv);
		//System.exit(0);
		return (posSequenceContextVector);
	}
	
	private TreeMap<Integer, Integer> averageTriggerDistance(LinkedList<String> bufferLList, Integer cv, 
			TreeMap<Integer, Integer> tier0BufferMap) {

		LinkedList<String> posSequenceUnit = new LinkedList<>();
		for(int i=0;i<bufferLList.size();i++){
			String tier1StringValue = bufferLList.get(i);
			int contextId = getIconClusterStats(tier1StringValue, 0)-1;
			int clusterId = getIconClusterStats(tier1StringValue, 1);
			if(invariantClusters.containsKey(contextId)){
				if(invariantClusters.get(contextId).containsKey(clusterId)){
					Iterator<LinkedList<String>> tier1Itr = 
							invariantClusters.get(contextId).get(clusterId).keySet().iterator();
					while(tier1Itr.hasNext()){
						LinkedList<String> tier1LListValue = tier1Itr.next();
						//if(!tier1LListValue.contains(normTriggerTerm)){
						if(tier1LListValue.stream().filter((currVal) -> currVal.matches("\\#"))
								.collect(Collectors.toList()).size() == 0){
							posSequenceUnit.addAll(tier1LListValue);
							break;
						}
						//}
					}
				}else{
					//posSequenceUnit.addAll(defaultSequence);
				}
			}else{
				//posSequenceUnit.addAll(defaultSequence);
			}
		}
		
		int startIndex = 0;
		for(int k=0;k<posSequenceUnit.size();k++){
			if(posSequenceUnit.get(k).equals(normTriggerTerm)){
				startIndex = k-startIndex;
			}
		}
		int count =0;
		if(tier0BufferMap.containsKey(startIndex)){
			count = tier0BufferMap.get(startIndex);
		}
		count++;
		tier0BufferMap.put(startIndex, count);
		//System.out.println("\t"+startIndex+"\t>>"+posSequenceUnit+"\t"+cv);
		
		return(tier0BufferMap);
	}
	
	private Double averageTriggerDistanceSelection(LinkedList<String> bufferLList) {
		
		LinkedList<String> posSequenceUnit = new LinkedList<>();
		for(int i=0;i<bufferLList.size();i++){
			String tier1StringValue = bufferLList.get(i);
			int contextId = getIconClusterStats(tier1StringValue, 0)-1;
			int clusterId = getIconClusterStats(tier1StringValue, 1);
			if(invariantClusters.containsKey(contextId)){
				if(invariantClusters.get(contextId).containsKey(clusterId)){
					Iterator<LinkedList<String>> tier1Itr = 
							invariantClusters.get(contextId).get(clusterId).keySet().iterator();
					while(tier1Itr.hasNext()){
						LinkedList<String> tier1LListValue = tier1Itr.next();
						//if(!tier1LListValue.contains(normTriggerTerm)){
						if(tier1LListValue.stream().filter((currVal) -> currVal.matches("\\#"))
								.collect(Collectors.toList()).size() == 0){
							posSequenceUnit.addAll(tier1LListValue);
							break;
						}
						//}
					}
				}else{
					//posSequenceUnit.addAll(defaultSequence);
				}
			}else{
				//posSequenceUnit.addAll(defaultSequence);
			}
		}
		
		int startIndex = 0;
		for(int k=0;k<posSequenceUnit.size();k++){
			if(posSequenceUnit.get(k).equals(normTriggerTerm)){
				startIndex = k-startIndex;
			}
		}
		
		Double retVal = new Double(100);
		if(startIndex <= 6){
			retVal = new Double(0);
		}
		
		return(retVal);
	}
	
	private LinkedList<Double> addLLRWeightedRepresentation(LinkedList<Double> bufferLList,
			LinkedList<LinkedList<String>> orgBufferLList, 
			LinkedHashMap<String, Double> orgLexemeAppraiser) {
		
		JeniaTagger.setModelsPath(systemProperties.getProperty("geniaModelFile"));
		LinkedList<Double> returnWeight = new LinkedList<>(
				Collections.nCopies(bufferLList.size(), 0.0));
		for(int i=0;i<bufferLList.size();i++){
			LinkedList<String> currOrgTemplate = orgBufferLList.get(i);
			LinkedList<Double> orgTemplateWeight = new LinkedList<>(
					Collections.nCopies(windowDimension, 0.0));
			for(int j=0;j<currOrgTemplate.size();j++){
				String tier1StringValue = currOrgTemplate.get(j);
				if(orgLexemeAppraiser.containsKey(tier1StringValue)){
					//orgTemplateWeight.set(j, orgLexemeAppraiser.get(tier1StringValue));
					
					Sentence baseForm = JeniaTagger.analyzeAll(tier1StringValue, true);
					Iterator<Token> tokenItr = baseForm.iterator();
					while(tokenItr.hasNext()){
						Token currentToken = tokenItr.next();
						if(currentToken.pos.matches("VB.{0,1}|JJ.{0,1}")){ //
							orgTemplateWeight.set(j, orgLexemeAppraiser.get(tier1StringValue));
						}
					}
				}
			}
			Double weightFactor = Collections.max(orgTemplateWeight);
			int compVal = Double.compare(weightFactor, new Double(0).doubleValue());
			if(compVal != 0){
				Double updatedWeight = (bufferLList.get(i)*weightFactor);
				bufferLList.set(i, updatedWeight);
				//returnWeight.set(i, weightFactor);
			}
		}
		
		return bufferLList;
	}
	
	private void errorAnalysis(String modelPhase) throws IOException {

		int tier1Integer = errorBufferLList.get(1).size();
		int tier2Integer = errorBufferLList.get(-1).size();
		int threshold = tier1Integer;
		if(tier2Integer < tier1Integer){
			threshold = tier2Integer;
		}
		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier1Itr = 
				errorBufferLList.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = tier1Itr.next();
			ArrayList<LinkedList<String>> tier1BufferArray = new ArrayList<>();
			tier1BufferArray.addAll(tier1MapValue.getValue());
			Random randBuffer = new Random();
			for(int i=0;i<threshold;i++){
				int randIndex = randBuffer.nextInt(tier1BufferArray.size());
				writeSequenceToFile(tier1BufferArray.get(randIndex), tier1MapValue.getKey());
				tier1BufferArray.remove(randIndex);
			}
		}
		
		FileWriter tier1FileWS = null;
		BufferedWriter tier1BuffWS = null;
		if(modelPhase.equals("test")){
			tier1FileWS = new FileWriter(systemProperties.getProperty("testSequenceFile"));
		}else{
			tier1FileWS = new FileWriter(systemProperties.getProperty("trainSequenceFile"));
		}
		
		tier1BuffWS = new BufferedWriter(tier1FileWS);
		
		Iterator<Map.Entry<Integer, LinkedHashMap<String, Integer>>> tier5Itr = 
				errorBufferMap.entrySet().iterator();
		while(tier5Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<String, Integer>> tier5MapValue = tier5Itr.next();
			List<Map.Entry<String, Integer>> tier1BufferListMap = 
					new LinkedList<>(tier5MapValue.getValue().entrySet()); 
			Collections.sort(tier1BufferListMap, new Comparator<Map.Entry<String, Integer>>() {
				// descending order
				@Override
				public int compare(Entry<String, Integer> currItem, 
						Entry<String, Integer> nextItem) {
					return (nextItem.getValue().compareTo(currItem.getValue()));
				}
			});
			
			int countCutoff = 0; 
			Iterator<Map.Entry<String, Integer>> tier6Itr = tier1BufferListMap.iterator();
			while(tier6Itr.hasNext()){
				/**
				if(countCutoff == 10){
					break;
				}**/
				Map.Entry<String, Integer> tier6MapValue = tier6Itr.next();
				tier1BuffWS.write(tier5MapValue.getKey()+"\t"+
						tier6MapValue.getKey()+"\t"+tier6MapValue.getValue());
				tier1BuffWS.newLine();
				countCutoff++;
			}
		}
		
		tier1BuffWS.close();
		System.out.println("+ve::"+errorBufferMap.get(1).size());
		System.out.println("-ve::"+errorBufferMap.get(-1).size());
	}
	
	
	public TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> 
	biChannelFeaureMap(
			TreeMap<Integer, TreeMap<Integer, 
			LinkedHashMap<LinkedList<String>, Double>>> bufferFeatureMap,
			TreeMap<Integer, LinkedHashMap<String, 
			TreeMap<Integer, LinkedList<String>>>> bufferInstanceMap, String modelPhase, 
			HashMap<String, Integer> contextPositionUpdateMap,
			LinkedHashMap<String, Double> orgLexemeAppraiser, 
			TreeMap<Integer, LinkedHashMap<String, TreeMap<Integer, 
			ArrayList<LinkedList<String>>>>> bufferOrgInstanceMap,
			TreeMap<Integer, LinkedHashMap<String, TreeMap<Integer, 
			ArrayList<LinkedList<String>>>>> bufferPosInstanceMap) throws IOException {

		TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> tier4BufferResultMap = 
				new TreeMap<>();
		int sentCount = 1;
		Iterator<Map.Entry<Integer, LinkedHashMap<String, TreeMap<Integer, LinkedList<String>>>>> tier1Itr = 
				bufferInstanceMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			int breakCount = 0;
			HashMap<String, ArrayList<LinkedList<Double>>> tier3BufferResultMap = 
					new HashMap<>();
			Map.Entry<Integer, LinkedHashMap<String, TreeMap<Integer, LinkedList<String>>>> tier1MapValue = 
					tier1Itr.next();
			Iterator<Map.Entry<String, TreeMap<Integer, LinkedList<String>>>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<String, TreeMap<Integer, LinkedList<String>>> tier2MapValue = 
						tier2Itr.next();
				//System.out.println("\t"+tier2MapValue.getKey()+"***************");
				//if(tier2MapValue.getKey().equals("BioInfer_d113@3#5") && tier1MapValue.getKey() == 1){
				System.out.println("\t"+tier1MapValue.getKey()+"\t"+tier2MapValue.getKey()+"\t"+sentCount);
				sentCount++;
				ArrayList<LinkedList<Double>> tier2BufferResultList = new ArrayList<>();
				TreeMap<Integer, LinkedList<String>> tier1BufferMap = 
						longestIconSequence(tier2MapValue.getValue());
				Iterator<Map.Entry<Integer, LinkedList<String>>> tier3Itr = 
						tier1BufferMap.entrySet().iterator();
				int index = 0;
				while(tier3Itr.hasNext()){
					Map.Entry<Integer, LinkedList<String>> tier3MapValue = tier3Itr.next();
					ArrayList<LinkedList<String>> tier3OrgBufferList = bufferOrgInstanceMap
							.get(tier1MapValue.getKey()).get(tier2MapValue.getKey())
							.firstEntry().getValue();
					ArrayList<LinkedList<String>> tier3PosBufferList = bufferPosInstanceMap
							.get(tier1MapValue.getKey()).get(tier2MapValue.getKey())
							.firstEntry().getValue();
					
					ArrayList<Integer> indexArray = 
							call_relvantIconFrame(tier3MapValue.getValue());
					//System.out.println(">>>"+indexArray+"\t"+tier3MapValue.getValue());
					
					LinkedList<String> tier2BufferLList = extractIconFrame(
							tier3MapValue.getValue(), indexArray);
					
					LinkedList<LinkedList<String>> tier2OrgBufferLList = extractOrgFrame(
							tier3OrgBufferList, indexArray);
					
					LinkedList<LinkedList<String>> tier2PosBufferLList = extractOrgFrame(
							tier3PosBufferList, indexArray);

					LinkedList<LinkedList<String>> tier1BufferLList =
							generateIconVariants(tier2BufferLList);
					
					//System.out.print("\t"+tier2BufferLList+"\t"+tier1BufferLList);
					//System.exit(0);
					
					//System.out.println("\t>>"+tier1BufferLList+"\n\t>>"+tier2OrgBufferLList+"\n\t"+tier2PosBufferLList);
					//System.out.println("\t"+tier1BufferLList.get(2));
					
					LinkedList<Double> tier1BufferResultLList = new LinkedList<>();
					int classInstance = tier1MapValue.getKey(); 
					int goldInstance = tier1MapValue.getKey();
					if(modelPhase.equals("test")){
						classInstance = 0;
					}
					
					/**
					 * Features for sequenced edges
					 */
					tier1BufferResultLList = 
							generateBiChannelFeatureVector(tier1BufferLList, tier2PosBufferLList,
									bufferFeatureMap, classInstance, contextPositionUpdateMap, goldInstance);
					
					
					//System.out.println("b4>>"+tier1BufferResultLList);
					/**
					 * Add LLR Weighted Representation to Vector
					 */
					/**
					tier1BufferResultLList = (addLLRWeightedRepresentation(
							tier1BufferResultLList, tier2OrgBufferLList, orgLexemeAppraiser));**/
					/**
					LinkedList<Double> m = new LinkedList<>();
					Double a = new Double(1);
					for(int i=0;i<tier1BufferResultLList.size();){
						a = a*tier1BufferResultLList.get(i);
						//m.add(tier1BufferResultLList.get(i)+tier1BufferResultLList.get(i+1));
						i=i+1;
					}
					m.add(a);**/
					
					///System.out.println("aftr>>"+tier1BufferResultLList);
					/**
					 * Trigger Positional Features
					 */
					//tier1BufferResultLList.addAll(triggerPositionalVector(tier2BufferLList));
					
					/**
					 * Find Verb value
					 */
					
					//tier1BufferResultLList.add(triggerRelationVector(tier2BufferLList));
					
					/**
					 * POS sequence vector
					 */
					/**
					tier1BufferResultLList.addAll(posSequenceVector(
							tier2BufferLList, contextPositionUpdateMap, tier1MapValue.getKey()));**/
					/**
					 * TriggerDistance 
					 */
					/**
					tier1BufferResultLList.add(
							averageTriggerDistanceSelection(tier3MapValue.getValue()));**/
					
					//System.out.println(">>"+tier1BufferResultLList);
					tier2BufferResultList.add(tier1BufferResultLList);
					index++;
				}
				
				tier3BufferResultMap.put(tier2MapValue.getKey(), tier2BufferResultList);
				
				//System.exit(0);
				//}
				breakCount++;
				/**
				if(breakCount == 2){
					break;
				}**/
			}
			//System.exit(0);
			tier4BufferResultMap.put(tier1MapValue.getKey(), tier3BufferResultMap);
		}
		
		//errorAnalysis(modelPhase);
		
		return (tier4BufferResultMap);
	}

}
