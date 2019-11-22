/**
 * 
 */
package com.prj.bundle.training;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutput;
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
import com.prj.bundle.representation.InvariantEdgeFeatureEvaluation;
import com.prj.bundle.wrapper.OriginalLexemeAttributes;


/**
 * @author iasl
 *
 */
public class ICONEdgeTraining {
	
	private TreeMap<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, 
	LinkedList<Double>>>> iconFeatureMap;
	private HashSet<String> corporaOrgLexeme;
	public HashMap<String, Integer> contextPositionUpdateMap;
	private Properties systemProperties;
	private Integer seedFrame;
	private String normTriggerTerm;
	private String regularTerm;
	
	/**
	 * @throws IOException 
	 * 
	 */
	public ICONEdgeTraining() throws IOException {

        iconFeatureMap = new TreeMap<>();
        corporaOrgLexeme = new HashSet<>();
        contextPositionUpdateMap = new HashMap<>();
        this.systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
        seedFrame = Integer.parseInt(systemProperties.getProperty("seedFrameSize"));
        normTriggerTerm = systemProperties.getProperty("normTriggerTerm");
        regularTerm = systemProperties.getProperty("regularTerm");
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

	private TreeMap<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, 
	Double>>> readICONSequenceFeature(String fileName) throws IOException {

		TreeMap<Integer, TreeMap<Integer, LinkedHashMap<LinkedList
		<String>, Double>>> tier0BufferResultMap = new TreeMap<>();
		FileReader localFileReader = new FileReader(fileName);
		BufferedReader localBuffReader = new BufferedReader(localFileReader);
		String lineRead = localBuffReader.readLine();
		while(null != lineRead){
			ArrayList<String> tier1BufferList = new ArrayList<>(Arrays.asList(lineRead.split("\t")));
			TreeMap<Integer,LinkedHashMap<LinkedList<String>, Double>> tier1BufferMap = new TreeMap<>();
			Integer instanceType = Integer.parseInt(tier1BufferList.get(0));
			Integer variantType = Integer.parseInt(tier1BufferList.get(1));
			if(tier0BufferResultMap.containsKey(variantType)){
				tier1BufferMap = tier0BufferResultMap.get(variantType);
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
			tier0BufferResultMap.put(variantType, tier1BufferMap);
			
			lineRead = localBuffReader.readLine();
		}
		localBuffReader.close();
		
		return(tier0BufferResultMap);
	}

	public static void main(String[] args) {
		
		try {
			
			long beginSysTime = System.currentTimeMillis();
			ICONEdgeTraining trainingInstance = new ICONEdgeTraining();
			FrameFractioner frameInstance = new FrameFractioner();
			String modelPhase = "training";
			
			
			
			/**
			 * Original Lexeme Process
			 */
			// Attributes
			OriginalLexemeAttributes orgLexInstance = new OriginalLexemeAttributes();
			// Lexeme Weights
			LexicalAppraiser lexAppraiserInstance = new LexicalAppraiser();
			
			ListIterator<String> tier00Itr = trainingInstance.loadResource(
					trainingInstance.systemProperties.getProperty("processedOriginalFile")).listIterator();
			while(tier00Itr.hasNext()){
				String tier1BufferString = tier00Itr.next();
				ArrayList<String> tier1BufferArray = 
						trainingInstance.corporaDictionary(tier1BufferString);
				// generate proximal and distant word list
				if(!tier1BufferArray.isEmpty()){
					trainingInstance.corporaOrgLexeme.addAll(tier1BufferArray);
					frameInstance.learnContextFrameComposition(
							tier1BufferArray, trainingInstance.seedFrame, orgLexInstance);
				}
			}
			
			// determine word significance
			LinkedHashMap<String, Double> orgLexemeAppraiser = lexAppraiserInstance.
					createOrgLexemeContingencyMatrix(trainingInstance.corporaOrgLexeme, orgLexInstance);
			/**
			Iterator<Map.Entry<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>>> t1 = 
					orgLexInstance.orgLexemeContextFrame.entrySet().iterator();
			while(t1.hasNext()){
				Map.Entry<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>> t1V = t1.next();
				Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> t2 = 
						t1V.getValue().entrySet().iterator();
				System.out.println("\n\t"+t1V.getKey());
				while(t2.hasNext()){
					Map.Entry<Integer, ArrayList<LinkedList<String>>> t2V = t2.next();
					Iterator<LinkedList<String>> t3 = t2V.getValue().iterator();
					System.out.println("\t"+t2V.getKey());
					while(t3.hasNext()){
						System.out.println("\t"+t3.next());
					}
				}
			}**/
			/**
			Iterator<Map.Entry<String, Double>> t1Itr = orgLexemeAppraiser.entrySet().iterator();
			while(t1Itr.hasNext()){
				Map.Entry<String, Double> t1V = t1Itr.next();
				System.out.println("\t"+t1V.getKey()+"\t"+t1V.getValue());
			}
			System.exit(0);**/
			
			TreeSet<String> bufferFeatureSequenceList = new TreeSet<>();
			
			trainingInstance.readICONRepresentationFile(
					trainingInstance.systemProperties.getProperty("iConFeaturePattern"));

			
			TreeMap<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>> tier1BufferResultMap = 
					new TreeMap<>();
			
			
			TreeMap<Integer, LinkedHashMap<String, LinkedList<String>>> trainingBufferMap = 
					trainingInstance.loadInstanceResource(trainingInstance
					.systemProperties.getProperty("trainingPosTaggedFile"));

			TreeMap<Integer, LinkedHashMap<String, LinkedList<String>>> trainingOriginalBufferMap = 
					trainingInstance.loadInstanceResource(trainingInstance
					.systemProperties.getProperty("trainingOriginalFile"));
			
			TreeMap<Integer, Double> tier1LearnedWeights = new TreeMap<>();
			TreeMap<Integer, Double> tier2LearnedWeights = new TreeMap<>();
			TreeMap<Integer, TreeMap<Integer, Double>> tier3LearnedWeights = new TreeMap<>();
			Iterator<Integer> tier0Itr = trainingBufferMap.keySet().iterator();
			while(tier0Itr.hasNext()){
				Integer classType = tier0Itr.next();
				TreeMap<Integer, Double> tier0LearnedWeights = new TreeMap<>();
				Iterator<Integer> tier01Itr = trainingBufferMap.keySet().iterator();
				while(tier01Itr.hasNext()){
					tier0LearnedWeights.put(tier01Itr.next(), new Double(0));
				}
				tier3LearnedWeights.put(classType, tier0LearnedWeights);
				tier2LearnedWeights.put(classType, new Double(0));
				tier1LearnedWeights.put(classType, new Double(0.5));
			}
			
			InvariantContextRepresentation representationInstance = 
					new InvariantContextRepresentation(trainingInstance.iconFeatureMap,
							trainingInstance.seedFrame, trainingInstance.normTriggerTerm,
							trainingInstance.regularTerm,
							tier3LearnedWeights, tier2LearnedWeights, tier1LearnedWeights);			
			
			
			Iterator<Map.Entry<Integer, LinkedHashMap<String, LinkedList<String>>>> tier1Itr = 
					trainingBufferMap.entrySet().iterator();
			
			FileWriter tier1FileWS = null;
			BufferedWriter tier1BuffWS = null;
			boolean writeStatus = false;
			int instanceNumber = 0;
			TreeMap<Integer, LinkedHashMap<String, TreeMap<Integer, LinkedList<String>>>> tier2BufferResultMap = 
					new TreeMap<>();
			TreeMap<Integer, LinkedHashMap<String, TreeMap<Integer, 
			ArrayList<LinkedList<String>>>>> tier2PosBufferResultMap = 
					new TreeMap<>();
			TreeMap<Integer, LinkedHashMap<String, TreeMap<Integer, 
			ArrayList<LinkedList<String>>>>> tier2OrgBufferResultMap = 
					new TreeMap<>();
			TreeMap<Integer, ArrayList<LinkedList<String>>> tier1BufferMap = new TreeMap<>();
			
			TreeMap<Integer, Integer> tier2BufferMap = new TreeMap<>();
			
			while(tier1Itr.hasNext()){
				
				LinkedHashMap<String, TreeMap<Integer, LinkedList<String>>> tier4BufferResultMap = 
						new LinkedHashMap<>();

				LinkedHashMap<String, TreeMap<Integer, ArrayList<LinkedList<String>>>> tier4PosBufferResultMap = 
						new LinkedHashMap<>();
				
				LinkedHashMap<String, TreeMap<Integer, ArrayList<LinkedList<String>>>> tier4OrgBufferResultMap = 
						new LinkedHashMap<>();
				
				ArrayList<LinkedList<String>> tier1BufferList = new ArrayList<>();
				
				Map.Entry<Integer, LinkedHashMap<String, LinkedList<String>>> tier1MapValue = 
						tier1Itr.next();
				Iterator<Map.Entry<String, LinkedList<String>>> tier2Itr = 
						tier1MapValue.getValue().entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<String, LinkedList<String>> tier2MapValue = tier2Itr.next();
					//System.out.println("\t"+tier1MapValue.getKey()+"\t"+tier2MapValue.getKey());
					
					LinkedList<String> tier2OrgBufferList = 
							trainingOriginalBufferMap.get(tier1MapValue.getKey()).get(tier2MapValue.getKey());
					
					LinkedList<String> tier1BufferLList = new LinkedList<String>(
							frameInstance.restructureSentence(
									new ArrayList<>(tier2MapValue.getValue()), 
									trainingInstance.seedFrame));
					
					LinkedList<String> tier1OrgBufferLList = new LinkedList<String>(
							frameInstance.restructureSentence(
									new ArrayList<>(tier2OrgBufferList), 
									trainingInstance.seedFrame));
					
					//if(tier2MapValue.getKey().equals("IEPA_d23@1#1")&&(tier1MapValue.getKey()==1)){
					
					ArrayList<Integer> tier2BufferList = new ArrayList<>();
					for(int i=0;i<tier1BufferLList.size();i++){
						if(tier1BufferLList.get(i).matches(trainingInstance.normTriggerTerm)){
							tier2BufferList.add(i);
						}
					}
					int triggerDifference = 
							((tier2BufferList.get(tier2BufferList.size()-1) 
									- tier2BufferList.get(0))/trainingInstance.seedFrame);
					int count = 0;
					if(tier2BufferMap.containsKey(triggerDifference)){
						count = tier2BufferMap.get(triggerDifference);
					}
					count++;
					tier2BufferMap.put(triggerDifference, count);
					
					TreeMap<Integer, LinkedList<String>> tier5ResultBufferMap = 
							representationInstance
							.mapToSequenceInvariantContextRepresentation(
									tier1BufferLList, trainingInstance.contextPositionUpdateMap,
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
								trainingInstance.systemProperties.getProperty("iconTrainingFile"));
					}else{
						tier1FileWS = new FileWriter(
								trainingInstance.systemProperties.getProperty("iconTrainingFile"),true);
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
						tier1BufferList.add(tier5MapValue.getValue());
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
					instanceNumber++;
					//}
				}
				tier1BufferMap.put(tier1MapValue.getKey(), tier1BufferList);
				tier2BufferResultMap.put(tier1MapValue.getKey(), tier4BufferResultMap);
				tier2OrgBufferResultMap.put(tier1MapValue.getKey(), tier4OrgBufferResultMap);
				tier2PosBufferResultMap.put(tier1MapValue.getKey(), tier4PosBufferResultMap);
			}
			
						
			/**
			 * Generate Universal feature map
			 */
			InvariantEdgeFeatureEvaluation featureEvalInstance = 
					new InvariantEdgeFeatureEvaluation(tier1BufferMap, trainingInstance.seedFrame);
			/**
			featureEvalInstance.initializeFeatureEdgeSubContexts();
			featureEvalInstance.getFeatureEdgeDifferentialContexts();
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier7BufferResultMap =  
					featureEvalInstance.callSoftmaxFeatureEdgeScore();**/
			/**
			TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>> tier7BufferResultMap = 
					featureEvalInstance.callCorefFeatureEdgeScore(corefHopSize, instanceNumber);**/
			
			
			/***
			 * Read the sequence feature for training (IMPORTANT)
			 */
			TreeMap<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, 
			Double>>> tier7BufferResultMap = trainingInstance.readICONSequenceFeature(
							trainingInstance.systemProperties.getProperty("iconSequenceFeatureFile"));
			
			/***
			 * Read the sequence feature for generation (IMPORTANT)
			 */
			
			/**
			TreeMap<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, 
			Double>>> tier7BufferResultMap = 
					featureEvalInstance.generateFeatureTemplate(trainingInstance.iconFeatureMap);
			
			tier1FileWS = new FileWriter(
					trainingInstance.systemProperties.getProperty("iconSequenceFeatureFile"));
			tier1BuffWS = new BufferedWriter(tier1FileWS);
			Iterator<Entry<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>>>> tier7Itr = 
					tier7BufferResultMap.entrySet().iterator();
			while(tier7Itr.hasNext()){
				Entry<Integer, TreeMap<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier7MapValue = 
						tier7Itr.next();
				Iterator<Entry<Integer, LinkedHashMap<LinkedList<String>, Double>>> tier71Itr = 
						tier7MapValue.getValue().entrySet().iterator();
				while(tier71Itr.hasNext()){
					Entry<Integer, LinkedHashMap<LinkedList<String>, Double>> tier71MapValue = 
							tier71Itr.next();
					Iterator<Map.Entry<LinkedList<String>, Double>> tier72Itr = 
							tier71MapValue.getValue().entrySet().iterator();
					while(tier72Itr.hasNext()){
						Map.Entry<LinkedList<String>, Double> tier72MapValue = tier72Itr.next();
						String bufferString = "";
						for(String currentTerm : tier72MapValue.getKey()){
							bufferString = bufferString.concat(currentTerm+",");
						}
						bufferString = bufferString.substring(0, bufferString.length()-1);
						tier1BuffWS.write(tier71MapValue.getKey()+"\t"+tier7MapValue.getKey()+
								"\t"+bufferString+"\t"+tier72MapValue.getValue().toString());
						tier1BuffWS.newLine();
					}
				}
			}
			tier1BuffWS.flush();
			tier1BuffWS.close();
			//System.exit(0);
			**/
			
			/**
			 * Generate Training Data Feature Map
			 */
			/**
			TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> tier8BufferResultMap = 
					representationInstance.generateEdgeBasedContextFeatures(tier7BufferResultMap,
							tier1BufferResultMap, modelPhase);**/
			
			TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> tier8BufferResultMap = 
					representationInstance.biChannelFeaureMap(tier7BufferResultMap,
							tier2BufferResultMap, modelPhase, 
							trainingInstance.contextPositionUpdateMap, orgLexemeAppraiser, 
							tier2OrgBufferResultMap, tier2PosBufferResultMap);
			
			writeStatus = false;
			Iterator<Entry<Integer, HashMap<String, ArrayList<LinkedList<Double>>>>> tier8Itr = 
					tier8BufferResultMap.entrySet().iterator();
			while(tier8Itr.hasNext()){
				Entry<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> tier8MapValue = 
						tier8Itr.next();
				Iterator<Map.Entry<String, ArrayList<LinkedList<Double>>>> tier9Itr = 
						tier8MapValue.getValue().entrySet().iterator();
				while(tier9Itr.hasNext()){
					String representationString = "";
					Map.Entry<String, ArrayList<LinkedList<Double>>> tier9MapValue = tier9Itr.next();
					Iterator<LinkedList<Double>> tier10Itr = 
							tier9MapValue.getValue().iterator();
					while(tier10Itr.hasNext()){
						LinkedList<Double> tier10LList = tier10Itr.next();
						
						String tier11StringValue = tier10LList.stream()
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
								trainingInstance.systemProperties.getProperty("iconEdgeTrainingFile"));
					}else{
						tier1FileWS = new FileWriter(
								trainingInstance.systemProperties.getProperty("iconEdgeTrainingFile"),true);
					}
					tier1BuffWS = new BufferedWriter(tier1FileWS);
					
					// Classification
					/**
					String classType = "-1";
					if(tier8MapValue.getKey() == 3){ // 1 , 2 , 3 , 4
						classType = "1";
					}
					tier1BuffWS.write(classType+
							"\t"+tier9MapValue.getKey().toString()+"\t"+representationString);
					**/
					
					// Detection
					tier1BuffWS.write(tier8MapValue.getKey().toString()+
							"\t"+tier9MapValue.getKey().toString()+"\t"+representationString);
					
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
