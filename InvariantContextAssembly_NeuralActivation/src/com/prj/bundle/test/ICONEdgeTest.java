/**
 * 
 */
package com.prj.bundle.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.prj.bundle.model.FrameFractioner;
import com.prj.bundle.model.LexicalAppraiser;
import com.prj.bundle.representation.InvariantContextRepresentation;
import com.prj.bundle.wrapper.OriginalLexemeAttributes;

/**
 * @author iasl
 *
 */
public class ICONEdgeTest {
	
	private TreeMap<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, 
	LinkedList<Double>>>> iconFeatureMap;
	private HashSet<String> corporaOrgLexeme;
	public HashMap<String, Integer> contextPositionUpdateMap;
	private Properties systemProperties;
	private Integer seedFrame;
	private String normTriggerTerm;
	private String regularTerm;
	private TreeMap<Integer, TreeMap<Integer, 
	LinkedHashMap<LinkedList<String>, Double>>>  iconFeatureSequenceMap;

	/**
	 * @throws IOException 
	 * 
	 */
	public ICONEdgeTest() throws IOException {

		iconFeatureMap = new TreeMap<>();
		corporaOrgLexeme = new HashSet<>();
        contextPositionUpdateMap = new HashMap<>();
        this.systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
        seedFrame = Integer.parseInt(systemProperties.getProperty("seedFrameSize"));
        normTriggerTerm = systemProperties.getProperty("normTriggerTerm");
        regularTerm = systemProperties.getProperty("regularTerm");
        iconFeatureSequenceMap = new TreeMap<>();
	}
	
	
	private ArrayList<String> corporaDictionary(String bufferString) {
		
		if(null != bufferString){
			return(new ArrayList<>(
					Arrays.asList(bufferString.split("\\s+"))));
		}else{
			return(null);
		}
	}
	
	private ArrayList<String> loadResource(String fileName) throws IOException {

		ArrayList<String> tier1BufferArray = new ArrayList<>();
		FileReader localFileReader = new FileReader(fileName);
		BufferedReader localBuffReader = new BufferedReader(localFileReader);
		String lineRead = localBuffReader.readLine();
		while(null != lineRead){
			tier1BufferArray.add(lineRead.split("\\t")[1].trim());
			lineRead = localBuffReader.readLine();
		}
		localBuffReader.close();
		return(tier1BufferArray);
	}
	
	
	private TreeMap<Integer, LinkedHashMap<String, LinkedList<String>>> 
	loadInstanceResource(String fileName) throws IOException {

		TreeMap<Integer, LinkedHashMap<String, LinkedList<String>>> tier1BufferMap = 
				new TreeMap<>();
		FileReader localFileReader = new FileReader(fileName);
		BufferedReader localBuffReader = new BufferedReader(localFileReader);
		String lineRead = localBuffReader.readLine();
		while(null != lineRead){
			ArrayList<String> tier1BufferArray = 
					new ArrayList<>(Arrays.asList(lineRead.split("\t")));
			Integer instanceType = Integer.valueOf(tier1BufferArray.get(0));
			String docId = String.valueOf(tier1BufferArray.get(1));
			LinkedList<String> tier1BufferLList = new LinkedList<>(
					Arrays.asList(tier1BufferArray.get(2).split("\\s+")));
			
			LinkedHashMap<String, LinkedList<String>> tier2BufferMap = 
					new LinkedHashMap<>();
			if(tier1BufferMap.containsKey(instanceType)){
				tier2BufferMap = tier1BufferMap.get(instanceType);
			}
			tier2BufferMap.put(docId, tier1BufferLList);
			tier1BufferMap.put(instanceType, tier2BufferMap);
			
			lineRead = localBuffReader.readLine();
		}
		localBuffReader.close();
		return(tier1BufferMap);
	}
	
	
	private void readICONRepresentationFile(String fileName) throws IOException {

		FileReader localFileReader = new FileReader(fileName);
		BufferedReader localBuffReader = new BufferedReader(localFileReader);
		String lineRead = localBuffReader.readLine();
		Integer contextId = 0, clusterId = 0;;
		while(null != lineRead){
			ArrayList<String> tier1BufferArray = 
					new ArrayList<>(Arrays.asList(lineRead.split("\t")));

			String bufferContextId = tier1BufferArray.get(0).replaceAll("[a-z|A-Z]", "");
			if(bufferContextId.length() > 0){
				contextId = Integer.parseInt(bufferContextId);
			}
			String bufferClusterId = tier1BufferArray.get(1).replaceAll("[a-z|A-Z]", "");
			if(bufferClusterId.length() > 0){
				clusterId = Integer.parseInt(bufferClusterId);
			}
			LinkedList<String> invariantFeature = new LinkedList<>(
					Arrays.asList(tier1BufferArray.get(2).replaceAll("\\[|\\]", "").split(",")));
			LinkedList<Double> invariantScore = new LinkedList<>(
					Arrays.asList(tier1BufferArray.get(3).replaceAll("\\[|\\]", "").split(",")).stream()
					.map((currValue) -> Double.parseDouble(currValue)).collect(Collectors.toList()));
			/**
			 * Update iCon Feature Map
			 */
			LinkedHashMap<Integer, HashMap<LinkedList<String>, 
			LinkedList<Double>>> tier1BufferMap = new LinkedHashMap<>();
			if(iconFeatureMap.containsKey(contextId)){
				tier1BufferMap = iconFeatureMap.get(contextId);
			}
			HashMap<LinkedList<String>, LinkedList<Double>> tier2BufferMap = new HashMap<>();
			if(tier1BufferMap.containsKey(clusterId)){
				tier2BufferMap = tier1BufferMap.get(clusterId);
			}
			tier2BufferMap.put(invariantFeature, invariantScore);
			tier1BufferMap.put(clusterId, tier2BufferMap);
			iconFeatureMap.put(contextId, tier1BufferMap);
			
			/**
			 * Update context position map
			 */
			if(contextId >= seedFrame){
				contextPositionUpdateMap.putIfAbsent(invariantFeature.get(0), contextId);
			}
			
			lineRead = localBuffReader.readLine();
		}
		localBuffReader.close();
	}
	
	private void readICONSequenceFeature(String fileName) throws IOException {

		FileReader localFileReader = new FileReader(fileName);
		BufferedReader localBuffReader = new BufferedReader(localFileReader);
		String lineRead = localBuffReader.readLine();
		while(null != lineRead){
			ArrayList<String> tier1BufferList = new ArrayList<>(Arrays.asList(lineRead.split("\t")));
			TreeMap<Integer,LinkedHashMap<LinkedList<String>, Double>> tier1BufferMap = new TreeMap<>();
			Integer instanceType = Integer.parseInt(tier1BufferList.get(0));
			Integer variantType = Integer.parseInt(tier1BufferList.get(1));
			if(iconFeatureSequenceMap.containsKey(variantType)){
				tier1BufferMap = iconFeatureSequenceMap.get(variantType);
			}
			LinkedHashMap<LinkedList<String>, Double> tier2BufferMap = new LinkedHashMap<>();
			if(tier1BufferMap.containsKey(instanceType)){
				tier2BufferMap = tier1BufferMap.get(instanceType);
			}
			LinkedList<String> tier1BufferLList = new LinkedList<>(
					Arrays.asList(tier1BufferList.get(2).split("\\,")));
			Double featureScore =  Double.parseDouble(tier1BufferList.get(3));
			tier2BufferMap.put(tier1BufferLList, featureScore);
			tier1BufferMap.put(instanceType, tier2BufferMap);
			iconFeatureSequenceMap.put(variantType, tier1BufferMap);
			
			lineRead = localBuffReader.readLine();
		}
		localBuffReader.close();
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			long beginSysTime = System.currentTimeMillis();
			ICONEdgeTest testInstance = new ICONEdgeTest();
			FrameFractioner frameInstance = new FrameFractioner();
			String modelPhase = "test";
			
			
			/**
			 * Original Lexeme Process
			 */
			// Attributes
			OriginalLexemeAttributes orgLexInstance = new OriginalLexemeAttributes();
			// Lexeme Weights
			LexicalAppraiser lexAppraiserInstance = new LexicalAppraiser();
			
			ListIterator<String> tier00Itr = testInstance.loadResource(
					testInstance.systemProperties.getProperty("processedOriginalFile")).listIterator();
			while(tier00Itr.hasNext()){
				String tier1BufferString = tier00Itr.next();
				ArrayList<String> tier1BufferArray = 
						testInstance.corporaDictionary(tier1BufferString);
				// generate proximal and distant word list
				if(!tier1BufferArray.isEmpty()){
					testInstance.corporaOrgLexeme.addAll(tier1BufferArray);
					frameInstance.learnContextFrameComposition(
							tier1BufferArray, testInstance.seedFrame, orgLexInstance);
				}
			}
			
			// determine word significance
			LinkedHashMap<String, Double> orgLexemeAppraiser = lexAppraiserInstance.
					createOrgLexemeContingencyMatrix(testInstance.corporaOrgLexeme, orgLexInstance);
			
			
			testInstance.readICONRepresentationFile(
					testInstance.systemProperties.getProperty("iConFeaturePattern"));
			
			/**
			 * Read Generate Universal Feature Templates
			 */
			testInstance.readICONSequenceFeature(testInstance
					.systemProperties.getProperty("iconSequenceFeatureFile"));
			TreeSet<String> bufferFeatureSequenceList = new TreeSet<>();
			
			/**
			Iterator<Map.Entry<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, 
			Double>>>> tier011Itr = 
					testInstance.iconFeatureSequenceMap.entrySet().iterator();
			while(tier011Itr.hasNext()){
				Map.Entry<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, 
				Double>>> tier1MapValue = 
						tier011Itr.next();
				Iterator<Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier012Itr = 
						tier1MapValue.getValue().entrySet().iterator();
				while(tier012Itr.hasNext()){
					Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier012MapValue = 
							tier012Itr.next();
					Iterator<LinkedList<String>> tier013Itr = 
							tier012MapValue.getValue().keySet().iterator();
					while(tier013Itr.hasNext()){
						bufferFeatureSequenceList.addAll(tier013Itr.next().stream()
								.map((currVal) -> currVal).collect(Collectors.toList()));
					}
				}
			}**/
			
			TreeMap<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>> tier1BufferResultMap = 
					new TreeMap<>();
			
			TreeMap<Integer, ArrayList<LinkedList<String>>> tier1BufferMap = new TreeMap<>(); 
			
			TreeMap<Integer, LinkedHashMap<String, LinkedList<String>>> testBufferMap = 
					testInstance.loadInstanceResource(testInstance
							.systemProperties.getProperty("testPosTaggedFile"));
			
			TreeMap<Integer, LinkedHashMap<String, LinkedList<String>>> testOriginalBufferMap = 
					testInstance.loadInstanceResource(testInstance
					.systemProperties.getProperty("testOriginalFile"));
			
			
			TreeMap<Integer, Double> tier1LearnedWeights = new TreeMap<>();
			TreeMap<Integer, Double> tier2LearnedWeights = new TreeMap<>();
			TreeMap<Integer, TreeMap<Integer, Double>> tier3LearnedWeights = new TreeMap<>();
			Iterator<Integer> tier0Itr = testBufferMap.keySet().iterator();
			while(tier0Itr.hasNext()){
				Integer classType = tier0Itr.next();
				TreeMap<Integer, Double> tier0LearnedWeights = new TreeMap<>();
				Iterator<Integer> tier01Itr = testBufferMap.keySet().iterator();
				while(tier01Itr.hasNext()){
					tier0LearnedWeights.put(tier01Itr.next(), new Double(0));
				}
				tier3LearnedWeights.put(classType, tier0LearnedWeights);
				if(classType == 1){
					tier2LearnedWeights.put(classType, new Double(0.5).doubleValue());
				}else if(classType == -1){
					tier2LearnedWeights.put(classType, new Double(0.5).doubleValue());
				}
				tier1LearnedWeights.put(classType, new Double(0.5));
			}
			
			InvariantContextRepresentation representationInstance = 
					new InvariantContextRepresentation(testInstance.iconFeatureMap,
							testInstance.seedFrame, 
							testInstance.normTriggerTerm, testInstance.regularTerm,
							tier3LearnedWeights, tier2LearnedWeights, tier1LearnedWeights);
			
			Iterator<Map.Entry<Integer, LinkedHashMap<String, LinkedList<String>>>> tier1Itr = 
					testBufferMap.entrySet().iterator();
			
			FileWriter tier1FileWS = null;
			BufferedWriter tier1BuffWS = null;
			boolean writeStatus = false;
			
			TreeMap<Integer, LinkedHashMap<String, TreeMap<Integer, 
			LinkedList<String>>>> tier2BufferResultMap = new TreeMap<>();
			TreeMap<Integer, LinkedHashMap<String, TreeMap<Integer, 
			ArrayList<LinkedList<String>>>>> tier2OrgBufferResultMap = new TreeMap<>();
			TreeMap<Integer, LinkedHashMap<String, TreeMap<Integer, 
			ArrayList<LinkedList<String>>>>> tier2PosBufferResultMap = new TreeMap<>();
			
			while(tier1Itr.hasNext()){
				
				LinkedHashMap<String, TreeMap<Integer, LinkedList<String>>> tier4BufferResultMap = 
						new LinkedHashMap<>();
				LinkedHashMap<String, TreeMap<Integer, ArrayList<LinkedList<String>>>> tier4OrgBufferResultMap = 
						new LinkedHashMap<>();
				LinkedHashMap<String, TreeMap<Integer, ArrayList<LinkedList<String>>>> tier4PosBufferResultMap = 
						new LinkedHashMap<>();
				
				Map.Entry<Integer, LinkedHashMap<String, LinkedList<String>>> tier1MapValue = 
						tier1Itr.next();
				Iterator<Map.Entry<String, LinkedList<String>>> tier2Itr = 
						tier1MapValue.getValue().entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<String, LinkedList<String>> tier2MapValue = tier2Itr.next();
					//System.out.println("\t"+tier2MapValue.getKey()+"\t"+tier1MapValue.getKey());
					
					LinkedList<String> tier2OrgBufferList = testOriginalBufferMap
							.get(tier1MapValue.getKey()).get(tier2MapValue.getKey());
					
					LinkedList<String> tier1BufferLList = new LinkedList<String>(
							frameInstance.restructureSentence(
									new ArrayList<>(tier2MapValue.getValue()), 
									testInstance.seedFrame));
					
					LinkedList<String> tier1OrgBufferLList = new LinkedList<String>(
							frameInstance.restructureSentence(
									new ArrayList<>(tier2OrgBufferList), 
									testInstance.seedFrame));
					
					//if(tier2MapValue.getKey().equals("HPRD50_d12@2#1")&&(tier1MapValue.getKey()==-1)){
					
					TreeMap<Integer, LinkedList<String>> tier5ResultBufferMap = 
							representationInstance
							.mapToSequenceInvariantContextRepresentation(
									tier1BufferLList, testInstance.contextPositionUpdateMap,
									bufferFeatureSequenceList);
					
					TreeMap<Integer, ArrayList<LinkedList<String>>> tier5PosResultBufferMap = 
							representationInstance
							.mapToSequencePOSContextRepresentation(
									tier1BufferLList);
					
					TreeMap<Integer, ArrayList<LinkedList<String>>> tier5OrgResultBufferMap = 
							representationInstance
							.mapToSequenceOriginalContextRepresentation(
									tier1OrgBufferLList);

					tier4BufferResultMap.put(tier2MapValue.getKey(), tier5ResultBufferMap);
					tier4OrgBufferResultMap.put(tier2MapValue.getKey(), tier5OrgResultBufferMap);
					tier4PosBufferResultMap.put(tier2MapValue.getKey(), tier5PosResultBufferMap);
					
					if(!writeStatus){
						tier1FileWS = new FileWriter(
								testInstance.systemProperties.getProperty("iconTestFile"));
					}else{
						tier1FileWS = new FileWriter(
								testInstance.systemProperties.getProperty("iconTestFile"),true);
					}
					tier1BuffWS = new BufferedWriter(tier1FileWS);
					
					/**
					 * Invariant Context Tag representation
					 */
					
					Iterator<Map.Entry<Integer, LinkedList<String>>> tier5Itr = 
							tier5ResultBufferMap.entrySet().iterator();
					while(tier5Itr.hasNext()){
						String iconRepresentationVector = "";
						Map.Entry<Integer, LinkedList<String>> tier5MapValue = 
								tier5Itr.next();
						iconRepresentationVector = iconRepresentationVector
								+(tier5MapValue.getValue().stream()
								.map(tVal -> String.valueOf((tVal)))
								.reduce((prev,curr)->prev+","+curr).get())+"|";
					
						iconRepresentationVector = iconRepresentationVector
								.substring(0, iconRepresentationVector.length()-1);
						
						tier1BuffWS.write(tier1MapValue.getKey().toString()
								+"\t"+tier2MapValue.getKey().toString()+"\t"
								+tier5MapValue.getKey()+"\t"+iconRepresentationVector);
						tier1BuffWS.newLine();
						tier1BuffWS.flush();
					}
					tier1BuffWS.close();
					writeStatus = true;
					//}
				}
				tier2BufferResultMap.put(tier1MapValue.getKey(), tier4BufferResultMap);
				tier2OrgBufferResultMap.put(tier1MapValue.getKey(), tier4OrgBufferResultMap);
				tier2PosBufferResultMap.put(tier1MapValue.getKey(), tier4PosBufferResultMap);
			}
			
			/**
			 * Generate Test Data Feature Map
			 */
			/**
			TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> tier7BufferResultMap = 
					representationInstance.generateEdgeBasedContextFeatures(
							testInstance.iconFeatureSequenceMap,tier1BufferResultMap, modelPhase);**/
			int corefHopSize = 1;
			TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> tier7BufferResultMap = 
					representationInstance.biChannelFeaureMap(testInstance.iconFeatureSequenceMap,
							tier2BufferResultMap, modelPhase, 
							testInstance.contextPositionUpdateMap, orgLexemeAppraiser, 
							tier2OrgBufferResultMap, tier2PosBufferResultMap);
			
			writeStatus = false;
			Iterator<Entry<Integer, HashMap<String, ArrayList<LinkedList<Double>>>>> tier7Itr = 
					tier7BufferResultMap.entrySet().iterator();
			while(tier7Itr.hasNext()){
				Entry<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> tier7MapValue = 
						tier7Itr.next();
				Iterator<Map.Entry<String, ArrayList<LinkedList<Double>>>> tier8Itr = 
						tier7MapValue.getValue().entrySet().iterator();
				while(tier8Itr.hasNext()){
					String representationString = "";
					Map.Entry<String, ArrayList<LinkedList<Double>>> tier8MapValue = tier8Itr.next();
					Iterator<LinkedList<Double>> tier9Itr = tier8MapValue.getValue().iterator();
					while(tier9Itr.hasNext()){
						LinkedList<Double> tier9LList = tier9Itr.next();
						
						String tier11StringValue = tier9LList.stream()
								.map((currVal) -> String.valueOf(currVal))
								.collect(Collectors.toList()).stream()
								.reduce((x,y)-> x.toString()+","+y.toString()).get();
						representationString = representationString + 
								tier11StringValue.toString().concat(",");
					}
					if(representationString.length()>0){
						representationString = 
								representationString.substring(0, representationString.length()-1);
					}
					
					if(!writeStatus){
						tier1FileWS = new FileWriter(
								testInstance.systemProperties.getProperty("iconEdgeTestFile"));
					}else{
						tier1FileWS = new FileWriter(
								testInstance.systemProperties.getProperty("iconEdgeTestFile"),true);
					}
					tier1BuffWS = new BufferedWriter(tier1FileWS);
					
					tier1BuffWS.write(tier7MapValue.getKey()+
							"\t"+tier8MapValue.getKey().toString()+"\t"+representationString);
					tier1BuffWS.newLine();
					tier1BuffWS.flush();
					writeStatus = true;
				}
			}
			tier1BuffWS.close();
			
			System.out.println("\n Total Execution Time:-"+(System.currentTimeMillis()-beginSysTime)/1000);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
