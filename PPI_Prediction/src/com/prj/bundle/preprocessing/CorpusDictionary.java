/**
 * 
 */
package com.prj.bundle.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author neha
 *
 */
public class CorpusDictionary implements Callable<Hashtable<String, 
LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>>>> {

	private Node currentXmlNode;
	private LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>> processProteinEntities;
	private LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>> processAbstractCollection;
	private LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>> processRelationCollection;
	
	/**
	 * Constructors
	 */
	public CorpusDictionary() {
		
	}
	
	public CorpusDictionary(Node currNode, 
			Hashtable<String, LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>>> resultHolder) {
		
		this.currentXmlNode = currNode;
		this.processProteinEntities = resultHolder.get("Protein");
		this.processAbstractCollection = resultHolder.get("Abstract");
		this.processRelationCollection = resultHolder.get("Relation");
	}
	
	/**
	 * Update abstracts for normalized entity mentions
	 * @param abstractText
	 * @param docId 
	 * @param entityLocationMap
	 * @return
	 */
	private String updateAbstract(String abstractText, String docId) {
	
		TreeMap<Integer, ArrayList<String>> decoyMap = new TreeMap<>();
		if(processProteinEntities.containsKey(docId)){
			decoyMap = processProteinEntities.get(docId);
		}
		char[] bufferArray = abstractText.toCharArray();
		StringBuilder tier1BufferBuilder = new StringBuilder();
		for(int currIndex=0;currIndex<bufferArray.length;currIndex++){
			String currentIndexString = String.valueOf(bufferArray[currIndex]);
			if(decoyMap.containsKey(currIndex)){
				TreeMap<Integer, ArrayList<String>> tier1BufferMap = new TreeMap<>(decoyMap);
				ArrayList<String> tier1BufferList = decoyMap.get(currIndex);
				Iterator<String> tier1Itr = tier1BufferList.iterator();
				int endIndex = currIndex;
				StringBuilder bufferBuilder = new StringBuilder();
				while(tier1Itr.hasNext()){
					String tier1StringValue = tier1Itr.next();
					ArrayList<String> tier2BufferList = new ArrayList<>(
							Arrays.asList(tier1StringValue.split("#")));
					endIndex = Integer.parseInt(tier2BufferList.get(0));
					String replaceTag = tier2BufferList.get(1);
					bufferBuilder.append(replaceTag.concat(" "));
				}
				
				ArrayList<Integer> tier2BufferList = new ArrayList<>(
						IntStream.range(currIndex+1, endIndex).boxed()
						.collect(Collectors.toList()));
				ArrayList<Integer> tier3BufferList = new ArrayList<>(tier2BufferList.stream()
						.filter((currVal) -> tier1BufferMap.containsKey(currVal))
						.collect(Collectors.toList()));
				if(tier3BufferList.size() >= 0){
					int lastEndIndex = currIndex;
					Iterator<Integer> tier2Itr = tier3BufferList.iterator();
					while(tier2Itr.hasNext()){
						Integer tier2IntegerValue = tier2Itr.next();
						Iterator<String> tier3Itr = decoyMap.get(tier2IntegerValue).iterator();
						while(tier3Itr.hasNext()){
							String tier3StringValue = tier3Itr.next();
							ArrayList<String> tier4BufferList = new ArrayList<>(
									Arrays.asList(tier3StringValue.split("#")));
							lastEndIndex = Integer.parseInt(tier4BufferList.get(0));
							String replaceTag = tier4BufferList.get(1);
							bufferBuilder.append(replaceTag.concat(" "));
						}
					}
					
					if(lastEndIndex > endIndex){
						endIndex = lastEndIndex;
					}
				}
				tier1BufferBuilder.append(bufferBuilder.toString());
				
				currIndex = (endIndex-1);
			}else{
				tier1BufferBuilder.append(currentIndexString);
			}
		}
		
		abstractText = tier1BufferBuilder.toString().trim();
		return(abstractText);
	}
	
	/**
	* replace realtion id mention for normalized entity mention
	**/
	private ArrayList<String> replaceRelationId(String abstractBundle, 
			String docId) {

		ArrayList<String> tier1BufferResultList = new ArrayList<>();
		abstractBundle = abstractBundle.replaceAll("\\[|\\{|\\(|\\)|\\}|\\]", " ");
		if(processRelationCollection.containsKey(docId)){
			TreeMap<Integer, ArrayList<String>> tier1BufferMap = processRelationCollection.get(docId);
			Iterator<Map.Entry<Integer, ArrayList<String>>> tier1Itr = 
					tier1BufferMap.entrySet().iterator();
			while(tier1Itr.hasNext()){
				String bufferString = abstractBundle;
				Map.Entry<Integer, ArrayList<String>> tier1MapValue = tier1Itr.next();
				Iterator<String> tier2Itr = tier1MapValue.getValue().iterator();
				ArrayList<String> tier1BufferList = new ArrayList<>();
				Matcher genericPatternMatch = Pattern.compile("ArgT\\d+").matcher(bufferString); 
				while(genericPatternMatch.find()){
					String temp = bufferString.substring(
							genericPatternMatch.start(), genericPatternMatch.end());
					tier1BufferList.add(temp);
				}
				int matchIndex = 0;
				while(tier2Itr.hasNext()){
					String tier2StringValue = tier2Itr.next();
					matchIndex = matchIndex + tier1BufferList.stream()
							.filter((currVal) -> Pattern.compile(tier2StringValue)
									.matcher(currVal).matches()).collect(Collectors.toList()).size();
					
				}

				if(matchIndex == 2){
					tier2Itr = tier1MapValue.getValue().iterator();
					while(tier2Itr.hasNext()){
						String tier2StringValue = tier2Itr.next();
						/**
						 * Screen for all attribute tag ids
						 */
						TreeMap<String, LinkedList<Integer>> tier2BufferMap = new TreeMap<>(); 
						Matcher argMatch = Pattern.compile("ArgT\\d+").matcher(bufferString);
						while(argMatch.find()){
							LinkedList<Integer> tier1BufferLList = new LinkedList<>();
							tier1BufferLList.add(argMatch.start());
							tier1BufferLList.add(argMatch.end());
							String temp = bufferString.substring(
									argMatch.start(), argMatch.end());
							tier2BufferMap.put(temp, tier1BufferLList);
						}
						// replace id
						if(tier2BufferMap.containsKey(tier2StringValue)){
							StringBuilder bufferBuilder = new StringBuilder();
							LinkedList<Integer> tier2BufferLList = tier2BufferMap.get(tier2StringValue);
							bufferBuilder.append(bufferString.substring(0, tier2BufferLList.get(0)));
							bufferBuilder.append("TRIGGERPRI");
							bufferBuilder.append(bufferString.substring(
									tier2BufferLList.get(1), bufferString.length()));
							bufferString = bufferBuilder.toString().trim();
						}
						
					}
					if(Pattern.compile("ArgT\\d+").matcher(bufferString).find()){
						bufferString = bufferString.replaceAll("ArgT\\d+", "PROTEINT");
					}
					tier1BufferResultList.add(bufferString);
				}
			}
		}
		return(tier1BufferResultList);
	}
	
	/**
	 * Assemeble candidate instance abstracts
	 * @param subElement
	 * @param docPos
	 * @param docId
	 * @param entityLocationMap 
	 */
	private void assembleAbstract(Element subElement, String docId) {
		
		String abstractText = subElement.
				getElementsByTagName("text").item(0).getTextContent();
		
		abstractText = updateAbstract(abstractText, docId);
		TreeMap<Integer, ArrayList<String>> tier1BufferResultMap = new TreeMap<>();
		ArrayList<String> tier1BufferList = new ArrayList<>();
		int sentenceIndex = 1, totalRelationSize=0;
		for(String abstractBundle : abstractText.split("\n")){
			if(!abstractBundle.endsWith(".")){
				abstractBundle = abstractBundle.concat(".");
			}
			ArrayList<String> decoyList = 
					replaceRelationId(abstractBundle, docId);
			if(!decoyList.isEmpty()){
				//tier1BufferList.addAll(decoyList);
				tier1BufferResultMap.put(sentenceIndex, decoyList);
				totalRelationSize = totalRelationSize+decoyList.size();
			}
			sentenceIndex++;
		}
		if(processRelationCollection.containsKey(docId)){
			if(processRelationCollection.get(docId).size() != totalRelationSize){
				System.out.println("\t"+docId);
				
					Iterator<Map.Entry<Integer, ArrayList<String>>> t4Itr = 
							processRelationCollection.get(docId).entrySet().iterator();
					System.out.println("\t"+docId);
					while(t4Itr.hasNext()){
						Map.Entry<Integer, ArrayList<String>> t4V = t4Itr.next();
						System.out.println("\t"+t4V.getKey()+"\t"+t4V.getValue());
					}
					
					Iterator<String> t7Itr = tier1BufferList.iterator();
					while(t7Itr.hasNext()){
						String t7V = t7Itr.next();
						System.out.println("\t"+t7V);
					}
					System.exit(0);
				
			}
		}
		
		TreeMap<Integer, ArrayList<String>> decoyMap = new TreeMap<>();
		if(processAbstractCollection.containsKey(docId)){
			decoyMap = processAbstractCollection.get(docId);
		}
		decoyMap.putAll(tier1BufferResultMap);
		processAbstractCollection.put(docId, decoyMap);
	}

	/**
	 *  detect and normalize relation in the realtion mention list
	 * @param currentNodeElement
	 * @param subElement 
	 * @param string 
	 * @param textContent
	 * @param textContent2
	 */
	private void findRelation(String docId, 
			Element subElement) {
		
		Element subNode = (Element) subElement.getElementsByTagName("node").item(0);
		String identifier1 = "Arg"+subNode.getAttribute("refid").toString();
		subNode = (Element) subElement.getElementsByTagName("node").item(1);
		String identifier2 = "Arg"+subNode.getAttribute("refid").toString();
		Integer relId = Integer.parseInt(subElement.getAttribute("id").toString().replaceAll("[a-z|A-Z]", ""));
		TreeMap<Integer, ArrayList<String>> decoyHash = new TreeMap<>();
		if(processRelationCollection.containsKey(docId)){
			decoyHash = processRelationCollection.get(docId);
		}
		ArrayList<String> decoyList = new ArrayList<>();
		decoyList.add(identifier1);
		decoyList.add(identifier2);
		decoyHash.put(relId, decoyList);
		processRelationCollection.put(docId, decoyHash);
	}
	
	
	/**
	 * Create Entity based NE dictionary
	 * @param docId 
	 * @param subElement
	 * @param processEntities
	 * @return 
	 */
	private LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> generateCorpusEntity(
			String docId, Element subElement, 
			LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> processEntities) {
		
		String entityId = docId;
		String tagId = subElement.getAttribute("id").toString();
		String entityName = subElement.
				getElementsByTagName("text").item(0).getTextContent();
		Integer locationIndex = Integer.parseInt(((Element)subElement.getElementsByTagName("location")
				.item(0)).getAttribute("offset").toString());
		Integer endIndex = Integer.parseInt(((Element)subElement.getElementsByTagName("location")
				.item(0)).getAttribute("length").toString());
		TreeMap<Integer, ArrayList<String>> decoyHash = new TreeMap<>();
		ArrayList<String> decoyList = new ArrayList<>();
		if(processEntities.containsKey(entityId)){
			decoyHash = processEntities.get(entityId);
			if(decoyHash.containsKey(locationIndex)){
				decoyList = decoyHash.get(locationIndex);
			}
		}
		tagId = "Arg"+tagId;
		tagId = String.valueOf(locationIndex+endIndex).concat("#"+tagId);
		decoyList.add(tagId);
		decoyHash.put(locationIndex, decoyList);
		processEntities.put(entityId, decoyHash);
		return(processEntities);
	}
	
	/**
	 * Put the current thread to sleep 
	 */
	private void haltThreadProcess() {
		try {
			Thread.sleep(3);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Create NE dictionary from the corpus data
	 */
	@Override
	public Hashtable<String, LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>>> call() 
			throws Exception {
		
		Hashtable<String, LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>>> resultSet = 
				new Hashtable<>();
		if(currentXmlNode.getNodeType() == Node.ELEMENT_NODE){
			Element currentNodeElement = (Element) currentXmlNode;
			String docId = 
					currentNodeElement.getElementsByTagName("id").item(0).getTextContent();
			/**
			 * Take into account annotation tags for entity name retrievals 
			 */
			NodeList subNodeList = currentNodeElement.getElementsByTagName("annotation");
			for(int subNodeNm=0;subNodeNm<subNodeList.getLength();subNodeNm++){
				Node subNode = subNodeList.item(subNodeNm);
				//System.out.println(">> protein"+subNodeNm);
				if(subNode.getNodeType() == Node.ELEMENT_NODE){
					Element subElement = (Element) subNode;
					processProteinEntities = generateCorpusEntity(docId, 
							subElement, processProteinEntities);
				}
			}
			
			/**
			Iterator<Map.Entry<String, TreeMap<Integer, ArrayList<String>>>> t1Itr = 
					processProteinEntities.entrySet().iterator();
			while(t1Itr.hasNext()){
				Map.Entry<String, TreeMap<Integer, ArrayList<String>>> t1V = t1Itr.next();
				Iterator<Map.Entry<Integer, ArrayList<String>>> t2Itr = 
						t1V.getValue().entrySet().iterator();
				System.out.println("\t"+t1V.getKey());
				while(t2Itr.hasNext()){
					Map.Entry<Integer, ArrayList<String>> t2V = t2Itr.next();
					System.out.println("\t"+t2V.getKey()+"\t"+t2V.getValue());
				}
			}**/
			
			resultSet.put("Protein",processProteinEntities);
			
			
			subNodeList = currentNodeElement.getElementsByTagName("relation");
			for(int subNodeNm=0;subNodeNm<subNodeList.getLength();subNodeNm++){
				 Node subNode = subNodeList.item(subNodeNm);
				 if(subNode.getNodeType() == Node.ELEMENT_NODE){
					Element subElement = (Element) subNode;
					findRelation(docId, subElement);
				 }
			}
			
			/**
			Iterator<Map.Entry<String, TreeMap<Integer, ArrayList<String>>>> t3Itr = 
					processRelationCollection.entrySet().iterator();
			while(t3Itr.hasNext()){
				Map.Entry<String, TreeMap<Integer, ArrayList<String>>> t3V = t3Itr.next();
				Iterator<Map.Entry<Integer, ArrayList<String>>> t4Itr = 
						t3V.getValue().entrySet().iterator();
				System.out.println("\t"+t3V.getKey());
				while(t4Itr.hasNext()){
					Map.Entry<Integer, ArrayList<String>> t4V = t4Itr.next();
					System.out.println("\t"+t4V.getKey()+"\t"+t4V.getValue());
				}
			}**/
			
			resultSet.put("Relation", processRelationCollection);
			
			subNodeList = currentNodeElement.getElementsByTagName("passage");
			for(int subNodeNm=0;subNodeNm<subNodeList.getLength();subNodeNm++){
				 Node subNode = subNodeList.item(subNodeNm);
				 if(subNode.getNodeType() == Node.ELEMENT_NODE){
					 Element subElement = (Element) subNode;
					 assembleAbstract(subElement, docId);
				 }
			}
			resultSet.put("Abstract",processAbstractCollection);
			
			/**
			Iterator<Map.Entry<String, TreeMap<Integer, ArrayList<String>>>> t5Itr = 
					processAbstractCollection.entrySet().iterator();
			while(t5Itr.hasNext()){
				Map.Entry<String, TreeMap<Integer, ArrayList<String>>> t5V = t5Itr.next();
				Iterator<Map.Entry<Integer, ArrayList<String>>> t6Itr = 
						t5V.getValue().entrySet().iterator();
				System.out.println("\t"+t5V.getKey());
				while(t6Itr.hasNext()){
					Map.Entry<Integer, ArrayList<String>> t6V = t6Itr.next();
					Iterator<String> t7Itr = t6V.getValue().iterator();
					while(t7Itr.hasNext()){
						String t7V = t7Itr.next();
						System.out.println("\t"+t6V.getKey()+"\t"+t7V);
					}
				}
			}**/
			
			haltThreadProcess();
		}
		return resultSet;
	}
}
