/**
 * 
 */
package com.prj.bundle.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.jmcejuela.bio.jenia.JeniaTagger;
import com.jmcejuela.bio.jenia.common.Sentence;
import com.jmcejuela.bio.jenia.common.Token;
import com.prj.bundle.wrapper.OriginalLexemeAttributes;
import com.prj.bundle.wrapper.PosLexemeAttributes;


/**
 * @author iasl
 *
 */
public class FrameFractioner{
	
	private ArrayList<String> triggerArrayList;
	private String normTriggerTerm;
	private Properties systemProperties;
	private ArrayList<String> stopWords;
	
	public FrameFractioner() throws IOException {
		
		triggerArrayList = new ArrayList<>();
		this.systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
        for(String currentTrigger : 
        	systemProperties.getProperty("triggerTerms").split("\\,")){
			triggerArrayList.add(currentTrigger);
		}
        normTriggerTerm = systemProperties.getProperty("normTriggerTerm");
        
        stopWords = new ArrayList<>();
        FileReader tier1FileRd = new FileReader(systemProperties.getProperty("stopWordPath"));
        BufferedReader tier1BuffRd = new BufferedReader(tier1FileRd);
        String currentRead = tier1BuffRd.readLine();
        while(currentRead != null){
        	stopWords.add(currentRead.trim().toLowerCase());
        	currentRead = tier1BuffRd.readLine();
        }
        tier1BuffRd.close();
	}

	private void extractSubContextFrames(LinkedList<String> bufferLList, 
			OriginalLexemeAttributes orgLexInstance) {

		for(int i=bufferLList.size();i>0;i--){
			LinkedList<String> tier1BufferLList = 
					new LinkedList<>(bufferLList.subList(0, i));
			int subFrameCount = 0;
			if(orgLexInstance.subContextPool.containsKey(tier1BufferLList)){
				subFrameCount = orgLexInstance.subContextPool.get(tier1BufferLList);
			}
			subFrameCount++;
			orgLexInstance.subContextPool.put(tier1BufferLList, subFrameCount);
		}
		
	}
	
	
	private void extractSubVariantFrames(LinkedList<String> bufferLList, 
			OriginalLexemeAttributes orgLexInstance) {

		for(int i=bufferLList.size();i>0;i--){
			LinkedList<String> tier1BufferLList = 
					new LinkedList<>(bufferLList.subList(0, i));
			int subFrameCount = 0;
			if(orgLexInstance.subVariantPool.containsKey(tier1BufferLList)){
				subFrameCount = orgLexInstance.subVariantPool.get(tier1BufferLList);
			}
			subFrameCount++;
			orgLexInstance.subVariantPool.put(tier1BufferLList, subFrameCount);
		}
		
	}
	
	private void extractSubContextFrames(LinkedList<String> bufferLList, 
			PosLexemeAttributes posLexInstance) {

		for(int i=bufferLList.size();i>0;i--){
			LinkedList<String> tier1BufferLList = 
					new LinkedList<>(bufferLList.subList(0, i));
			int subFrameCount = 0;
			if(posLexInstance.subContextPool.containsKey(tier1BufferLList)){
				subFrameCount = posLexInstance.subContextPool.get(tier1BufferLList);
			}
			subFrameCount++;
			posLexInstance.subContextPool.put(tier1BufferLList, subFrameCount);
		}
		
	}
	
	private void extractSubVariantFrames(LinkedList<String> bufferLList, 
			PosLexemeAttributes posLexInstance) {

		for(int i=bufferLList.size();i>0;i--){
			LinkedList<String> tier1BufferLList = 
					new LinkedList<>(bufferLList.subList(0, i));
			int subFrameCount = 0;
			if(posLexInstance.subVariantPool.containsKey(tier1BufferLList)){
				subFrameCount = posLexInstance.subVariantPool.get(tier1BufferLList);
			}
			subFrameCount++;
			posLexInstance.subVariantPool.put(tier1BufferLList, subFrameCount);
		}
		
	}
	
	
	private LinkedList<String> updateLexemContextSequence(LinkedList<String> bufferLList) {

		Iterator<String> tier1Itr = triggerArrayList.iterator();
		ArrayList<Integer> tier1BufferList =  (ArrayList<Integer>) 
				IntStream.range(0, bufferLList.size())
				.boxed().collect(Collectors.toList());
		while(tier1Itr.hasNext()){
			String currentTerm = tier1Itr.next();
			ArrayList<Integer> tier2BufferList = (ArrayList<Integer>) 
					tier1BufferList.stream().
					filter(index -> bufferLList.get(index).equals(currentTerm)).
					collect(Collectors.toList());
			Iterator<Integer> tier2Itr = tier2BufferList.iterator();
			String replaceToken = currentTerm.replaceAll("PROTEINT[\\d+]", "PROTEINT0");
			while(tier2Itr.hasNext()){
				bufferLList.set(tier2Itr.next(), replaceToken);
			}
		}
		return (bufferLList);
	}
	
	private LinkedList<String> updateContextSequence(LinkedList<String> bufferLList) {
		
		JeniaTagger.setModelsPath(systemProperties.getProperty("geniaModelFile"));
		Iterator<String> tier1Itr = triggerArrayList.iterator();
		ArrayList<Integer> tier1BufferList =  (ArrayList<Integer>) 
				IntStream.range(0, bufferLList.size())
				.boxed().collect(Collectors.toList());
		while(tier1Itr.hasNext()){
			String currentTerm = tier1Itr.next();
			ArrayList<Integer> tier2BufferList = (ArrayList<Integer>) 
					tier1BufferList.stream().
					filter(index -> bufferLList.get(index).equals(currentTerm)).
					collect(Collectors.toList());
			Iterator<Integer> tier2Itr = tier2BufferList.iterator();
			while(tier2Itr.hasNext()){
				String replaceToken = currentTerm.replaceAll("PROTEINT[\\d+]", "PROTEINT");
				bufferLList.set(tier2Itr.next(), replaceToken);
				/**
				Sentence baseForm = JeniaTagger.analyzeAll(currentTerm, true);
				Iterator<Token> tier3Itr = baseForm.iterator();
				while(tier3Itr.hasNext()){
					Token currentToken = tier3Itr.next();
					bufferLList.set(tier2Itr.next(), currentToken.pos);
				}**/
			}
		}
		return(bufferLList);
	}
	
	private void extractContextFrame(int startIndex, int endIndex, 
			ArrayList<String> extendedArray, int frameCount, String keyTerm, 
			int docIndex, OriginalLexemeAttributes orgLexInstance) {
		
		HashMap<Integer, ArrayList<LinkedList<String>>> tier1BufferMap = new HashMap<>();
		ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>();
		// extraction from max 0 ---> seed frame size index for trigger term
		if(orgLexInstance.orgLexemeContextFrame.containsKey(docIndex)){
			tier1BufferMap = orgLexInstance.orgLexemeContextFrame.get(docIndex);
		}
		LinkedList<String> tier1BufferLList = 
				new LinkedList<>(extendedArray.subList(startIndex, endIndex));
		
		// process the context frame to remove unwanted "trigger" term mentions
		tier1BufferLList = updateLexemContextSequence(tier1BufferLList);
		// extract sub frames 
		extractSubContextFrames(tier1BufferLList, orgLexInstance);
		
		// screen for stopwords
		/**
		Iterator<String> tier1Itr = tier1BufferLList.iterator();
		while(tier1Itr.hasNext()){
			String currToken = tier1Itr.next();
			if((stopWords.contains(currToken.toLowerCase())) 
					|| (currToken.contentEquals("@"))){
				tier1Itr.remove();
			}
		}**/
		
		// update the context map
		if(tier1BufferMap.containsKey(frameCount)){
			tier1BufferList = tier1BufferMap.get(frameCount);
		}
		tier1BufferList.add(tier1BufferLList);
		tier1BufferMap.put(frameCount, tier1BufferList);
		orgLexInstance.orgLexemeContextFrame.put(docIndex, tier1BufferMap);
	}
	
	private void extractContextFrame(int startIndex, int endIndex, 
			ArrayList<String> extendedArray, int frameCount, String keyTerm, 
			int docIndex, PosLexemeAttributes posLexInstance) {
		
		HashMap<Integer, ArrayList<LinkedList<String>>> tier1BufferMap = new HashMap<>();
		ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>();
		// extraction from max 0 ---> seed frame size index for trigger term
		if(posLexInstance.posContextFrame.containsKey(docIndex)){
			tier1BufferMap = posLexInstance.posContextFrame.get(docIndex);
		}
		LinkedList<String> tier1BufferLList = 
				new LinkedList<>(extendedArray.subList(startIndex, endIndex));
		/**
		if(tier1BufferLList.contains(keyTerm)){
			tier1BufferLList.set(tier1BufferLList.indexOf(keyTerm), normTriggerTerm);
		}**/
		
		// process the context frame to remove unwanted "trigger" term mentions
		tier1BufferLList = updateContextSequence(tier1BufferLList);
		// extract sub frames 
		extractSubContextFrames(tier1BufferLList, posLexInstance);

		// update the context map
		if(tier1BufferMap.containsKey(frameCount)){
			tier1BufferList = tier1BufferMap.get(frameCount);
		}
		//System.out.println("::"+tier1BufferList.size());
		tier1BufferList.add(tier1BufferLList);
		tier1BufferMap.put(frameCount, tier1BufferList);
		posLexInstance.posContextFrame.put(docIndex, tier1BufferMap);
	}
	
	private void callFrameConfiguration(
			Integer keyTermIndex, Integer seedFrame, String keyTerm, 
			ArrayList<String> extendedArray, int docIndex, 
			OriginalLexemeAttributes orgLexInstance) {
		
		int frameCount=0, frameSize=seedFrame, endIndex=0,startIndex=keyTermIndex;
		while(frameCount < seedFrame){
			ArrayList<String> tier1BufferList = new ArrayList<>(extendedArray);
			endIndex = (startIndex+frameSize);
			//System.out.println("\t"+startIndex+"\t"+endIndex+"\t"+splitTokens.size());
			//System.out.println("\t*****************"+frameCount+"*******************************");
			if((startIndex >= 0) && (endIndex <= extendedArray.size())){
				tier1BufferList.set(keyTermIndex, normTriggerTerm);
				//size sufficient
				extractContextFrame(startIndex, endIndex, tier1BufferList, 
						frameCount, keyTerm, docIndex, orgLexInstance);
				startIndex--;
			}
			frameCount++;
		}
	}

	private void callFrameConfiguration(
			Integer keyTermIndex, Integer seedFrame, String keyTerm, 
			ArrayList<String> extendedArray, int docIndex, 
			PosLexemeAttributes posLexInstance) {
		
		int frameCount=0, frameSize=seedFrame, endIndex=0,startIndex=keyTermIndex;
		while(frameCount < seedFrame){
			ArrayList<String> tier1BufferList = new ArrayList<>(extendedArray);
			endIndex = (startIndex+frameSize);
			//System.out.println("\t"+startIndex+"\t"+endIndex+"\t"+splitTokens.size());
			//System.out.println("\t*****************"+frameCount+"*******************************");
			if((startIndex >= 0) && (endIndex <= extendedArray.size())){
				tier1BufferList.set(keyTermIndex, normTriggerTerm);
				//size sufficient
				extractContextFrame(startIndex, endIndex, tier1BufferList, 
						frameCount, keyTerm, docIndex, posLexInstance);
				startIndex--;
			}
			frameCount++;
		}
	}

	public LinkedHashMap<String, Integer> updateDictionaryVolume(
			LinkedHashMap<String, Integer> dictionarMap, String dictionaryKey) {
		
		int count = 0;
		if(dictionarMap.containsKey(dictionaryKey)){
			count = dictionarMap.get(dictionaryKey);
		}
		count++;
		dictionarMap.put(dictionaryKey, count);
		
		return(dictionarMap);
	}	

	public boolean triggerEventPresent(LinkedList<String> bufferLList) {

		boolean retVal = false;
		Iterator<String> tier1Itr = bufferLList.iterator();
		while(tier1Itr.hasNext()){
			String tier1StringValue = tier1Itr.next();
			/**
			if((tier1StringValue.matches("PROTEINT[\\d+]")) 
					|| (tier1StringValue.matches("@"))){
				retVal = true;
				break;
			}**/
			if((tier1StringValue.matches("@"))){
				retVal = true;
				break;
			}
		}
		return(retVal);
	}
	
	private ArrayList<LinkedList<String>> callVariantFrameConfiguration(Integer seedFrame,
			ArrayList<String> extendedArray) {

		ArrayList<LinkedList<String>> tier1ResultBufferList = new ArrayList<>();
		ArrayList<Integer> extendedArrayIndex =  (ArrayList<Integer>) 
				IntStream.range(0, extendedArray.size())
				.boxed().collect(Collectors.toList());
		/**
		for(int i=0;i<extendedArrayIndex.size()-seedFrame;i++){
			int startIndex = i;
			int endIndex = i+seedFrame;
			LinkedList<String> tier2BufferLList = new LinkedList<>(
					extendedArray.subList(startIndex, endIndex));
			tier2BufferLList = new LinkedList<>(tier2BufferLList.stream()
					.map((currVal) -> currVal.replaceFirst("PROTEINT\\d+", "PROTEINT"))
					.collect(Collectors.toList()));
			tier1ResultBufferList.add(tier2BufferLList);
		}**/
		
		/**
		 * variants for PPI set
		 */
		/**
		ArrayList<Integer> tier2BufferList = new ArrayList<>();
		Iterator<Integer> tier1Itr = extendedArrayIndex.iterator();
		while(tier1Itr.hasNext()){
			Integer tier1IntergerValue = tier1Itr.next();
			if(triggerArrayList.contains(extendedArray.get(tier1IntergerValue))){
				tier2BufferList.add(tier1IntergerValue);
			}
		}
		//System.out.println("\t:"+tier2BufferList);
		
		for(int i=0;i<tier2BufferList.size()-1;i++){
			for(int j=(i+1);j<tier2BufferList.size();j++){
				ArrayList<Integer> tier3BufferList = new ArrayList<>();
				tier3BufferList.add(tier2BufferList.get(i));
				tier3BufferList.add(tier2BufferList.get(j));
				ArrayList<Integer> triggerIndicesArray = new ArrayList<>();
				Iterator<Integer> tier2Itr = tier3BufferList.iterator();
				while(tier2Itr.hasNext()){
					Integer tier2IntergerValue = tier2Itr.next();
					for(int k=0;k<seedFrame-2;k++){
						triggerIndicesArray.add(tier2IntergerValue-k);
						triggerIndicesArray.add(tier2IntergerValue+k);
					}
				}
				//System.out.println("\n::"+triggerIndicesArray);
				
				for(int startIndex=0;startIndex<(extendedArrayIndex.size()-seedFrame);startIndex++){
					//System.out.println("\t\t"+extendedArray.get(startIndex));
					int endIndex = (startIndex+seedFrame);
					LinkedList<Integer> tier1BufferLList = new LinkedList<>(
							IntStream.range(startIndex, endIndex).boxed().collect(Collectors.toList()));
					LinkedList<String> tier2BufferLList = new LinkedList<>(
							tier1BufferLList.stream()
							.map((val)->extendedArray.get(val)).collect(Collectors.toList()));
					//System.out.println("\t:::"+tier2BufferLList);
					if(!triggerEventPresent(tier2BufferLList)){
						if(tier1BufferLList.stream()
								.filter((val) -> triggerIndicesArray.contains(val))
								.collect(Collectors.toList()).size() == 0){
							LinkedList<String> tier3BufferLList =
							new LinkedList<>(tier2BufferLList.stream()
									.map((currVal) -> currVal.replaceFirst("PROTEINT\\d+", "PROTEINT"))
									.collect(Collectors.toList()));
							// tier2BufferLList = 
							//System.out.println("\t select map::"+tier3BufferLList);
							tier1ResultBufferList.add(tier3BufferLList);
						}
					}
				}
			}
		}
		**/
		
		/**
		 * Variants for DDI 
		 */
		ArrayList<Integer> tier2BufferList = new ArrayList<>();
		Iterator<Integer> tier1Itr = extendedArrayIndex.iterator();
		while(tier1Itr.hasNext()){
			Integer tier1IntergerValue = tier1Itr.next();
			if(triggerArrayList.contains(extendedArray.get(tier1IntergerValue))
					&& (!extendedArray.get(tier1IntergerValue).matches("PROTEINT0"))){
				tier2BufferList.add(tier1IntergerValue);
			}
		}
		//System.out.println("\t:"+tier2BufferList);
		
		for(int i=0;i<tier2BufferList.size()-1;i++){
			for(int j=(i+1);j<tier2BufferList.size();j++){
				ArrayList<Integer> tier3BufferList = new ArrayList<>();
				tier3BufferList.add(tier2BufferList.get(i));
				tier3BufferList.add(tier2BufferList.get(j));
				ArrayList<Integer> triggerIndicesArray = new ArrayList<>();
				Iterator<Integer> tier2Itr = tier3BufferList.iterator();
				while(tier2Itr.hasNext()){
					Integer tier2IntergerValue = tier2Itr.next();
					for(int k=0;k<seedFrame-2;k++){
						triggerIndicesArray.add(tier2IntergerValue-k);
						triggerIndicesArray.add(tier2IntergerValue+k);
					}
				}
				//System.out.println("\n::"+triggerIndicesArray);
				
				for(int startIndex=0;startIndex<(extendedArrayIndex.size()-seedFrame);startIndex++){
					//System.out.println("\t\t"+extendedArray.get(startIndex));
					int endIndex = (startIndex+seedFrame);
					LinkedList<Integer> tier1BufferLList = new LinkedList<>(
							IntStream.range(startIndex, endIndex).boxed().collect(Collectors.toList()));
					LinkedList<String> tier2BufferLList = new LinkedList<>(
							tier1BufferLList.stream()
							.map((val)->extendedArray.get(val)).collect(Collectors.toList()));
					//System.out.println("\t:::"+tier2BufferLList);
					if(!triggerEventPresent(tier2BufferLList)){
						if(tier1BufferLList.stream()
								.filter((val) -> triggerIndicesArray.contains(val))
								.collect(Collectors.toList()).size() == 0){
							LinkedList<String> tier3BufferLList =
							new LinkedList<>(tier2BufferLList.stream()
									.map((currVal) -> currVal.replaceFirst("PROTEINT\\d+", "PROTEINT"))
									.collect(Collectors.toList()));
							// tier2BufferLList = 
							//System.out.println("\t select map::"+tier3BufferLList);
							tier1ResultBufferList.add(tier3BufferLList);
						}
					}
				}
			}
		}

		return(tier1ResultBufferList);
	}

	private void callFrameGenerator(ArrayList<String> extendedArray, 
			Integer seedFrame, OriginalLexemeAttributes orgLexInstance) {

		int docIndex = orgLexInstance.orgLexemeContextFrame.size()+1;
		ListIterator<String> tier1Itr = triggerArrayList.listIterator();
		while(tier1Itr.hasNext()){
			Integer keyTermIndex = 0;
			String keyTerm = tier1Itr.next();
			if(extendedArray.contains(keyTerm)){
				ArrayList<Integer> extendedArrayIndex =  (ArrayList<Integer>) 
						IntStream.range(0, extendedArray.size())
						.boxed().collect(Collectors.toList());
				ArrayList<Integer> tier1BufferList = (ArrayList<Integer>) 
						extendedArrayIndex.stream().
						filter(index -> extendedArray.get(index).equals(keyTerm)).
						collect(Collectors.toList());
				Iterator<Integer> tier2Itr = tier1BufferList.iterator();
				while(tier2Itr.hasNext()){
					keyTermIndex = tier2Itr.next();
					
					// find proximal and distant words
					if(keyTerm.matches("PROTEINT[\\d+&&[^0]]")){
						for(int i=0;i<extendedArray.size();i++){
							if((i>(keyTermIndex-seedFrame)) && (i<(keyTermIndex+seedFrame))){
								orgLexInstance.triggerProximal = updateDictionaryVolume(
										orgLexInstance.triggerProximal, extendedArray.get(i));
							}else{
								orgLexInstance.triggerDistant = updateDictionaryVolume(
										orgLexInstance.triggerDistant, extendedArray.get(i));
							}
						}
					}
					
					// separate word frames
					callFrameConfiguration(keyTermIndex, seedFrame, keyTerm, extendedArray, docIndex, 
							orgLexInstance);
				}
			}
		}
		// find true variants from a sentence
		ArrayList<LinkedList<String>> tier1ResultBufferList = 
				callVariantFrameConfiguration(seedFrame, extendedArray);
		orgLexInstance.orgLexemeVariantFrame.put(docIndex, tier1ResultBufferList);
		// extract sub frames
		for(LinkedList<String> tier1BufferLList : tier1ResultBufferList){
			extractSubVariantFrames(tier1BufferLList, orgLexInstance);
		}
	}

	private void callFrameGenerator( ArrayList<String> extendedArray, 
			Integer seedFrame, PosLexemeAttributes posLexInstance) {

		int docIndex = posLexInstance.posContextFrame.size()+1;
		ListIterator<String> tier1Itr = triggerArrayList.listIterator();
		while(tier1Itr.hasNext()){
			Integer keyTermIndex = 0;
			String keyTerm = tier1Itr.next();
			if(extendedArray.contains(keyTerm)){
				ArrayList<Integer> extendedArrayIndex =  (ArrayList<Integer>) 
						IntStream.range(0, extendedArray.size())
						.boxed().collect(Collectors.toList());
				ArrayList<Integer> tier1BufferList = (ArrayList<Integer>) 
						extendedArrayIndex.stream().
						filter(index -> extendedArray.get(index).equals(keyTerm)).
						collect(Collectors.toList());
				Iterator<Integer> tier2Itr = tier1BufferList.iterator();
				while(tier2Itr.hasNext()){
					keyTermIndex = tier2Itr.next();
					
					// find proximal and distant words
					if(keyTerm.matches("PROTEINT[\\d+&&[^0]]")){
						for(int i=0;i<extendedArray.size();i++){
							if((i>(keyTermIndex-seedFrame)) && (i<(keyTermIndex+seedFrame))){
								posLexInstance.triggerProximal = updateDictionaryVolume(
										posLexInstance.triggerProximal, extendedArray.get(i));
							}else{
								posLexInstance.triggerDistant = updateDictionaryVolume(
										posLexInstance.triggerDistant, extendedArray.get(i));
							}
						}
					}
					
					// separate word frames
					callFrameConfiguration(keyTermIndex, seedFrame, keyTerm, extendedArray, docIndex, 
							posLexInstance);
				}
			}
		}
		// find true variants from a sentence
		ArrayList<LinkedList<String>> tier1ResultBufferList = 
				callVariantFrameConfiguration(seedFrame, extendedArray); 
		posLexInstance.posVariantFrame.put(docIndex, tier1ResultBufferList);
		// extract sub frames
		for(LinkedList<String> tier2BufferLList : tier1ResultBufferList){
			extractSubVariantFrames(tier2BufferLList, posLexInstance);
		}
	}
	
	public ArrayList<String> restructureSentence(ArrayList<String> bufferArray, Integer seedFrame) {
		
		ArrayList<String> restructureArray = new ArrayList<>();
		ArrayList<String> frameExtender = new ArrayList<>();
		for(int i=0;i<seedFrame;i++){
			frameExtender.add("@");
		}
		restructureArray.addAll(frameExtender);
		restructureArray.addAll(bufferArray);
		restructureArray.addAll(frameExtender);
		return(restructureArray);
	}
	
	public void learnContextFrameComposition(
			ArrayList<String> bufferArray, Integer seedFrame,
			OriginalLexemeAttributes orgLexInstance) {
	
		ArrayList<String> extendedArray = restructureSentence(bufferArray, seedFrame);
		callFrameGenerator(extendedArray, seedFrame, orgLexInstance);
	}

	public void learnContextFrameComposition(
			ArrayList<String> bufferArray, Integer seedFrame, 
			PosLexemeAttributes posLexInstance) {

		ArrayList<String> extendedArray = restructureSentence(bufferArray, seedFrame);
		callFrameGenerator(extendedArray, seedFrame, posLexInstance);
		
	}
	
}
