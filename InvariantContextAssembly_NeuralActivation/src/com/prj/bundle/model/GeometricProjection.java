/**
 * 
 */
package com.prj.bundle.model;

import java.io.IOException;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.print.DocFlavor.STRING;

import com.prj.bundle.metrics.AlgebraicInvarianceEval;
import com.prj.bundle.wrapper.DifferentialContextAttributes;
import com.prj.bundle.wrapper.OriginalLexemeAttributes;
import com.prj.bundle.wrapper.PosLexemeAttributes;

/**
 * @author iasl
 *
 */
public class GeometricProjection{

	private TreeMap<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> posContextFrame;
	private TreeMap<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> orgContextFrame;
	private HashMap<LinkedList<String>, Integer> subContextPool;
	private TreeMap<Integer, ArrayList<LinkedList<String>>> posVariantFrame;
	private TreeMap<Integer, ArrayList<LinkedList<String>>> orgLexemeVariantFrame;
	private HashMap<LinkedList<String>, Integer> subVariantPool;
	private Integer referenceContextSize;
	private LinkedHashMap<String, Double> posTagAppraiser;
	private LinkedHashMap<String, Double> orgLexemeAppraiser;
	public HashMap<String, Integer> contextPositionUpdateMap;
	private TreeMap<Integer, LinkedHashMap<Integer, 
	HashMap<LinkedList<String>, LinkedList<Double>>>> invariantCluster;
	
	/**
	 * 
	 */
	public GeometricProjection() {

	}

	public GeometricProjection(PosLexemeAttributes posLexInstance, 
			OriginalLexemeAttributes orgLexInstance, Integer seedFrame, 
			LinkedHashMap<String, Double> posAppraiser, 
			LinkedHashMap<String, Double> orgAppraiser) {
		
		posContextFrame = posLexInstance.getPosContextFrame();
		orgContextFrame = orgLexInstance.getOrgLexemeContextFrame();
		subContextPool = posLexInstance.getSubContextPool();
		posVariantFrame = posLexInstance.getPosVariantFrame();
		orgLexemeVariantFrame = orgLexInstance.getOrgLexemeVariantFrame();
		subVariantPool = posLexInstance.getSubVariantPool();
		referenceContextSize = seedFrame;
		invariantCluster = new TreeMap<>();
		posTagAppraiser = new LinkedHashMap<>(posAppraiser);
		orgLexemeAppraiser = new LinkedHashMap<>(orgAppraiser);
		contextPositionUpdateMap = new LinkedHashMap<>();
	}
	
	public boolean triggerEventPresent(LinkedList<String> bufferLList) {

		boolean retVal = false;
		if(bufferLList.stream().filter((token) -> token.matches("TRIGGERPRI"))
				.collect(Collectors.toList()).size() > 0){
			retVal = true;
		}
		/**
		Iterator<String> tier1Itr = bufferLList.iterator();
		while(tier1Itr.hasNext()){
			String tier1StringValue = tier1Itr.next();
			if((tier1StringValue.matches("TRIGGERPRI"))){
				retVal = true;
				break;
			}
		}**/
		return(retVal);
	}
	
	private double contextFrequencyAccounts(List<String> bufferList, boolean variantModel) {
		
		double returnVal = 0;
		if(variantModel){
			if(subVariantPool.containsKey(bufferList)){
				returnVal = subVariantPool.get(bufferList);
			}
		}else{
			if(subContextPool.containsKey(bufferList)){
				returnVal = subContextPool.get(bufferList);
			}
		}
		return(returnVal);
	}
	
	private double marginalContextFrequencyAccount(List<String> bufferList) {
		
		double returnVal = 0;
		if(subContextPool.containsKey(bufferList)){
			returnVal = subContextPool.get(bufferList);
		}
		returnVal = (returnVal/
				subContextPool.values().stream().reduce(0, (prev,curr)-> prev+curr));
		return(returnVal);
		
	}

	
	private double contextPosTagWeightAccounts(List<String> bufferList) {
		
		double returnVal = 0;
		Iterator<String> tier1Itr = bufferList.iterator();
		while(tier1Itr.hasNext()){
			String tier1StrVal = tier1Itr.next();
			if(posTagAppraiser.containsKey(tier1StrVal)){
				if(tier1StrVal.equals("DT")){
					returnVal = 1;
				}else{
					returnVal = (returnVal + posTagAppraiser.get(tier1StrVal));
				}
			}
		}
		if(returnVal == 0){
			returnVal = posTagAppraiser.get("NA#");
		}
		
		return (returnVal);
	}
	
	private double contextOrgLexemeWeightAccounts(List<String> bufferList) {

		double returnVal = 0;
		/**
		Iterator<String> tier1Itr = bufferList.iterator();
		ArrayList<Double> tier1ResultBufferList = new ArrayList<>();
		while(tier1Itr.hasNext()){
			String tier1StrVal = tier1Itr.next();
			if(orgLexemeAppraiser.containsKey(tier1StrVal)){
				tier1ResultBufferList.add(orgLexemeAppraiser.get(tier1StrVal));
			}else{
				tier1ResultBufferList.add(orgLexemeAppraiser.get("NA#"));
			}
		}
		Collections.sort(tier1ResultBufferList, Collections.reverseOrder());
		for(int i=0;i<referenceContextSize;i++){
			returnVal = returnVal+tier1ResultBufferList.get(i);
		}
		*/
		
		int triggerIndex = bufferList.indexOf("TRIGGERPRI");
		double weightBase = (1/referenceContextSize);
		double indexWeight = 0;
		for(int i=0;i<bufferList.size();i++){
			if(i == triggerIndex){
				indexWeight = 1;
			}else if(i < triggerIndex){
				indexWeight = ((triggerIndex-i)*weightBase);
			}else if(i > triggerIndex){
				indexWeight = 1+((i-triggerIndex)*weightBase);
			}
			String tier1StrVal = bufferList.get(i);;
			if(orgLexemeAppraiser.containsKey(tier1StrVal)){
				returnVal = returnVal + 
						(indexWeight * orgLexemeAppraiser.get(tier1StrVal));
			}else{
				returnVal = returnVal + 
						(indexWeight * orgLexemeAppraiser.get("NA#"));
			}
		}
		return (returnVal);
	}
	
	
	/**
	 * Joint conditional probability for sequence contexts
	 * Marginal sequence occurrence probability for single words
	 * @param posTagBufferList
	 * @param orgLexemeBufferList 
	 * @param variantModel 
	 * @return 
	 */
	private double scoreContextDifferentials(List<String> posTagBufferList, 
			LinkedList<String> orgLexemeBufferList, boolean variantModel) {

		double avgInformationScore = 1;
		for(int i=posTagBufferList.size();i>0;i--){
			double numScore = 0, denScore = 0, condProbScore = 1, numWeightScore = 0, 
					denWeightScore = 0;
			double informationBit = 0;
			numScore = contextFrequencyAccounts(posTagBufferList.subList(0, i), variantModel);
			double numPosWeightScore = 1;
			numPosWeightScore = contextPosTagWeightAccounts(posTagBufferList.subList(0, i));
			double numOrgWeightScore = 1;
			numOrgWeightScore = contextOrgLexemeWeightAccounts(orgLexemeBufferList.subList(0, i));
			//numWeightScore = ((0.3*numPosWeightScore)+(0.7*numOrgWeightScore));
			if(!variantModel){
				numWeightScore = numOrgWeightScore;
			}else{
				numWeightScore = 1;
			}
			numScore = (numScore * numWeightScore);
			//System.out.println("\t"+numOrgWeightScore+"\t"+numPosWeightScore+"");
			if((i-1)>0){
				denScore = contextFrequencyAccounts(posTagBufferList.subList(0, i-1), variantModel);
				double denPosWeightScore = 1;
				denPosWeightScore = contextPosTagWeightAccounts(posTagBufferList.subList(0, i-1));
				double denOrgWeightScore = 1;
				denOrgWeightScore = contextOrgLexemeWeightAccounts(orgLexemeBufferList.subList(0, i));
				//denWeightScore = ((0.3*denPosWeightScore)+(0.7*denOrgWeightScore));
				if(!variantModel){
					denWeightScore = denOrgWeightScore;
				}else{
					denWeightScore = 1;
				}
				denScore = (denScore * denWeightScore);
				/**
				if(numScore == denScore){
					numScore = numWeightScore;
					denScore = contextWeightAccounts(bufferList.subList(0, i-1));
				}
				**/
				if(denScore > 0){
					condProbScore = (numScore/denScore);
				}else{
					System.err.println(
							"scoreContextDifferentials() ~ GeometricProjection, "
							+ "Context occurrence error "+ posTagBufferList.subList(0, i-1));
					condProbScore = 0;
				}
			}else{
				condProbScore = 1; 
				//condProbScore =  marginalContextFrequencyAccount(bufferList.subList(0, i));
			}
			/**
			 * Mutual Information
			 */
			informationBit = (Math.log(condProbScore)/Math.log(referenceContextSize-1));
			avgInformationScore = avgInformationScore - (condProbScore * informationBit);
			
			/**
			System.out.println("\t"+posTagBufferList.subList(0, i)+"\t"+posTagBufferList.subList(0, i-1));
			System.out.println("\t"+orgLexemeBufferList.subList(0, i)+"\t"+orgLexemeBufferList.subList(0, i-1));
			System.out.println("numScore\t"+numScore+"\tdenScore\t"+denScore);
			System.out.println("score\t"+condProbScore);**/
		}
		
		//System.out.println("avgInformationScore\t"+avgInformationScore);
		if(avgInformationScore == 0.0){
			System.out.println(posTagBufferList);
		}
		return avgInformationScore;
	}
	

	private TreeMap<Integer, Double> generateContextDifferentials(
			LinkedList<String> posTagBufferLList, LinkedList<String> orgLexemeBufferLList) {
		
		
		boolean variantModel = true;
		if(triggerEventPresent(posTagBufferLList)){
			variantModel = false;
		}
		double conditionedContextScore = 0;
		TreeMap<Integer, Double> tier1BufferMap = new TreeMap<>();
		for(int i=posTagBufferLList.size();i>0;i--){
			LinkedList<String> tier1BufferLList = new LinkedList<>(posTagBufferLList.subList(0, i));
			LinkedList<String> tier2BufferLList = new LinkedList<>(orgLexemeBufferLList.subList(0, i));
			conditionedContextScore = scoreContextDifferentials(
					tier1BufferLList, tier2BufferLList, variantModel);
			tier1BufferMap.put(i, conditionedContextScore);
		}
		return(tier1BufferMap);
	}
	
	private TreeMap<Integer, ArrayList<DifferentialContextAttributes>> unpackContextFeatures(
			TreeMap<Integer, HashMap<Integer, 
			LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>>> bufferMap) {

		TreeMap<Integer, ArrayList<DifferentialContextAttributes>> tier1ResultBufferMap = 
				new TreeMap<>();
		Iterator<Map.Entry<Integer,HashMap<Integer, LinkedHashMap<LinkedList<String>, 
		LinkedHashMap<String, Double>>>>> tier1Itr = bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer,HashMap<Integer, LinkedHashMap<LinkedList<String>, 
			LinkedHashMap<String, Double>>>> tier1MapValue = 
					tier1Itr.next();
			Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, 
			LinkedHashMap<String, Double>>>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<Integer, LinkedHashMap<LinkedList<String>, 
				LinkedHashMap<String, Double>>> tier2MapValue = 
						tier2Itr.next();
				Iterator<Map.Entry<LinkedList<String>, LinkedHashMap<String, Double>>> tier3Itr = 
						tier2MapValue.getValue().entrySet().iterator();
				ArrayList<DifferentialContextAttributes> tier1ResultBufferList = new ArrayList<>();
				if(tier1ResultBufferMap.containsKey(tier2MapValue.getKey())){
					tier1ResultBufferList = tier1ResultBufferMap.get(tier2MapValue.getKey());
				}
				while(tier3Itr.hasNext()){
					Map.Entry<LinkedList<String>, LinkedHashMap<String, Double>> tier3MapValue = 
							tier3Itr.next();
					tier1ResultBufferList.add(new DifferentialContextAttributes(tier3MapValue));
				}
				tier1ResultBufferMap.put(tier2MapValue.getKey(), tier1ResultBufferList);
			}
		}
		
		/**
		Iterator<Map.Entry<Integer, ArrayList<DifferentialContextAttributes>>> t1 = 
				tier1ResultBufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, ArrayList<DifferentialContextAttributes>> t1V = t1.next();
			System.out.println("\n\t"+t1V.getKey());
			Iterator<DifferentialContextAttributes> t2 = t1V.getValue().iterator();
			while (t2.hasNext()) {
				DifferentialContextAttributes t2V = t2.next();
				System.out.println("\t"+t2V.getReferenceFeature()+"\n\t"+t2V.getDifferentialAttributes());
			}
		}
		System.exit(0);**/
		
		return(tier1ResultBufferMap);
		
	}
	
	private int getGenericPosContextIndex(LinkedList<String> bufferLList) {
		
		int index = 0;
		String currentPosTag = bufferLList.get(0);
		if(contextPositionUpdateMap.isEmpty()){
			index = referenceContextSize;
		}else{
			if(!contextPositionUpdateMap.containsKey(currentPosTag)){
				Iterator<Map.Entry<String, Integer>> tier1Itr = 
						contextPositionUpdateMap.entrySet().iterator();
				int fillSize = 0;
				while(tier1Itr.hasNext()){
					Map.Entry<String,Integer> tier1MapValue = tier1Itr.next();
					if(fillSize == contextPositionUpdateMap.size()-1){
						index = tier1MapValue.getValue()+1;
					}
					fillSize++;
				}
			}else{
				index = contextPositionUpdateMap.get(currentPosTag);
			}
		}
		contextPositionUpdateMap.putIfAbsent(currentPosTag, index);
		
		return(index);
	}
	
	private TreeMap<Integer, ArrayList<DifferentialContextAttributes>> segmentationUsingInitialPosTag(
			LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>> bufferHashMap, 
			TreeMap<Integer, ArrayList<DifferentialContextAttributes>> resultBufferMap) {
		
		Iterator<Map.Entry<LinkedList<String>, LinkedHashMap<String, Double>>> tier1Itr = 
				bufferHashMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<LinkedList<String>, LinkedHashMap<String, Double>> tier1MapValue = 
					tier1Itr.next();
			Integer contextIndex = getGenericPosContextIndex(tier1MapValue.getKey());
			ArrayList<DifferentialContextAttributes> tier1ResultBufferList = new ArrayList<>();
			if(resultBufferMap.containsKey(contextIndex)){
				tier1ResultBufferList = resultBufferMap.get(contextIndex);
			}
			tier1ResultBufferList.add(new DifferentialContextAttributes(tier1MapValue));
			resultBufferMap.put(contextIndex, tier1ResultBufferList);
		}
		return(resultBufferMap);
	}

	private TreeMap<Integer, ArrayList<DifferentialContextAttributes>> unpackVariantFeatures(
			HashMap<Integer, LinkedHashMap<LinkedList<String>, 
			LinkedHashMap<String, Double>>> bufferMap) {

		TreeMap<Integer, ArrayList<DifferentialContextAttributes>> tier1ResultBufferMap = 
				new TreeMap<>();
		Iterator<Map.Entry<Integer,LinkedHashMap<LinkedList<String>, 
		LinkedHashMap<String, Double>>>> tier1Itr = bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer,LinkedHashMap<LinkedList<String>, 
			LinkedHashMap<String, Double>>> tier1MapValue = tier1Itr.next();
			tier1ResultBufferMap = segmentationUsingInitialPosTag(
					tier1MapValue.getValue(), tier1ResultBufferMap);
		}
		
		/**
		Iterator<Map.Entry<String, Integer>> t2Itr = 
				contextPositionUpdateMap.entrySet().iterator();
		while(t2Itr.hasNext()){
			Map.Entry<String, Integer> t2V = t2Itr.next();
			System.out.println("\t"+t2V.getKey()+"\t"+t2V.getValue());
		}
		System.exit(0);**/

		/**
		Iterator<Map.Entry<Integer, ArrayList<DifferentialContextAttributes>>> t1 = 
				tier1ResultBufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, ArrayList<DifferentialContextAttributes>> t1V = t1.next();
			System.out.println("\n\t"+t1V.getKey());
			Iterator<DifferentialContextAttributes> t2 = t1V.getValue().iterator();
			while (t2.hasNext()) {
				DifferentialContextAttributes t2V = t2.next();
				System.out.println("\t"+t2V.getReferenceFeature()+"\n\t"+t2V.getDifferentialAttributes());
			}
		}
		System.exit(0);**/
		
		return (tier1ResultBufferMap);
	}

	private LinkedList<Entry<Integer, Double>> compareMapEntries(
			LinkedList<Entry<Integer, Double>> linkedList) {
		
		Collections.sort(linkedList, new Comparator<Map.Entry<Integer, Double>>() {
			// ascending order
			@Override
			public int compare(Entry<Integer, Double> currItem, Entry<Integer, Double> nextItem) {
				return (currItem.getValue().compareTo(nextItem.getValue()));
			}
		});
		return(linkedList);
	}

	private TreeMap<Integer, LinkedHashMap<String, Integer>> calculateMarginOfError(
			ArrayList<DifferentialContextAttributes> bufferList) 
			throws IOException {
		
		TreeMap<Integer, LinkedHashMap<String, Integer>> tier1ResultBufferMap = new TreeMap<>();
		Iterator<DifferentialContextAttributes> tier1Itr = bufferList.iterator();
		while(tier1Itr.hasNext()){
			Iterator<String> tier2Itr = 
					tier1Itr.next().getReferenceFeature().iterator();
			int index=0;
			while(tier2Itr.hasNext()){
				String currentToken = tier2Itr.next();
				LinkedHashMap<String, Integer> tier2BufferMap = new LinkedHashMap<>();
				if(tier1ResultBufferMap.containsKey(index)){
					tier2BufferMap = tier1ResultBufferMap.get(index);
				}
				tier2BufferMap = new FrameFractioner()
						.updateDictionaryVolume(tier2BufferMap, currentToken);
				tier1ResultBufferMap.put(index, tier2BufferMap);
				index++;
			}
		}
		
		return(tier1ResultBufferMap);
	}
	
	/**
	 * Collapse the size > 1 clusters into more generic patterns  
	 * @param bufferMap
	 * @return
	 */
	private HashMap<LinkedList<String>, LinkedList<Double>> identifyGeneralizedPattern(
			HashMap<LinkedList<String>, LinkedList<Double>> bufferMap) {
		
		LinkedHashMap<Integer, HashSet<String>> tier1BufferMap = new LinkedHashMap<>();
		LinkedHashMap<Integer, HashSet<Double>> tier2BufferMap = new LinkedHashMap<>();
		Iterator<LinkedList<String>> tier1Itr = bufferMap.keySet().iterator();
		while(tier1Itr.hasNext()){
			LinkedList<String> tier1LLValue = tier1Itr.next();
			for(int i=0;i<tier1LLValue.size();i++){
				HashSet<String> tier1BufferSet = new HashSet<>();
				if(tier1BufferMap.containsKey(i)){
					tier1BufferSet = tier1BufferMap.get(i);
				}
				tier1BufferSet.add(tier1LLValue.get(i));
				tier1BufferMap.put(i, tier1BufferSet);
				
				HashSet<Double> tier2BufferSet = new HashSet<>();
				if(tier2BufferMap.containsKey(i)){
					tier2BufferSet = tier2BufferMap.get(i);
				}
				tier2BufferSet.add(bufferMap.get(tier1LLValue).get(i));
				tier2BufferMap.put(i, tier2BufferSet);
			}
		}
		
		
		LinkedList<String> tier1ResultBufferLList = new LinkedList<>();
		LinkedList<Double> tier2ResultBufferLList = new LinkedList<>();
		Iterator<Map.Entry<Integer, HashSet<String>>> tier2Itr = tier1BufferMap.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<Integer, HashSet<String>> tier2MapValue = tier2Itr.next();
			LinkedList<Double> tier3BufferLList = new LinkedList<>(
					tier2BufferMap.get(tier2MapValue.getKey()));
			if(tier2MapValue.getValue().size() == 1){
				tier1ResultBufferLList.add(tier2MapValue.getValue().iterator().next());
			}else{
				tier1ResultBufferLList.add("#");
			}
			tier2ResultBufferLList.add(
					(tier3BufferLList.stream()
							.reduce((prev,curr)->prev+curr).get()
							/tier3BufferLList.size()));
		}
		bufferMap.put(tier1ResultBufferLList, tier2ResultBufferLList);
		
		return(bufferMap);
	}

	private void initiateInvariantClustering(AlgebraicInvarianceEval invaraiantInstance, 
			ArrayList<DifferentialContextAttributes> bufferList, 
			int seedIndex, Integer contextIndex, 
			TreeMap<Integer, LinkedHashMap<String, Integer>> bufferMap) {
		
		if(bufferList.size() > 0){
			LinkedList<String> a = new LinkedList<>(Arrays.asList("TRIGGERPRI", "VBP", "JJ","NN", "NN", "NNS", "CC"));
			//System.out.println("\t"+bufferList.size());
			TreeMap<Integer, Double> tier1ResultBufferMap = new TreeMap<>();
			for(int i=seedIndex;i<bufferList.size();i++){
				tier1ResultBufferMap.put(i, invaraiantInstance.compareInvarianceQuotient(
						bufferList.get(seedIndex), bufferList.get(i), bufferMap));
				/**
				if(a.equals(bufferList.get(i).getReferenceFeature())){
					System.out.println(">>"+tier1ResultBufferMap.get(i)+"\t>>"+a);
					System.exit(0);
				}**/
			}
			
			// set boundary condition for pattern reduction
			double errorThreshold = 0.10;
			if(tier1ResultBufferMap.get(seedIndex) > 0.0){
				//System.out.println("\n\t WRONG MAPPED"+tier1ResultBufferMap.get(seedIndex));
				errorThreshold = 
						tier1ResultBufferMap.get(seedIndex)
						+(errorThreshold*tier1ResultBufferMap.get(seedIndex));	
			}else if(tier1ResultBufferMap.get(seedIndex) == 0.0){
				//System.out.println("\n\t mapped in another"+tier1ResultBufferMap.get(seedIndex));
				errorThreshold = 
						tier1ResultBufferMap.get(seedIndex)+(errorThreshold);
			}
			double boundaryThreshold = errorThreshold;
			
			/*Iterator<Map.Entry<Integer, Double>> t1 = tier1ResultBufferMap.entrySet().iterator();
			while(t1.hasNext()){
				Map.Entry<Integer, Double> t1V = t1.next();
				System.out.println("\t"+bufferList.get(t1V.getKey()).getReferenceFeature()+"\t"+t1V.getValue());
			}*/
			//System.exit(0);
			
			/**
			 * Update the invariant feature cluster
			 */
			List<Map.Entry<Integer, Double>> tier2ResultBufferMap = 
					compareMapEntries(new LinkedList<>(tier1ResultBufferMap.entrySet()));
			List<Map.Entry<Integer, Double>> tier3ResultBufferMap = 
					new LinkedList<>(tier2ResultBufferMap.stream()
					.filter(entryVal -> ((entryVal.getValue() <= boundaryThreshold) 
							&& (entryVal.getValue() >= new Double(0))))
					.collect(Collectors.toList()));
			LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>> tier4ResultBufferMap = 
					new LinkedHashMap<>();
			HashMap<LinkedList<String>, LinkedList<Double>> tier5ResultBufferMap = new HashMap<>();
			Iterator<Map.Entry<Integer, Double>> tier1Itr = tier3ResultBufferMap.iterator();
			while(tier1Itr.hasNext()){
				Map.Entry<Integer, Double> tier1MapValue = tier1Itr.next();
				LinkedList<Double> tier1BufferLList = new LinkedList<>(
						bufferList.get(tier1MapValue.getKey()).getDifferentialAttributes().values());
				//tier1BufferLList.add(tier1MapValue.getValue());
				/**
				if(bufferList.get(tier1MapValue.getKey()).getReferenceFeature().equals(a)){
					System.out.println(">>>"+bufferList.get(tier1MapValue.getKey()).getReferenceFeature());
				}**/
				tier5ResultBufferMap.put(
						bufferList.get(tier1MapValue.getKey()).getReferenceFeature(), tier1BufferLList);
				//tier1ResultBufferList.add(bufferList.get(tier1MapValue.getKey()).getReferenceFeature());
				//System.out.println("\t"+tier1MapValue.getKey()+"\t"+bufferList.get(tier1MapValue.getKey()).getReferenceFeature()+"\t"+tier1MapValue.getValue());
			}
			/**
			 * Identify a generalized pattern
			 */
			if(tier5ResultBufferMap.size() > 1){
				tier5ResultBufferMap = identifyGeneralizedPattern(tier5ResultBufferMap);
			}
			
			if(invariantCluster.containsKey(contextIndex)){
				tier4ResultBufferMap = invariantCluster.get(contextIndex);
			}
			tier4ResultBufferMap.put(tier4ResultBufferMap.size()+1, tier5ResultBufferMap);
			invariantCluster.put(contextIndex, tier4ResultBufferMap);
			//System.out.println("\t"+tier4ResultBufferMap);
			
			/**
			 * Update  the buffer list - removing the clustered context features
			 */
			/*
			Iterator<LinkedList<String>> tier2Itr = tier5ResultBufferMap.keySet().iterator();
			while(tier2Itr.hasNext()){
				LinkedList<String> tier2ListValue = tier2Itr.next();
				bufferList = (ArrayList<DifferentialContextAttributes>)bufferList.stream()
						.filter(attributeValue -> (!attributeValue.getReferenceFeature().containsAll(tier2ListValue)))
						.collect(Collectors.toList());
				
			}*/
			Iterator<DifferentialContextAttributes> tier2Itr = bufferList.iterator();
			while(tier2Itr.hasNext()){
				DifferentialContextAttributes tier2AttributeValue = tier2Itr.next();
				Iterator<LinkedList<String>> tier3Itr = tier5ResultBufferMap.keySet().iterator();
				while(tier3Itr.hasNext()){
					LinkedList<String> tier3ItrLList = tier3Itr.next();
					if(tier3ItrLList.equals(tier2AttributeValue.getReferenceFeature())){
						tier2Itr.remove();
						break;
					}
				}
			}
			
			//System.out.println("last \t"+bufferList.size());
			//System.exit(0);
			initiateInvariantClustering(invaraiantInstance, 
					bufferList, seedIndex, contextIndex, bufferMap);
		}
	}

	public TreeMap<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> 
	learnStableInvariantGeometricTransformation() throws IOException {
	
		AlgebraicInvarianceEval invaraiantInstance = new AlgebraicInvarianceEval();
		
		/**
		 * Invariant pattern for trigger words
		 */
		Iterator<Map.Entry<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>>> tier1Itr = 
				posContextFrame.entrySet().iterator();
		TreeMap<Integer, HashMap<Integer, LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>>> tier4ResultBufferMap = 
				new TreeMap<>();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> tier1MapValue = 
					tier1Itr.next();
			//System.out.println("\t"+tier1MapValue.getKey());
			Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier2Itr = 
					tier1MapValue.getValue().entrySet().iterator();
			HashMap<Integer, LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>> tier3ResultBufferMap = 
					new HashMap<>();
			while(tier2Itr.hasNext()){
				Map.Entry<Integer, ArrayList<LinkedList<String>>> tier2MapValue = tier2Itr.next();
				//System.out.println("\t\t\t"+tier2MapValue.getKey());
				Iterator<LinkedList<String>> tier3Itr = tier2MapValue.getValue().iterator();
				LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>> tier2ResultBufferMap = 
						new LinkedHashMap<>();
				int index=0;
				while(tier3Itr.hasNext()){
					LinkedList<String> tier3ListValue = tier3Itr.next();
					LinkedList<String> tier4LList = orgContextFrame.get(tier1MapValue.getKey())
							.get(tier2MapValue.getKey()).get(index);
					TreeMap<Integer, Double> returnBufferMap = 
							generateContextDifferentials(tier3ListValue, tier4LList);
					Iterator<Map.Entry<Integer, Double>> tier4Itr = 
							returnBufferMap.entrySet().iterator();
					LinkedHashMap<String, Double> tier1ResultBufferMap = new LinkedHashMap<>();
					double refCoefX = returnBufferMap.get(referenceContextSize);
					double refCoefY = 0;
					while(tier4Itr.hasNext()){
						Map.Entry<Integer, Double> tier4MapValue = tier4Itr.next();
						refCoefY = tier4MapValue.getValue();
						String diffPair = String.valueOf(referenceContextSize)
								+"_"+String.valueOf(tier4MapValue.getKey());
						tier1ResultBufferMap.put(
								diffPair, invaraiantInstance.generatePolynomialForm(refCoefX, refCoefY));
					}
					tier2ResultBufferMap.put(tier3ListValue, tier1ResultBufferMap);
					index++;
				}
				tier3ResultBufferMap.put(tier2MapValue.getKey(), tier2ResultBufferMap);
			}
			tier4ResultBufferMap.put(tier1MapValue.getKey(), tier3ResultBufferMap);
		}
		
		/**
		Iterator<Map.Entry<Integer, HashMap<Integer, LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>>>> t1 = tier4ResultBufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, HashMap<Integer, LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>>> t1V = t1.next();
			Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>>> t2 = t1V.getValue().entrySet().iterator();
			System.out.println("\n\t****"+t1V.getKey());
			while(t2.hasNext()){
				Map.Entry<Integer, LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>> t2V = t2.next();
				Iterator<Map.Entry<LinkedList<String>, LinkedHashMap<String, Double>>> t3 = t2V.getValue().entrySet().iterator();
				System.out.println("\t\t\t"+t2V.getKey());
				while(t3.hasNext()){
					Map.Entry<LinkedList<String>, LinkedHashMap<String, Double>> t3V = t3.next();
					System.out.println("\t"+t3V.getKey()+"\t"+t3V.getValue());
				}
			}
		}
		System.exit(0);**/
		
		// reorganize the context pattern data structure according to trigger index
		TreeMap<Integer, ArrayList<DifferentialContextAttributes>> tier5ResultBufferMap = 
				unpackContextFeatures(tier4ResultBufferMap);
		
		/**
		 * initiate pattern clustering iteratively over different Trigger term index
		 */
		Iterator<Map.Entry<Integer, ArrayList<DifferentialContextAttributes>>> tier5Itr = 
				tier5ResultBufferMap.entrySet().iterator();
		while(tier5Itr.hasNext()){
			Map.Entry<Integer, ArrayList<DifferentialContextAttributes>> tier5MapValue = 
					tier5Itr.next();
			// determine variable token probability in each context position
			TreeMap<Integer, LinkedHashMap<String, Integer>> tier6ResultBufferMap = 
					calculateMarginOfError(tier5MapValue.getValue());
			// call for clustering
			initiateInvariantClustering(invaraiantInstance, 
					tier5MapValue.getValue(), 0, 
					tier5MapValue.getKey(), tier6ResultBufferMap);
		}
		
		/**
		Iterator<Entry<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>>> t1 = 
		invariantCluster.entrySet().iterator();
		while(t1.hasNext()){
			Entry<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> t1V = t1.next();
			Iterator<Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> t2 = 
					t1V.getValue().entrySet().iterator();
			while(t2.hasNext()){
				Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>> t2V= t2.next();
				System.out.println("\t"+t1V.getKey()+"\t"+t2V.getKey());
				Iterator<Map.Entry<LinkedList<String>, LinkedList<Double>>> t3 = 
						t2V.getValue().entrySet().iterator();
				while(t3.hasNext()){
					Map.Entry<LinkedList<String>, LinkedList<Double>> t3V = t3.next();
					System.out.println("\t"+t3V.getKey()+"\t"+t3V.getValue());
				}
			}
		}
		System.exit(0);**/
		
		return(invariantCluster);
		
	}

	public TreeMap<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> 
	learnDifferentialInvariantGeometricTransformation(ICONCaller iconInstance) throws IOException {
		
		AlgebraicInvarianceEval invaraiantInstance = new AlgebraicInvarianceEval();
		
		/**
		 * Invariant pattern for variable context
		 */
		
		Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> tier1Itr = 
				posVariantFrame.entrySet().iterator();
		HashMap<Integer, LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>> tier3ResultBufferMap = 
				new HashMap<>();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<String>>> tier1MapValue = tier1Itr.next();
			//System.out.println("\t"+tier1MapValue.getKey());
			LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>> tier2ResultBufferMap = 
					new LinkedHashMap<>();
			if(tier1MapValue.getValue().size() > 0){
				Iterator<LinkedList<String>> tier2Itr = tier1MapValue.getValue().iterator();
				int index=0;
				while(tier2Itr.hasNext()){
					LinkedList<String> tier2LList = tier2Itr.next();
					LinkedList<String> tier3LList = orgLexemeVariantFrame.
							get(tier1MapValue.getKey()).get(index);
					TreeMap<Integer, Double> returnBufferMap = 
							generateContextDifferentials(tier2LList, tier3LList);
					Iterator<Map.Entry<Integer, Double>> tier4Itr = 
							returnBufferMap.entrySet().iterator();
					LinkedHashMap<String, Double> tier1ResultBufferMap = new LinkedHashMap<>();
					double refCoefX = returnBufferMap.get(referenceContextSize);
					double refCoefY = 0;
					while(tier4Itr.hasNext()){
						Map.Entry<Integer, Double> tier4MapValue = tier4Itr.next();
						refCoefY = tier4MapValue.getValue();
						String diffPair = String.valueOf(referenceContextSize)
								+"_"+String.valueOf(tier4MapValue.getKey());
						tier1ResultBufferMap.put(
								diffPair, invaraiantInstance.generatePolynomialForm(refCoefX, refCoefY));
					}
					tier2ResultBufferMap.put(tier2LList, tier1ResultBufferMap);
					index++;
				}
				tier3ResultBufferMap.put(tier1MapValue.getKey(), tier2ResultBufferMap);
			}
		}
		
		/**
		Iterator<Map.Entry<Integer, LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>>> t1 = 
				tier3ResultBufferMap.entrySet().iterator();
		while(t1.hasNext()){
			Map.Entry<Integer, LinkedHashMap<LinkedList<String>, LinkedHashMap<String, Double>>> t1V = 
					t1.next();
			Iterator<Map.Entry<LinkedList<String>, LinkedHashMap<String, Double>>> t2 = 
					t1V.getValue().entrySet().iterator();
			System.out.println("\n\t****"+t1V.getKey());
			while(t2.hasNext()){
				Map.Entry<LinkedList<String>, LinkedHashMap<String, Double>> t2V = t2.next();
				System.out.println("\t"+t2V.getKey()+"\t"+t2V.getValue());
			}
		}
		System.exit(0);**/
		
		// reorganize the context pattern data structure according to trigger index
		TreeMap<Integer, ArrayList<DifferentialContextAttributes>> tier4ResultBufferMap = 
				unpackVariantFeatures(tier3ResultBufferMap);
		
		iconInstance.contextPositionUpdateMap = contextPositionUpdateMap;
		
		/**
		 * initiate pattern clustering iteratively over different Trigger term index
		 */
		Iterator<Map.Entry<Integer, ArrayList<DifferentialContextAttributes>>> tier4Itr = 
				tier4ResultBufferMap.entrySet().iterator();
		while(tier4Itr.hasNext()){
			Map.Entry<Integer, ArrayList<DifferentialContextAttributes>> tier4MapValue = 
					tier4Itr.next();
			// determine variable token probability in each context position
			TreeMap<Integer, LinkedHashMap<String, Integer>> tier5ResultBufferMap = 
					calculateMarginOfError(tier4MapValue.getValue());
			// call for clustering
			initiateInvariantClustering(invaraiantInstance, 
					tier4MapValue.getValue(), 0, 
					tier4MapValue.getKey(), tier5ResultBufferMap);
		}
		
		/**
		Iterator<Entry<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>>> t1 = 
		invariantCluster.entrySet().iterator();
		while(t1.hasNext()){
			Entry<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> t1V = t1.next();
			Iterator<Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> t2 = 
					t1V.getValue().entrySet().iterator();
			while(t2.hasNext()){
				Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>> t2V= t2.next();
				System.out.println("\t"+t1V.getKey()+"\t"+t2V.getKey());
				Iterator<Map.Entry<LinkedList<String>, LinkedList<Double>>> t3 = 
						t2V.getValue().entrySet().iterator();
				while(t3.hasNext()){
					Map.Entry<LinkedList<String>, LinkedList<Double>> t3V = t3.next();
					System.out.println("\t"+t3V.getKey()+"\t"+t3V.getValue());
				}
			}
		}
		System.exit(0);**/
		
		return(invariantCluster);
		
	}

}
