/**
 * 
 */
package com.prj.bundle.representation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.rmi.CORBA.Tie;

import org.omg.Messaging.SYNC_WITH_TRANSPORT;

/**
 * @author iasl
 *
 */
public class InvariantEdgeFeatureEvaluation {

	private TreeMap<Integer, ArrayList<LinkedList<String>>> invariantEdgeFeatureBundle;
	private  TreeMap<Integer,LinkedHashMap<LinkedList<String>, Integer>> subFeatureEdgeContext;
	private HashMap<LinkedList<String>, Integer> differentailFeatureEdgeFrequency;
	private  TreeMap<Integer,LinkedHashMap<LinkedList<String>, Double>> subCoRefEdgeContext;
	private TreeMap<Integer, TreeMap<Integer, HashSet<LinkedList<String>>>> differentialTemplate;
	private Integer windowDimension;
	
	/**
	 * @param iconFeatureMap 
	 * @param seedFrame 
	 * @param tier3BufferMap 
	 * 
	 */
	public InvariantEdgeFeatureEvaluation(
			TreeMap<Integer, ArrayList<LinkedList<String>>> bufferMap, Integer seedFrame) {
		invariantEdgeFeatureBundle = bufferMap;
		subCoRefEdgeContext= new TreeMap<>();
		subFeatureEdgeContext= new TreeMap<>();
		differentailFeatureEdgeFrequency = new HashMap<>();
		differentialTemplate = new TreeMap<>();
		windowDimension = seedFrame;
	}

	private void extractSubContexts(LinkedList<String> bufferLList, Integer instanceClass) {

		LinkedHashMap<LinkedList<String>, Integer> tier1BufferMap = new LinkedHashMap<>();
		if(subFeatureEdgeContext.containsKey(instanceClass)){
			tier1BufferMap = subFeatureEdgeContext.get(instanceClass);
		}
		for(int i=bufferLList.size();i>0;i--){
			LinkedList<String> tier1BufferLList = 
					new LinkedList<>(bufferLList.subList(0, i));
			int subFrameCount = 0;
			if(tier1BufferMap.containsKey(tier1BufferLList)){
				subFrameCount = tier1BufferMap.get(tier1BufferLList);
			}
			subFrameCount++;
			tier1BufferMap.put(tier1BufferLList, subFrameCount);
		}
		for(int j=1;j<bufferLList.size()-1;j++){
			LinkedList<String> tier1BufferLList = 
					new LinkedList<>(bufferLList.subList(j, bufferLList.size()));
			for(int k=tier1BufferLList.size();k>0;k--){
				LinkedList<String> tier2BufferLList = 
						new LinkedList<>(tier1BufferLList.subList(0, k));
				int subFrameCount = 0;
				if(tier1BufferMap.containsKey(tier2BufferLList)){
					subFrameCount = tier1BufferMap.get(tier2BufferLList);
				}
				subFrameCount++;
				tier1BufferMap.put(tier2BufferLList, subFrameCount);
			}
		}
		subFeatureEdgeContext.put(instanceClass, tier1BufferMap);
	}
	
	public void initializeFeatureEdgeSubContexts() {

		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier1Itr = 
				invariantEdgeFeatureBundle.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = tier1Itr.next();
			Iterator<LinkedList<String>> tier2Itr = tier1MapValue.getValue().iterator();
			while(tier2Itr.hasNext()){
				LinkedList<String> tier2LListValue = tier2Itr.next();
				extractSubContexts(tier2LListValue, tier1MapValue.getKey());
			}
		}
	}

	private LinkedList<Entry<LinkedList<String>, Integer>> compareMapEntries(
			LinkedList<Entry<LinkedList<String>, Integer>> linkedList, int order) {
		
		if(order == 1){
			Collections.sort(linkedList, new Comparator<Map.Entry<LinkedList<String>, Integer>>() {
				// descending
				@Override
				public int compare(Entry<LinkedList<String>, Integer> currItem, 
							Entry<LinkedList<String>, Integer> nextItem) {
					return (nextItem.getValue().compareTo(currItem.getValue()));
				}
			});
		}else if(order == 2){
			Collections.sort(linkedList, new Comparator<Map.Entry<LinkedList<String>, Integer>>() {
				// ascending
				@Override
				public int compare(Entry<LinkedList<String>, Integer> currItem, 
							Entry<LinkedList<String>, Integer> nextItem) {
					return (currItem.getValue().compareTo(nextItem.getValue()));
				}
			});
		}
		
		return(linkedList);
	}
	
	private HashSet<LinkedList<String>> extractDifferentialFeatureEdges(
			LinkedList<String> bufferLList) {

		//System.out.println("\t>>>>"+bufferLList);
		int pairLengthLimit = (bufferLList.size());
		int templateSize = 1;
		HashSet<LinkedList<String>> tier1BufferResultSet = new HashSet<>();
		while(templateSize < pairLengthLimit){
			for(int i=0;i<bufferLList.size()-1;i++){
				for(int j=i+1;j<bufferLList.size();j++){
					if((j+templateSize) <= pairLengthLimit){
						LinkedList<String> tier1BufferList = new LinkedList<>();
						tier1BufferList.add(bufferLList.get(i));
						tier1BufferList.addAll(bufferLList.subList(j, j+templateSize));	
						//System.out.println("\t sub1>>"+bufferLList.get(i));
						//System.out.println("\t sub2>>"+bufferLList.subList(j, j+templateSize));
						//System.out.println("\tinterim>>"+tier1BufferList);
						if(tier1BufferList.size()!=pairLengthLimit){
							tier1BufferResultSet.add(tier1BufferList);
						}
					}
				}
			}
			templateSize++;
		}
		//System.exit(0);
		return(tier1BufferResultSet);
	}
	
	public void getFeatureEdgeDifferentialContexts() {
		
		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier1Itr = 
				invariantEdgeFeatureBundle.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = tier1Itr.next();
			Iterator<LinkedList<String>> tier2Itr = tier1MapValue.getValue().iterator();
			while(tier2Itr.hasNext()){
				LinkedList<String> tier2LListValue = tier2Itr.next();
				HashSet<LinkedList<String>> tier1BufferSet = 
						extractDifferentialFeatureEdges(tier2LListValue);
				Iterator<LinkedList<String>> tier3Itr = tier1BufferSet.iterator();
				while(tier3Itr.hasNext()){
					Integer templateScore = 0;
					LinkedList<String> tier3MapValue = tier3Itr.next();
					//System.out.println("\t"+tier1MapValue);
					if(differentailFeatureEdgeFrequency.containsKey(tier3MapValue)){
						templateScore = differentailFeatureEdgeFrequency.get(tier3MapValue);
					}
					templateScore++;
					differentailFeatureEdgeFrequency.put(tier3MapValue, templateScore);
				}
			}
		}
		
	}

	private Integer getFrequencyScores(List<String> bufferList, Integer classType) {

		Integer frequencyCount = 0;
		if(subFeatureEdgeContext.get(classType).containsKey(bufferList)){
			frequencyCount = subFeatureEdgeContext.get(classType).get(bufferList);
		}
		
		return(frequencyCount);
	}
	
	private Double scoreFeatureEdgeVariants(LinkedList<String> bufferLList, Integer classType) {
		
		LinkedList<String> testEdge = bufferLList;
		LinkedList<String> conditionedEdge = new LinkedList<>();
		Double numScore = new Double(0).doubleValue(), denScore = new Double(0).doubleValue();
		Double conditionedProb = new Double(0).doubleValue();
		if(bufferLList.size()>1){
			conditionedEdge = new LinkedList<>(bufferLList.subList(0, bufferLList.size()-1));
			numScore =  getFrequencyScores(testEdge, classType).doubleValue();
			denScore =  getFrequencyScores(conditionedEdge, classType).doubleValue();
		}else{
			numScore =  getFrequencyScores(testEdge, classType).doubleValue();
			denScore = numScore;
		}
		//System.out.println("\t"+numScore+"\t"+denScore);
		conditionedProb = (numScore/denScore);
		
		return(conditionedProb);
	}

	private LinkedHashMap<LinkedList<String>, Double> callSequentialProbabilityEval(
			ArrayList<LinkedList<String>> bufferList, Integer classType) {
		
		LinkedHashMap<LinkedList<String>, Double> tier1ResultBufferMap = new LinkedHashMap<>();
		LinkedList<Double> collectiveScore = new LinkedList<>();
		Iterator<LinkedList<String>> tier1Itr = bufferList.iterator();
		while(tier1Itr.hasNext()){
			LinkedList<String> tier1LList = tier1Itr.next();
			Double conditionedSequenceScore = new Double(1).doubleValue();
			for(int i=tier1LList.size();i>0;i--){
				conditionedSequenceScore = (conditionedSequenceScore * 
						(scoreFeatureEdgeVariants(new LinkedList<>(
								tier1LList.subList(0, i)), classType)));
			}
			conditionedSequenceScore = Math.pow(windowDimension, conditionedSequenceScore);// Math.exp(conditionedSequenceScore);
			collectiveScore.add(conditionedSequenceScore);
			tier1ResultBufferMap.put(tier1LList, conditionedSequenceScore);
		}
		
		Double maxScore = collectiveScore.stream().reduce((x,y)-> x+y).get();

		Iterator<Map.Entry<LinkedList<String>, Double>> tier2Itr = 
				tier1ResultBufferMap.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<LinkedList<String>, Double> tier2MapValue = tier2Itr.next();
			tier1ResultBufferMap.put(tier2MapValue.getKey(), (tier2MapValue.getValue()/maxScore));
		}
		
		return(tier1ResultBufferMap);
	}
	
	private Double instancePresentInMap(
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferMap,
			Entry<LinkedList<String>, Double> bufferEntryValue, Double maxClassScore) {

		Double returnScore = new Double(0).doubleValue();
		int index = 1;
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1MapValue = 
					tier1Itr.next();
			if(tier1MapValue.getValue().containsKey(bufferEntryValue.getKey())){
				returnScore = tier1MapValue.getValue().get(bufferEntryValue.getKey());
				index++;
			}
		}
		
		returnScore = ((returnScore + 
				(bufferEntryValue.getValue()/maxClassScore)));
		
		return(returnScore);
	}
	
	private boolean matchIconTemplates(LinkedList<String> bufferLList, LinkedList<String> compareLList){
		
		boolean retVal = false;
		HashSet<LinkedList<String>> tier1BufferSet = 
				extractDifferentialFeatureEdges(bufferLList);
		if(tier1BufferSet.stream()
				.filter((currList) -> currList.equals(compareLList))
				.collect(Collectors.toList()).size() > 0){
			retVal = true;
		}
		return(retVal);
	}

	private  void invokeBatchNormalization() {

		/**
		 * Identify feature Size difference
		 */
		TreeMap<Integer, Integer> tier1BufferMap = new TreeMap<>();
		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier1Itr = 
				invariantEdgeFeatureBundle.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = tier1Itr.next();
			tier1BufferMap.put(tier1MapValue.getValue().size(), tier1MapValue.getKey());
		}
		System.out.println("\t"+tier1BufferMap);
		Integer batchSize = tier1BufferMap.firstKey();
		tier1BufferMap.remove(batchSize);
		batchSize = (int) (1.0*(batchSize*2));
		//batchSize = (int) ((batchSize/3));
		System.out.println("\t"+batchSize);
		List<Map.Entry<LinkedList<String>, Integer>> tier1BufferListMap =
				compareMapEntries(new LinkedList<>(differentailFeatureEdgeFrequency.entrySet()),1);
		Iterator<Integer> tier2Itr = tier1BufferMap.values().iterator();
		while(tier2Itr.hasNext()){
			Integer tier2IntegerValue = tier2Itr.next();
			ArrayList<LinkedList<String>> tier1BufferList = 
					invariantEdgeFeatureBundle.get(tier2IntegerValue);
			int reductionCount = 0, rotation=0;
			while(tier1BufferList.size() > batchSize){
				Iterator<Map.Entry<LinkedList<String>, Integer>> tier3Itr = 
						tier1BufferListMap.iterator();
				while(tier3Itr.hasNext()){
					int priorFeatureSpaceSize = tier1BufferList.size();
					Map.Entry<LinkedList<String>, Integer> tier3MapValue = tier3Itr.next();
					int cutoffValue = 1; 
					if(tier3MapValue.getValue() > 1){
						cutoffValue = new Double(
								Math.ceil(tier3MapValue.getValue()/3)).intValue();
						//cutoffValue = tier3MapValue.getValue();
					}
					int templateMatchSize = 0;
					Iterator<LinkedList<String>> tier4Itr = tier1BufferList.iterator();
					while(tier4Itr.hasNext()){
						LinkedList<String> tier4LListValue = tier4Itr.next();
						if(matchIconTemplates(tier4LListValue, tier3MapValue.getKey())){
							templateMatchSize++;
							if(templateMatchSize < cutoffValue){
								tier4Itr.remove();
							}else{
								//System.out.println("breakSize\t>>"+templateMatchSize);
								break;
							}
						}
					}
					//System.out.println("\t1. "+priorFeatureSpaceSize+"\t 2. "+tier1BufferList.size());
					reductionCount = tier1BufferList.size();
					if(reductionCount <= batchSize){
						break;
					}else{
						System.out.println("\t count>>"+reductionCount+
								"\t target>>"+batchSize+"\t rotation>>"+rotation);
					}
				}
				rotation++;
			}
			invariantEdgeFeatureBundle.put(tier2IntegerValue, tier1BufferList);
		}
		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> t1Itr = 
				invariantEdgeFeatureBundle.entrySet().iterator();
		while(t1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = t1Itr.next();
			System.out.println("\t"+tier1MapValue.getKey()+"\t>>"+tier1MapValue.getValue().size());			
		}
		//System.exit(0);
	}
	
	public TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> callSoftmaxFeatureEdgeScore() {

		TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier3ResultBufferMap = 
				new TreeMap<>();
		TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1ResultBufferMap = 
				new TreeMap<>();
		invokeBatchNormalization();
		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier1Itr = 
				invariantEdgeFeatureBundle.entrySet().iterator();
		Double maxScore = new Double(0).doubleValue();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = tier1Itr.next();
			LinkedHashMap<LinkedList<String>, Double> tier2ResultBufferMap = 
					callSequentialProbabilityEval(tier1MapValue.getValue(), tier1MapValue.getKey());
			maxScore = maxScore+tier2ResultBufferMap.values().stream().reduce((x,y)->x+y).get();
			tier1ResultBufferMap.put(tier1MapValue.getKey(), tier2ResultBufferMap);
		}
		
		tier1ResultBufferMap = featureDropoutNormalization(tier1ResultBufferMap);
		
		int totalFeatureSize = 0;
		Double maxClassScore = new Double(maxScore).doubleValue(); //new Double(1); //
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier2Itr = 
				tier1ResultBufferMap.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier2MapValue = 
					tier2Itr.next();
			LinkedHashMap<LinkedList<String>, Double> tier4ResultBufferMap = new LinkedHashMap<>();
			if(tier3ResultBufferMap.containsKey(tier2MapValue.getKey())){
				tier4ResultBufferMap = tier3ResultBufferMap.get(tier2MapValue.getKey());
			}
			tier4ResultBufferMap.putAll(tier2MapValue.getValue().entrySet().stream()
					.collect(Collectors.toMap(currVal -> currVal.getKey(), currVal -> 
					instancePresentInMap(tier3ResultBufferMap, currVal, maxClassScore))));
			tier3ResultBufferMap.put(tier2MapValue.getKey(), tier4ResultBufferMap);
			totalFeatureSize = totalFeatureSize+tier4ResultBufferMap.size();
		}
		
		System.out.println("feature size prior \t"+totalFeatureSize);
		return(tier3ResultBufferMap);
	}

	public TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> featureDropoutNormalization(
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferMap) {
		
		TreeMap<Integer, TreeMap<Integer, ArrayList<LinkedList<String>>>> tier1BufferResultMap = 
				new TreeMap<>();
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Integer>>> tier1Itr = 
				subFeatureEdgeContext.entrySet().iterator();
		while(tier1Itr.hasNext()){
			TreeMap<Integer, ArrayList<LinkedList<String>>> tier1BufferMap = new TreeMap<>();
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Integer>> tier1MapValue = 
					tier1Itr.next();
			Iterator<Map.Entry<LinkedList<String>, Integer>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<LinkedList<String>, Integer> tier2MapValue = tier2Itr.next();
				if((tier2MapValue.getKey().size() > 1) 
						&& (tier2MapValue.getKey().size() < windowDimension)){
					ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>();
					if(tier1BufferMap.containsKey(tier2MapValue.getValue())){
						tier1BufferList = tier1BufferMap.get(tier2MapValue.getValue());
					}
					tier1BufferList.add(tier2MapValue.getKey());
					tier1BufferMap.put(tier2MapValue.getValue(), tier1BufferList);
				}
			}
			
			/**
			Iterator<Entry<Integer, ArrayList<LinkedList<String>>>> t1Itr = tier1BufferMap.entrySet().iterator();
			while(t1Itr.hasNext()){
				System.out.println("\t"+t1Itr.next());
			}**/

			Integer dropFeatures = (int) Math.round(0.05*tier1BufferMap.size()); 
			ArrayList<LinkedList<String>> tier2BufferList = new ArrayList<>();
			Iterator<Entry<Integer, ArrayList<LinkedList<String>>>> tier3Itr = 
					tier1BufferMap.entrySet().iterator();
			int dropCount = 0;
			while(tier3Itr.hasNext()){
				Entry<Integer, ArrayList<LinkedList<String>>> tier3MapValue = tier3Itr.next();
				tier2BufferList.addAll(tier3MapValue.getValue());
				//System.out.println("\t"+tier2BufferList.size()+"\t"+dropCount+"\t"+dropFeatures);
				dropCount++;
				if(dropCount >= dropFeatures){
					break;
				}
				tier3Itr.remove();
			}
			tier1BufferResultMap.put(tier1MapValue.getKey(), tier1BufferMap);
			
			System.out.println("\t"+tier2BufferList.size()+"\t"+tier1MapValue.getKey());
			
			Iterator<Map.Entry<LinkedList<String>, Double>> tier4Itr = 
					bufferMap.get(tier1MapValue.getKey()).entrySet().iterator();
			while(tier4Itr.hasNext()){
				Map.Entry<LinkedList<String>, Double> tier4MapValue = tier4Itr.next();
				LinkedList<String> tier1BufferLList = tier4MapValue.getKey();
				boolean removeStatus = false;
				for(int i=tier1BufferLList.size();i>0;i--){
					LinkedList<String> tier2BufferLList = 
							new LinkedList<>(tier1BufferLList.subList(0, i));
					if(tier2BufferList.stream()
							.filter((currList) -> currList.equals(tier2BufferLList))
							.collect(Collectors.toList()).size() > 0){
						//System.out.println("\t"+tier1BufferLList);
						removeStatus = true;
						break;
					}
				}
				if(removeStatus){
					tier4Itr.remove();
				}
			}
		}
		
		return(bufferMap);
	}
	
	private void extractPairedCoRefContexts(LinkedList<String> bufferLList, 
			TreeMap<Integer, ArrayList<Integer>> bufferMap, Integer instanceType) {
		
		LinkedHashMap<LinkedList<String>, Double> tier1BufferMap = new LinkedHashMap<>();
		if(subCoRefEdgeContext.containsKey(instanceType)){
			tier1BufferMap = subCoRefEdgeContext.get(instanceType);
		}
		
		int coRefRangeLimit = 1;//windowDimension-1; 
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
					//tier1BufferLList.addAll(tier1BufferList);
					Collections.sort(tier1BufferLList);
				}else{
					tier1BufferLList.add(tier1MapValue.getKey());
				}
				if(!tier1BufferSet.contains(tier1BufferLList)){
					tier1BufferSet.add(tier1BufferLList);
					
					LinkedList<String> tier2BufferLList = new LinkedList<>();
					if(tier1BufferLList.size() == coRefRangeLimit){
						Iterator<Integer> tier3Itr = tier1BufferLList.iterator();
						while(tier3Itr.hasNext()){
							Integer tier3IntegerValue = tier3Itr.next();
							tier2BufferLList.add(bufferLList.get(tier3IntegerValue));
						}
					}else if(!tier1BufferLList.isEmpty()){
						System.out.println("\t"+tier1BufferLList+"\t>>>"+tier1MapValue);
						System.out.println("critical error in pairing ~ extractPairedCoRefContexts()");
						System.exit(0);
					}
					if(!tier2BufferLList.isEmpty()){
						Double subFrameCount = new Double(0).doubleValue();
						if(tier1BufferMap.containsKey(tier2BufferLList)){
							subFrameCount = tier1BufferMap.get(tier2BufferLList).doubleValue();
						}
						subFrameCount = (subFrameCount + new Double(1).doubleValue());
						tier1BufferMap.put(tier2BufferLList, subFrameCount);
						extractSubContexts(tier2BufferLList, instanceType);
					}
				}
			}
		}
		subCoRefEdgeContext.put(instanceType, tier1BufferMap);
	}
	
	private void findTriggerFeatureBlocks(LinkedList<String> bufferLList, int corefHopSize, 
			Integer instanceType) {
		
		ArrayList<Integer> triggerSelectRange = (ArrayList<Integer>) 
				IntStream.range(1, windowDimension+1).boxed().collect(Collectors.toList());
		TreeMap<Integer, ArrayList<Integer>> tier1BufferMap = new TreeMap<>();
		int triggerIndex=0;
		Iterator<String> tier1Itr = bufferLList.iterator();
		while(tier1Itr.hasNext()){
			Integer tier1BufferInteger = Integer.parseInt(tier1Itr.next().split("\\-")[0]);
			if(triggerSelectRange.contains(tier1BufferInteger)){
				int startHop = (triggerIndex-corefHopSize);
				if(startHop < 0){
					startHop = 0;
				}
				int endHop = (triggerIndex+(corefHopSize+1));
				if(endHop > bufferLList.size()){
					endHop = bufferLList.size();
				}
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
			extractPairedCoRefContexts(bufferLList, tier1BufferMap, instanceType);
		}
	}
	
	private void findCompleteTriggerFeatureBlocks(LinkedList<String> bufferLList, int corefHopSize, 
			Integer instanceType) {
		
		ArrayList<Integer> triggerSelectRange = (ArrayList<Integer>) 
				IntStream.range(1, windowDimension+1).boxed().collect(Collectors.toList());
		TreeMap<Integer, ArrayList<Integer>> tier1BufferMap = new TreeMap<>();
		int triggerIndex=0;
		Iterator<String> tier1Itr = bufferLList.iterator();
		while(tier1Itr.hasNext()){
			Integer tier1BufferInteger = Integer.parseInt(tier1Itr.next().split("\\-")[0]);
			if(triggerSelectRange.contains(tier1BufferInteger)){
				int startHop = (triggerIndex-corefHopSize);
				if(startHop < 0){
					startHop = 0;
				}
				int endHop = (triggerIndex+(corefHopSize+1));
				if(endHop > bufferLList.size()){
					endHop = bufferLList.size();
				}
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
			extractPairedCoRefContexts(bufferLList, tier1BufferMap, instanceType);
		}
	}
	
	private void processIconSequenceEdges(int corefHopSize, int instanceNumber) {

		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier1Itr = 
				invariantEdgeFeatureBundle.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = tier1Itr.next();
			Iterator<LinkedList<String>> tier2Itr = tier1MapValue.getValue().iterator();
			while(tier2Itr.hasNext()){
				Integer instanceType = tier1MapValue.getKey();
				LinkedList<String> tier2LListValue = tier2Itr.next();
				findTriggerFeatureBlocks(tier2LListValue, corefHopSize, instanceType);
				//findCompleteTriggerFeatureBlocks(tier2LListValue, corefHopSize, instanceType);
			}
		}
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier2Itr = 
				subCoRefEdgeContext.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier2MapValue = 
					tier2Itr.next();
			Double currInstanceSize = new Double(tier2MapValue.getValue().size()).doubleValue();
			//Double currInstanceSize = new Double(instanceNumber).doubleValue();
			//Double currInstanceSize = new Double(1).doubleValue();
			//System.out.println("\t>>"+currInstanceSize);
			LinkedHashMap<LinkedList<String>, Double> tier2BufferMap = new LinkedHashMap<>();
			tier2BufferMap.putAll(tier2MapValue.getValue().entrySet()
					.stream().collect(Collectors.toMap((currVal) -> currVal.getKey(), 
							(currVal) -> (currVal.getValue()/currInstanceSize))));
			subCoRefEdgeContext.put(tier2MapValue.getKey(), tier2BufferMap);
		}
		
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> t1Itr = 
				subCoRefEdgeContext.entrySet().iterator();
		while(t1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> t1V = t1Itr.next();
			Iterator<Map.Entry<LinkedList<String>, Double>> t2Itr = 
					t1V.getValue().entrySet().iterator();
			while(t2Itr.hasNext()){
				Map.Entry<LinkedList<String>, Double> t2V = t2Itr.next();
				System.out.println("\t"+t1V.getKey()+"\t>>"+t2V.getKey()+"\t>>"+t2V.getValue());
			}
		}
		System.exit(0);
	}
	
	private LinkedList<Entry<LinkedList<String>, Double>> compareMapEntriesDouble(
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
	
	private TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> featureDropOut(
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferMap, 
			int instanceDrop) {

		LinkedHashMap<LinkedList<String>, Double> tier1BufferMap = bufferMap.get(instanceDrop);
		List<Map.Entry<LinkedList<String>, Double>> tier1BufferListMap = 
				new LinkedList<>(tier1BufferMap.entrySet());
		tier1BufferListMap = compareMapEntriesDouble(new LinkedList<>(tier1BufferMap.entrySet()), 2);
		List<Map.Entry<LinkedList<String>, Double>> tier2BufferListMap = 
				tier1BufferListMap.subList(0, tier1BufferListMap.size());
		LinkedHashMap<LinkedList<String>, Double> tier2BufferMap = new LinkedHashMap<>();
		Iterator<Map.Entry<LinkedList<String>, Double>> tier1Itr = tier2BufferListMap.iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<LinkedList<String>, Double> tier1MapValue = tier1Itr.next();
			tier2BufferMap.put(tier1MapValue.getKey(), tier1MapValue.getValue());
		}
		bufferMap.put(instanceDrop, tier2BufferMap);
		
		return (bufferMap);
	}
	
	private LinkedHashMap<LinkedList<String>, Double> templateSummary() {

		LinkedHashMap<LinkedList<String>, Double> tier1BufferMap = new LinkedHashMap<>();
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier1Itr = 
				subCoRefEdgeContext.entrySet().iterator();
		
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1MapValue = 
					tier1Itr.next();
			Iterator<Map.Entry<LinkedList<String>, Double>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<LinkedList<String>, Double> tier2MapValue = tier2Itr.next();
				Double tier2DoubleValue = tier2MapValue.getValue();
				Double totalCount = new Double(0).doubleValue();
				if(tier1BufferMap.containsKey(tier2MapValue.getKey())){
					totalCount = tier1BufferMap.get(tier2MapValue.getKey());
				}
				Double currentPairScore = Math.exp(tier2DoubleValue);
				totalCount = totalCount + currentPairScore;
				tier1BufferMap.put(tier2MapValue.getKey(), totalCount);
			}
		}
		
		Double totalScore = tier1BufferMap.values().stream().reduce((x,y)->(x+y)).get().doubleValue();
		System.out.println("total score>>"+totalScore);
		return(tier1BufferMap);
	}
	
	private TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> scoreCoRefEdges() {

		TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1ResultBufferMap = 
				new TreeMap<>();
		LinkedHashMap<LinkedList<String>, Double> templateSummaryMap = templateSummary();
		
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier1Itr = 
				subCoRefEdgeContext.entrySet().iterator();
		
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1MapValue = 
					tier1Itr.next();
			LinkedHashMap<LinkedList<String>, Double> tier1BufferMap = new LinkedHashMap<>();
			Iterator<Map.Entry<LinkedList<String>, Double>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<LinkedList<String>, Double> tier2MapValue = tier2Itr.next();
				Double tier2DoubleValue = tier2MapValue.getValue();
				Double currentPairScore = Math.exp(tier2DoubleValue);
				//Double totalCount = templateSummaryMap;
				Double totalCount = new Double(0).doubleValue();
				if(templateSummaryMap.containsKey(tier2MapValue.getKey())){
					totalCount = templateSummaryMap.get(tier2MapValue.getKey());
				}
				Double softMaxScore = (currentPairScore/totalCount);
				//softMaxScore = (softMaxScore*tier2DoubleValue);
				
				tier1BufferMap.put(tier2MapValue.getKey(), softMaxScore);
			}

			LinkedHashMap<LinkedList<String>, Double> tier2BufferMap = new LinkedHashMap<>();
			tier2BufferMap.putAll(tier1BufferMap);
			tier1ResultBufferMap.put(tier1MapValue.getKey(), tier2BufferMap);
		}
		
		return(tier1ResultBufferMap);
	}

	private TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> calculateSequencedScores() {

		TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier2ResultBufferMap =
				new TreeMap<>();
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier1Itr = 
				subCoRefEdgeContext.entrySet().iterator();
		Double maxScore1 = new Double(0).doubleValue();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1MapValue = 
					tier1Itr.next();
			int classType = tier1MapValue.getKey();
			Iterator<Map.Entry<LinkedList<String>, Double>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			LinkedHashMap<LinkedList<String>, Double> tier1ResultBufferMap = new LinkedHashMap<>();
			LinkedList<Double> collectiveScore = new LinkedList<>();
			while(tier2Itr.hasNext()){
				Map.Entry<LinkedList<String>, Double> tier2MapValue = tier2Itr.next();
				LinkedList<String> tier1LList = tier2MapValue.getKey();
				Double conditionedSequenceScore = new Double(1).doubleValue();
				for(int i=tier1LList.size();i>0;i--){
					conditionedSequenceScore = (conditionedSequenceScore * 
							(scoreFeatureEdgeVariants(new LinkedList<>(
									tier1LList.subList(0, i)), classType)));
				}
				//conditionedSequenceScore = Math.pow(windowDimension, conditionedSequenceScore);
				conditionedSequenceScore = Math.exp(conditionedSequenceScore);
				collectiveScore.add(conditionedSequenceScore);
				tier1ResultBufferMap.put(tier1LList, conditionedSequenceScore);
			}
			
			Double maxScore = collectiveScore.stream().reduce((x,y)-> x+y).get();
			maxScore1 = maxScore1+maxScore;
			
			Iterator<Map.Entry<LinkedList<String>, Double>> tier3Itr = 
					tier1ResultBufferMap.entrySet().iterator();
			while(tier3Itr.hasNext()){
				Map.Entry<LinkedList<String>, Double> tier3MapValue = tier3Itr.next();
				tier1ResultBufferMap.put(tier3MapValue.getKey(), (tier3MapValue.getValue()/maxScore));
			}
			tier2ResultBufferMap.put(classType, tier1ResultBufferMap);
		}
		
		/**
		int totalFeatureSize = 0;
		Double maxClassScore = new Double(maxScore1); //new Double(1); //
		TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier3ResultBufferMap = 
				new TreeMap<>();
		
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier2Itr = 
				tier2ResultBufferMap.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier2MapValue = 
					tier2Itr.next();
			LinkedHashMap<LinkedList<String>, Double> tier4ResultBufferMap = new LinkedHashMap<>();
			if(tier3ResultBufferMap.containsKey(tier2MapValue.getKey())){
				tier4ResultBufferMap = tier3ResultBufferMap.get(tier2MapValue.getKey());
			}
			tier4ResultBufferMap.putAll(tier2MapValue.getValue().entrySet().stream()
					.collect(Collectors.toMap(currVal -> currVal.getKey(), currVal -> 
					instancePresentInMap(tier3ResultBufferMap, currVal, maxClassScore))));
			tier3ResultBufferMap.put(tier2MapValue.getKey(), tier4ResultBufferMap);
			totalFeatureSize = totalFeatureSize+tier4ResultBufferMap.size();
		}**/
		
		return(tier2ResultBufferMap);
	}
	
	public TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> 
	callCorefFeatureEdgeScore(int corefHopSize, int instanceNumber) {
		
		processIconSequenceEdges(corefHopSize, instanceNumber);
		
		TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1ResultBufferMap = 
				calculateSequencedScores();
		
		/**
		TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1ResultBufferMap = 
				scoreCoRefEdges();**/
		/**
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> t1Itr = 
				tier1ResultBufferMap.entrySet().iterator();
		while(t1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> t1V = t1Itr.next();
			Iterator<Map.Entry<LinkedList<String>, Double>> t2Itr = 
					t1V.getValue().entrySet().iterator();
			while(t2Itr.hasNext()){
				Map.Entry<LinkedList<String>, Double> t2V = t2Itr.next();
				System.out.println("\t>>"+t1V.getKey()+"\t"+t2V.getKey()+"\t>>"+t2V.getValue());
			}
		}**/
		
		
		return(tier1ResultBufferMap);
		
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
	
	private LinkedList<String> call_relvantIconFrame(
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
		for(int j=0;j<tier1BufferIndexArray.size();j++){
			tier1BufferResultList.add(bufferLList.get(tier1BufferIndexArray.get(j)));
		}
		while(tier1BufferResultList.size() < 4){
			tier1BufferResultList = appendPadding(tier1BufferResultList);
		}

		//System.out.println("out>>>>"+tier1BufferResultList+"\t"+bufferLList);
		//System.out.println("out>>>>"+tier1BufferResultList);
		
		return(tier1BufferResultList);
	}

	private TreeMap<Integer, ArrayList<LinkedList<String>>> forwardInteractionLoop(
			LinkedList<String> bufferLList, 
			TreeMap<Integer, ArrayList<LinkedList<String>>> bufferMap) {

		int midPoint = bufferLList.size()/2;
		for(int j=midPoint;j<=bufferLList.size();j++){
			int currIndex = j;
			LinkedList<String> tier1BufferLList = new LinkedList<>(bufferLList);
			ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>();
			if(bufferMap.containsKey(currIndex)){
				tier1BufferList = bufferMap.get(currIndex);
			}
			for(int k=j;k<tier1BufferLList.size();k++){
				tier1BufferLList.set(k, "0-0");
			}
			tier1BufferList.add(tier1BufferLList);
			bufferMap.put(currIndex, tier1BufferList);
		}
		
		return(bufferMap);
	}
	
	private TreeMap<Integer, ArrayList<LinkedList<String>>> backwardInteractionLoop(
			LinkedList<String> bufferLList, 
			TreeMap<Integer, ArrayList<LinkedList<String>>> bufferMap) {

		int midPoint = bufferLList.size()/2;
		for(int j=midPoint;j<=bufferLList.size();j++){
			int currIndex = j;
			LinkedList<String> tier1BufferLList = new LinkedList<>(bufferLList);
			ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>();
			if(bufferMap.containsKey(currIndex)){
				tier1BufferList = bufferMap.get(currIndex);
			}
			int endPoint = bufferLList.size()-j;
			for(int k=0;k<endPoint;k++){
				tier1BufferLList.set(k, "0-0");
			}
			tier1BufferList.add(tier1BufferLList);
			bufferMap.put(currIndex, tier1BufferList);
		}
		
		return(bufferMap);
	}
	
	

	private void differentialEdgeTemplates(LinkedList<String> bufferList, Integer classInstance) {

		TreeMap<Integer, ArrayList<LinkedList<String>>> tier1BufferResultMap = new TreeMap<>();
		tier1BufferResultMap = forwardInteractionLoop(bufferList, tier1BufferResultMap);
		tier1BufferResultMap = backwardInteractionLoop(bufferList, tier1BufferResultMap);
		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier1Itr = 
				tier1BufferResultMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = tier1Itr.next();
			TreeMap<Integer, HashSet<LinkedList<String>>> tier1BufferMap = new TreeMap<>();
			if(differentialTemplate.containsKey(tier1MapValue.getKey())){
				tier1BufferMap = differentialTemplate.get(tier1MapValue.getKey());
			}
			HashSet<LinkedList<String>> tier1BufferSet = new HashSet<>();
			if(tier1BufferMap.containsKey(classInstance)){
				tier1BufferSet = tier1BufferMap.get(classInstance);
			}
			tier1BufferSet.addAll(tier1MapValue.getValue());
			tier1BufferMap.put(classInstance, tier1BufferSet);
			differentialTemplate.put(tier1MapValue.getKey(), tier1BufferMap);
		}
	}
	
	private ArrayList<LinkedList<String>> dropFeatures(ArrayList<LinkedList<String>> bufferList) {

		int dropLimit = (int) Math.round((0.10*bufferList.size()));
		ArrayList<Integer> indexArray = (ArrayList<Integer>) IntStream.range(0, bufferList.size())
				.boxed().collect(Collectors.toList());
		TreeSet<Integer> removeIndex = new TreeSet<>();
		Random numbGen = new Random();
		while(removeIndex.size() <= dropLimit){
			int randomNumber = numbGen.nextInt(bufferList.size());
			removeIndex.add(randomNumber);
		}
		indexArray = (ArrayList<Integer>) indexArray.stream()
				.filter((indexVal) -> !removeIndex.contains(indexVal)).collect(Collectors.toList());
		ArrayList<LinkedList<String>> tier1BufferResultList = new ArrayList<>(
				indexArray.stream().map((currIndex) -> bufferList.get(currIndex))
				.collect(Collectors.toList()));
		
		return(tier1BufferResultList);
		
	}
	
	private Double call_JointConditionalProbability(
			LinkedList<String> bufferLList, Integer classType, Integer variantIndex) {

		//System.out.println("\tb4>"+bufferLList);
		bufferLList = new LinkedList<>(bufferLList.stream()
				.filter((currVal) -> !currVal.equals("0-0")).collect(Collectors.toList()));
		//System.out.println("\t"+bufferLList);
		Double conditionedSequenceScore = new Double(1).doubleValue();
		/**
		for(int i=bufferLList.size();i>0;i--){
			conditionedSequenceScore = (conditionedSequenceScore * 
					(scoreFeatureEdgeVariants(new LinkedList<>(
							bufferLList.subList(0, i)), classType)));
		}**/
		conditionedSequenceScore = (conditionedSequenceScore * 
				(scoreFeatureEdgeVariants(new LinkedList<>(
						bufferLList.subList(0, bufferLList.size())), classType)));
		if(Double.compare(conditionedSequenceScore, new Double(0).doubleValue()) == 0){
			System.out.println("\t>>"+bufferLList+"\t"+variantIndex+"\t"+classType);
			System.exit(0);
		}
		conditionedSequenceScore = Math.pow(variantIndex.doubleValue(), conditionedSequenceScore);
		//conditionedSequenceScore = classType.doubleValue()*conditionedSequenceScore;
		//conditionedSequenceScore = Math.exp(conditionedSequenceScore*(classType));
	
		return(conditionedSequenceScore);
	}
	
	private TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> normalizeTemplateScores(
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferMap,
			HashSet<LinkedList<String>> eliminationList) {

		Iterator<LinkedList<String>> tier1Itr = eliminationList.iterator();
		while(tier1Itr.hasNext()){
			LinkedList<String> tier1LList = tier1Itr.next();
			Iterator<Integer> tier2Itr = bufferMap.keySet().iterator();
			Double normScore = new Double(0).doubleValue();
			while(tier2Itr.hasNext()){
				Integer tier2IntegerValue = tier2Itr.next();
				normScore = normScore + bufferMap.get(tier2IntegerValue).get(tier1LList);
			}
			normScore = normScore/bufferMap.size();
			
			LinkedHashMap<LinkedList<String>, Double> tier2BufferMap = new LinkedHashMap<>();
			tier2Itr = bufferMap.keySet().iterator();
			while(tier2Itr.hasNext()){
				Integer tier2IntegerValue = tier2Itr.next();
				if(bufferMap.containsKey(tier2IntegerValue)){
					tier2BufferMap = bufferMap.get(tier2IntegerValue);
				}
				if(tier2BufferMap.containsKey(tier1LList)){
					tier2BufferMap.put(tier1LList, normScore);
				}
				bufferMap.put(tier2IntegerValue, tier2BufferMap);
			}
		}
		
		return(bufferMap);
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
		
		System.out.println("\t"+tier1BufferMap);
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
		
		System.out.println("\t"+tier1BufferMap);
		return(tier1BufferMap);
		
	}
	
	private TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> dropCommonFeatures(
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> bufferMap,
			HashSet<LinkedList<String>> eliminationList) {

		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier1MapValue = 
					tier1Itr.next();
			if(tier1MapValue.getKey() == 1){
				int dropSize = (int) (tier1MapValue.getValue().size()*0.10);
				Iterator<Map.Entry<LinkedList<String>, Double>> tier2Itr = 
						tier1MapValue.getValue().entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<LinkedList<String>, Double> tier2MapValue = tier2Itr.next();
					//if(eliminationList.contains(tier2MapValue.getKey())){
					if(dropSize > 0){
						tier2Itr.remove();
						dropSize--;
					}
				}
			}
		}
		return (bufferMap);
	}
	
	
	public TreeMap<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>>> 
	generateFeatureTemplate(
			TreeMap<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, 
			LinkedList<Double>>>> iconFeatureMap) {

		TreeMap<Integer, ArrayList<LinkedList<String>>> tier1BufferMap = new TreeMap<>();
		TreeMap<Integer, Double> classWeightMap = 
				softmaxVariantScoring(new ArrayList<>(invariantEdgeFeatureBundle.keySet()));
				//sigmoidVariantScoring(new ArrayList<>(invariantEdgeFeatureBundle.keySet()));
		
		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier1Itr = 
				invariantEdgeFeatureBundle.entrySet().iterator();
		while(tier1Itr.hasNext()){
			ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>();
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = tier1Itr.next();
			ArrayList<LinkedList<String>> tier2BufferList = tier1MapValue.getValue();
			//tier2BufferList = dropFeatures(tier2BufferList);
			Iterator<LinkedList<String>> tier2Itr = tier2BufferList.iterator();
			Integer instanceType = tier1MapValue.getKey();
			while(tier2Itr.hasNext()){
				LinkedList<String> tier2LListValue = tier2Itr.next();
				LinkedList<String> tier2BufferLList = call_relvantIconFrame(tier2LListValue);
				tier1BufferList.add(tier2BufferLList);
			}
			tier1BufferMap.put(instanceType, tier1BufferList);
		}
		
		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier2Itr = 
				tier1BufferMap.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier2MapValue = tier2Itr.next();
			Integer classInstance = tier2MapValue.getKey(); 
			Iterator<LinkedList<String>> tier3Itr = tier2MapValue.getValue().iterator();
			while(tier3Itr.hasNext()){
				LinkedList<String> tier3MapValue = tier3Itr.next();
				/**
				 * Sub context counts for JCP
				 */
				extractSubContexts(tier3MapValue, classInstance);
				/**
				 * Variable template for Context Edge
				 */
				differentialEdgeTemplates(tier3MapValue, classInstance);
			}
		}
		
		/**
		 * JCP score for differential templates
		 */
		TreeMap<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier2BufferMap =
				new TreeMap<>();
		Iterator<Map.Entry<Integer, TreeMap<Integer, HashSet<LinkedList<String>>>>> tier4Itr = 
				differentialTemplate.entrySet().iterator();
		while(tier4Itr.hasNext()){
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier3BufferMap = 
					new TreeMap<>();
			Map.Entry<Integer, TreeMap<Integer, HashSet<LinkedList<String>>>> tier4MapValue = 
					tier4Itr.next();
			Iterator<Map.Entry<Integer, HashSet<LinkedList<String>>>> tier5Itr = 
					tier4MapValue.getValue().entrySet().iterator();
			while(tier5Itr.hasNext()){
				LinkedHashMap<LinkedList<String>, Double> tier4BufferMap = new LinkedHashMap<>();
				Map.Entry<Integer, HashSet<LinkedList<String>>> tier5MapValue = tier5Itr.next();
				Iterator<LinkedList<String>> tier6Itr = tier5MapValue.getValue().iterator();
				while(tier6Itr.hasNext()){
					LinkedList<String> tier6LList = tier6Itr.next();
					Double jcpScore = call_JointConditionalProbability(
							tier6LList, tier5MapValue.getKey(), tier4MapValue.getKey());
					jcpScore = (classWeightMap.get(tier5MapValue.getKey())*jcpScore);
					tier4BufferMap.put(tier6LList, jcpScore);
					if(jcpScore.isNaN()){
						System.out.println("Nan error");
						System.exit(0);
					}
				}
				tier3BufferMap.put(tier5MapValue.getKey(), tier4BufferMap);
			}
			tier2BufferMap.put(tier4MapValue.getKey(), tier3BufferMap);
		}

		/**
		 * Softmax scoring and normalization
		 */
		TreeMap<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier9BufferMap =
				new TreeMap<>();
		Iterator<Map.Entry<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>>>> tier7Itr = 
				tier2BufferMap.entrySet().iterator();
		while(tier7Itr.hasNext()){
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier7BufferMap = 
					new TreeMap<>();
			HashSet<LinkedList<String>> eliminationList = new HashSet<>();
			Map.Entry<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier7MapValue = 
					tier7Itr.next();
			Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier8Itr = 
					tier7MapValue.getValue().entrySet().iterator();
			while(tier8Itr.hasNext()){
				Map.Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier8MapValue = 
						tier8Itr.next();
				LinkedHashMap<LinkedList<String>, Double> tier8BufferMap = tier8MapValue.getValue();
				ArrayList<LinkedList<String>> redundantKeyMap = new ArrayList<>(
						tier8BufferMap.keySet());
				Double classMaxScore = tier8BufferMap.values().stream()
						.reduce((x,y) -> x+y).get().doubleValue();
				tier8BufferMap = new LinkedHashMap<>(tier8BufferMap.entrySet().stream()
						.collect(Collectors.toMap((currVal) -> currVal.getKey(), 
								(currVal) -> currVal.getValue())));
				tier7BufferMap.put(tier8MapValue.getKey(), tier8BufferMap);
				/**
				 * Gather differential templates
				 */
				if(eliminationList.isEmpty()){
					eliminationList.addAll(redundantKeyMap);
				}else{
					eliminationList = new HashSet<>(eliminationList.stream()
							.filter((currList) -> redundantKeyMap.contains(currList))
							.collect(Collectors.toList()));
				}
			}
			/**
			 * Template Overlap Normalization
			 */
			System.out.println("\t overlap>>"+tier7MapValue.getKey()+"\t"+eliminationList.size());
			//tier7BufferMap = normalizeTemplateScores(tier7BufferMap, eliminationList);
			//tier7BufferMap = dropCommonFeatures(tier7BufferMap, eliminationList);
			tier9BufferMap.put(tier7MapValue.getKey(), tier7BufferMap);
		}
		/**
		Iterator<Map.Entry<LinkedList<String>, Double>> t1Itr = 
				tier9BufferMap.get(2).get(1).entrySet().iterator();
		while(t1Itr.hasNext()){
			Map.Entry<LinkedList<String>, Double> t1V = t1Itr.next();
			System.out.println("\t>"+t1V.getKey()+"\t"+t1V.getValue());
					
		}
		System.exit(0);**/
		
		return(tier9BufferMap);
	}
	
}


