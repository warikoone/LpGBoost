/**
 * 
 */
package com.prj.bundle.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.prj.bundle.representation.InvariantContextRepresentation;
import com.prj.bundle.representation.InvariantEdgeFeatureEvaluation;
import com.prj.bundle.training.ICONEdgeTraining;
import com.prj.bundle.wrapper.OriginalLexemeAttributes;
import com.prj.bundle.wrapper.PosLexemeAttributes;

/**
 * @author iasl
 *
 */
public class ICONCaller {

	
	private Properties systemProperties;
	private HashSet<String> corporaOrgLexeme;
	private HashSet<String> corporaPosLexeme;
	private String normTriggerTerm;
	public HashMap<String, Integer> contextPositionUpdateMap;
	
	public ICONCaller() throws IOException {

		this.systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
        this.corporaOrgLexeme = new HashSet<>();
        this.corporaPosLexeme = new HashSet<>();
        contextPositionUpdateMap = new LinkedHashMap<>();
        normTriggerTerm = systemProperties.getProperty("normTriggerTerm");
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
	
	private ArrayList<String> corporaDictionary(String bufferString) {
		
		if(null != bufferString){
			return(new ArrayList<>(
					Arrays.asList(bufferString.split("\\s+"))));
		}else{
			return(null);
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			long beginSysTime = System.currentTimeMillis();
			ICONCaller iconInstance = new ICONCaller();
			Integer seedFrame = Integer.parseInt(
					iconInstance.systemProperties.getProperty("seedFrameSize"));
			
			
			// Attributes
			OriginalLexemeAttributes orgLexInstance = new OriginalLexemeAttributes();
			PosLexemeAttributes posLexInstance = new PosLexemeAttributes();
			// Context Frames
			FrameFractioner frameInstance = new FrameFractioner();
			// Lexeme Weights
			LexicalAppraiser lexAppraiserInstance = new LexicalAppraiser();
			
			
			
			/**
			 * Original Lexeme Process
			 */
			ListIterator<String> tier1Itr = iconInstance.loadResource(
					iconInstance.systemProperties.getProperty("processedOriginalFile")).listIterator();
			while(tier1Itr.hasNext()){
				String tier1BufferString = tier1Itr.next();
				ArrayList<String> tier1BufferArray = 
						iconInstance.corporaDictionary(tier1BufferString);
				// generate proximal and distant word list
				if(!tier1BufferArray.isEmpty()){
					iconInstance.corporaOrgLexeme.addAll(tier1BufferArray);
					frameInstance.learnContextFrameComposition(
							tier1BufferArray, seedFrame, orgLexInstance);
				}
			}
			// determine word significance
			LinkedHashMap<String, Double> orgLexemeAppraiser = lexAppraiserInstance.
					createOrgLexemeContingencyMatrix(iconInstance.corporaOrgLexeme, orgLexInstance);
			
			System.out.println("\n\t>>"+iconInstance.corporaOrgLexeme);
			System.out.println("\n\t>>"+iconInstance.corporaOrgLexeme.size());
			
			/**
			 * POS Lexeme Process
			 */
			int index = 0;
			tier1Itr = iconInstance.loadResource(
					iconInstance.systemProperties.getProperty("processedPOSTaggedFile")).listIterator();
			while(tier1Itr.hasNext()){
				String tier1BufferString = tier1Itr.next();
				ArrayList<String> tier1BufferArray = 
						iconInstance.corporaDictionary(tier1BufferString);
				//if(index == 2738){
				//System.out.println("\t"+index);
				//System.out.println(">>"+tier1BufferString);
				if(!tier1BufferArray.isEmpty()){
					iconInstance.corporaPosLexeme.addAll(tier1BufferArray);
					frameInstance.learnContextFrameComposition(
							tier1BufferArray, seedFrame, posLexInstance);
				}
				//}
				
				index++;
			}
			
			//System.exit(0);
			
			/**
			Iterator<Map.Entry<Integer, HashMap<Integer, ArrayList<LinkedList<String>>>>> t1 = 
					posLexInstance.posContextFrame.entrySet().iterator();
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
			}
			/**System.exit(0);**/
			
			// determine word significance
			LinkedHashMap<String, Double> posAppraiser = lexAppraiserInstance
					.createPosLexemeContingencyMatrix(iconInstance.corporaPosLexeme, posLexInstance);
			
			/**
			Iterator<Map.Entry<Integer, ArrayList<LinkedList<String>>>> t1 = 
					orgLexInstance.orgLexemeVariantFrame.entrySet().iterator();
			while(t1.hasNext()){
				Map.Entry<Integer, ArrayList<LinkedList<String>>> t1V = t1.next();
				Iterator<LinkedList<String>> t2 = 
						t1V.getValue().iterator();
				System.out.println("\n\t"+t1V.getKey());
				while(t2.hasNext()){
					LinkedList<String> t2V = t2.next();
					System.out.println("\t"+t2V);
				}
			}
			/**System.exit(0);**/
			
			System.out.println("\n\t>>"+iconInstance.corporaPosLexeme);
			System.out.println("\n\t>>"+iconInstance.corporaPosLexeme.size());
			
			// Text to Geometric Projection
			GeometricProjection projectionInstance = 
					new GeometricProjection(posLexInstance, orgLexInstance, 
							seedFrame, posAppraiser, orgLexemeAppraiser);
			
			TreeMap<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> 
			tier1ResultBufferMap = projectionInstance.learnStableInvariantGeometricTransformation();
			
			tier1ResultBufferMap = projectionInstance.
					learnDifferentialInvariantGeometricTransformation(iconInstance);
			
			/**
			 * Write ICON Patterns to file
			 */
			FileWriter fileWS = new FileWriter(
					iconInstance.systemProperties.getProperty("iConFeaturePattern"),false);
			BufferedWriter buffWS = new BufferedWriter(fileWS);
			Iterator<Map.Entry<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>>> tier2Itr = 
					tier1ResultBufferMap.entrySet().iterator();
			while(tier2Itr.hasNext()){
				Map.Entry<Integer, LinkedHashMap<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>>tier2MapValue = 
						tier2Itr.next();
				buffWS.write("index".concat(tier2MapValue.getKey().toString()));
				Iterator<Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>>> tier3Itr = 
						tier2MapValue.getValue().entrySet().iterator();
				while(tier3Itr.hasNext()){
					Map.Entry<Integer, HashMap<LinkedList<String>, LinkedList<Double>>> tier3MapValue = tier3Itr.next();
					Iterator<Map.Entry<LinkedList<String>, LinkedList<Double>>> tier4Itr = tier3MapValue.getValue().entrySet().iterator();
					buffWS.write("\tcluster".concat(tier3MapValue.getKey().toString()));
					int flag = 1;
					while(tier4Itr.hasNext()){
						Map.Entry<LinkedList<String>, LinkedList<Double>> tier4MapValue = tier4Itr.next();
						buffWS.write("\t["+tier4MapValue.getKey().stream()
								.reduce((curr,prev) -> curr+","+prev).get()+"]");
						buffWS.write("\t["+tier4MapValue.getValue().stream()
								.map(currVal -> String.valueOf(currVal))
								.reduce((curr,prev)->curr+","+prev).get()+"]");
						if((flag < tier3MapValue.getValue().size())){
							buffWS.newLine();
							buffWS.write("\t");
						}
						flag++;
					}
					buffWS.newLine();
				}
			}
			buffWS.close();
			System.out.println("\n Total Execution Time:-"+(System.currentTimeMillis()-beginSysTime)/1000);
			
			System.exit(0);
			
			/**
			 * Map the identified Invariant patterns to the candidate instances 
			 */
			
			/**
			InvariantContextRepresentation representationInstance = 
					new InvariantContextRepresentation(
							tier1ResultBufferMap, seedFrame, iconInstance.normTriggerTerm);
			
			TreeMap<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>> tier2BufferResultMap = 
					new TreeMap<>();
			
			TreeMap<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>> tier21BufferResultMap = 
					new TreeMap<>();
			
			TreeMap<Integer, ArrayList<LinkedList<String>>> tier3BufferMap = new TreeMap<>(); 
			
			Iterator<Map.Entry<Integer, LinkedHashMap<String, LinkedList<String>>>> tier5Itr = 
					iconInstance.loadInstanceResource(iconInstance
							.systemProperties.getProperty("instancePosTaggedFile"))
					.entrySet().iterator();
			FileWriter tier1FileWS = null;
			BufferedWriter tier1BuffWS = null;
			boolean writeStatus = false;
			while(tier5Itr.hasNext()){
				
				LinkedHashMap<String, ArrayList<LinkedList<String>>> tier4BufferResultMap = 
						new LinkedHashMap<>();
				LinkedHashMap<String, ArrayList<LinkedList<String>>> tier41BufferResultMap = 
						new LinkedHashMap<>();
				
				Map.Entry<Integer, LinkedHashMap<String, LinkedList<String>>> tier5MapValue = 
						tier5Itr.next();
				Iterator<Map.Entry<String, LinkedList<String>>> tier6Itr = 
						tier5MapValue.getValue().entrySet().iterator();
				while(tier6Itr.hasNext()){
					Map.Entry<String, LinkedList<String>> tier6MapValue = tier6Itr.next();
					//System.out.println("\t"+tier5MapValue.getKey()+"\t"+tier6MapValue.getKey());
					LinkedList<String> tier1BufferLList = new LinkedList<String>(
							frameInstance.restructureSentence(new ArrayList<>(tier6MapValue.getValue()), seedFrame));
					
					//if(tier6MapValue.getKey().equals("BioInfer_d245@4#1")&&(tier5MapValue.getKey()==1)){
					/**
					TreeMap<Integer, TreeMap<String, LinkedList<Double>>> tier2ResultBufferMap = 
							representationInstance
							.mapToSequenceInvariantRepresentation(tier1BufferLList);
					**/
					/**
					TreeMap<Integer, TreeMap<String, LinkedList<String>>> tier3ResultBufferMap = 
							representationInstance
							.mapToSequenceInvariantContextRepresentation(tier1BufferLList);

					ArrayList<LinkedList<String>> tier1BufferList = 
							representationInstance.generateClusterFeaturePairs(tier3ResultBufferMap);
					
					TreeMap<Integer, TreeMap<String, LinkedList<String>>> tier31ResultBufferMap = 
							representationInstance.
							mapToSequenceInvariantDifferentialRepresentation(
									tier1BufferLList, tier3ResultBufferMap.size(), 
									iconInstance.contextPositionUpdateMap);
					
					tier1BufferList = representationInstance
							.addVariantFeaturePairsToCluster(tier1BufferList, tier31ResultBufferMap);
					
					tier3ResultBufferMap.putAll(tier31ResultBufferMap);
					
					// add trigger terms feature map
					tier4BufferResultMap.put(tier6MapValue.getKey(), tier1BufferList);
					
					// add variant feature map
					Iterator<Map.Entry<Integer, TreeMap<String, LinkedList<String>>>> tier61Itr = 
							tier31ResultBufferMap.entrySet().iterator();
					ArrayList<LinkedList<String>> tier2BufferLList = new ArrayList<>();
					while(tier61Itr.hasNext()){
						Map.Entry<Integer, TreeMap<String, LinkedList<String>>> tier61MapValue = 
								tier61Itr.next();
						Iterator<LinkedList<String>> tier611Itr = 
								tier61MapValue.getValue().values().iterator();
						while(tier611Itr.hasNext()){
							tier2BufferLList.add(tier611Itr.next());
						}
					}
					tier41BufferResultMap.put(tier6MapValue.getKey(), tier2BufferLList);
					
					/**
					 * Screen and sort the features based on instances
					 */
			/**
					ArrayList<LinkedList<String>> tier1BufferSet = new ArrayList<>();
					if(tier3BufferMap.containsKey(tier5MapValue.getKey())){
						tier1BufferSet = tier3BufferMap.get(tier5MapValue.getKey());
					}
					ListIterator<LinkedList<String>> tier1ListItr = tier1BufferList.listIterator();
					while(tier1ListItr.hasNext()){
						tier1BufferSet.add(tier1ListItr.next());
					}
					tier3BufferMap.put(tier5MapValue.getKey(), tier1BufferSet);
					
					String iconRepresentationVector = "";
					if(!writeStatus){
						tier1FileWS = new FileWriter(
								iconInstance.systemProperties.getProperty("iconRepresentFile"));
					}else{
						tier1FileWS = new FileWriter(
								iconInstance.systemProperties.getProperty("iconRepresentFile"),true);
					}
					tier1BuffWS = new BufferedWriter(tier1FileWS);
					
					/**
					 * Invariant Vector representation
					 */
					/**
					Iterator<Map.Entry<Integer, TreeMap<String, LinkedList<Double>>>> tier7Itr = 
							tier2ResultBufferMap.entrySet().iterator();
					while(tier7Itr.hasNext()){
						Map.Entry<Integer, TreeMap<String, LinkedList<Double>>> tier7MapValue = 
								tier7Itr.next();
						Iterator<Map.Entry<String, LinkedList<Double>>> tier8Itr = 
								tier7MapValue.getValue().entrySet().iterator();
						while(tier8Itr.hasNext()){
							Map.Entry<String, LinkedList<Double>> tier8MapValue = tier8Itr.next();
							iconRepresentationVector = iconRepresentationVector
									+tier8MapValue.getValue().stream()
									.map(tVal -> String.valueOf((tVal)))
									.reduce((prev,curr)->prev+","+curr).get()+",";
						}
					}**/
					
					/**
					 * Invariant Context Tag representation
					 */
					
			/**
					Iterator<Map.Entry<Integer, TreeMap<String, LinkedList<String>>>> tier7Itr = 
							tier3ResultBufferMap.entrySet().iterator();
					while(tier7Itr.hasNext()){
						Map.Entry<Integer, TreeMap<String, LinkedList<String>>> tier7MapValue = 
								tier7Itr.next();
						Iterator<Map.Entry<String, LinkedList<String>>> tier8Itr = 
								tier7MapValue.getValue().entrySet().iterator();
						while(tier8Itr.hasNext()){
							Map.Entry<String, LinkedList<String>> tier8MapValue = tier8Itr.next();
							iconRepresentationVector = iconRepresentationVector
									+(tier8MapValue.getValue().stream()
									.map(tVal -> String.valueOf((tVal)))
									.reduce((prev,curr)->prev+","+curr).get())+"|";
							
						}
					}
					

					iconRepresentationVector = 
							iconRepresentationVector.substring(0, iconRepresentationVector.length()-1);
					tier1BuffWS.write(tier5MapValue.getKey().toString()+"\t"+tier6MapValue.getKey().toString()+"\t"+iconRepresentationVector);
					tier1BuffWS.newLine();
					tier1BuffWS.flush();
					tier1BuffWS.close();
					writeStatus = true;
					//}
				}
				tier2BufferResultMap.put(tier5MapValue.getKey(), tier4BufferResultMap);
				tier21BufferResultMap.put(tier5MapValue.getKey(), tier41BufferResultMap);
			}
			//System.exit(0);
			
			InvariantEdgeFeatureEvaluation featureEvalInstance = 
					new InvariantEdgeFeatureEvaluation(tier3BufferMap, seedFrame);
			featureEvalInstance.initializeFeatureEdgeSubContexts();
			LinkedHashMap<LinkedList<String>, Double> tier20ResultBufferMap = new LinkedHashMap<>(); 
					//featureEvalInstance.callSoftmaxFeatureEdgeScore();
			
			
			/**
			ArrayList<LinkedList<String>> negFeatures = new ArrayList<>(
					tier3BufferMap.get(-1).stream()
					.filter((currList) -> (!tier3BufferMap.get(1).contains(currList)))
					.collect(Collectors.toList()));
			System.out.println(">>"+tier3BufferMap.get(-1).size());
			System.out.println(">>"+negFeatures.size());
			
			ArrayList<LinkedList<String>> posFeatures = new ArrayList<>(
					tier3BufferMap.get(1).stream()
					.filter((val) -> !tier3BufferMap.get(-1).contains(val))
					.collect(Collectors.toList()));
			
			System.out.println(">>"+tier3BufferMap.get(1).size());
			System.out.println(">>"+posFeatures.size());
			
			ArrayList<LinkedList<String>> commonFeatures = new ArrayList<>(
					tier3BufferMap.get(1).stream()
					.filter((val) -> tier3BufferMap.get(-1).contains(val))
					.collect(Collectors.toList()));
			System.out.println(">>"+commonFeatures.size());
			**/
			//System.exit(0);
			
			/**
			writeStatus = false;
			Iterator<Map.Entry<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>>> tier8Itr = 
					tier2BufferResultMap.entrySet().iterator();
			while(tier8Itr.hasNext()){
				Map.Entry<Integer, LinkedHashMap<String, ArrayList<LinkedList<String>>>> tier8MapValue = 
						tier8Itr.next();
				Iterator<Map.Entry<String, ArrayList<LinkedList<String>>>> tier9Itr = 
						tier8MapValue.getValue().entrySet().iterator();
				while(tier9Itr.hasNext()){
					String representationString = "";
					Map.Entry<String, ArrayList<LinkedList<String>>> tier9MapValue = tier9Itr.next();
					Iterator<LinkedList<String>> tier10Itr = 
							tier9MapValue.getValue().iterator();
					while(tier10Itr.hasNext()){
						String associationEdge = "";
						LinkedList<String> tier10LList = tier10Itr.next();
						if(!commonFeatures.contains(tier10LList)){
							associationEdge = "[";
							Iterator<String> tier11Itr = tier10LList.iterator();
							while(tier11Itr.hasNext()){
								associationEdge = associationEdge + tier11Itr.next().concat(",");
							}
							associationEdge = associationEdge
									.substring(0, associationEdge.length()-1).trim().concat("]");
						}
						if(associationEdge.length()>0){
							representationString = representationString+associationEdge+" ";
						}
					}
					if(representationString.length()>0){
						representationString = 
								representationString.substring(0, representationString.length()-1);
					}
					
					if(!writeStatus){
						tier1FileWS = new FileWriter(
								iconInstance.systemProperties.getProperty("iconEdgeRepresentFile"));
					}else{
						tier1FileWS = new FileWriter(
								iconInstance.systemProperties.getProperty("iconEdgeRepresentFile"),true);
					}
					tier1BuffWS = new BufferedWriter(tier1FileWS);
					
					tier1BuffWS.write(tier8MapValue.getKey().toString()+
							"\t"+tier9MapValue.getKey().toString()+"\t"+representationString);
					tier1BuffWS.newLine();
					tier1BuffWS.flush();
					writeStatus = true;
				}
			}
			tier1BuffWS.close();
			**/
			
			/**
			TreeMap<Integer, HashMap<String, ArrayList<LinkedList<Double>>>> tier3BufferResultMap = 
					representationInstance.generateEdgeBasedContextFeatures(tier20ResultBufferMap,
							tier2BufferResultMap);
			
			/**
			tier3BufferResultMap = representationInstance
					.addTermBasedVariantFeatures(tier3BufferResultMap, tier21BufferResultMap);**/
			
			/**
			writeStatus = false;
			Iterator<Entry<Integer, HashMap<String, ArrayList<LinkedList<Double>>>>> tier8Itr = 
					tier3BufferResultMap.entrySet().iterator();
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
					int indexVal = 1;
					while(tier10Itr.hasNext()){
						LinkedList<Double> tier10LList = tier10Itr.next();
						Collections.sort(tier10LList, Collections.reverseOrder());
						/**
						Iterator<Double> tier11Itr = tier10LList.iterator();
						while(tier11Itr.hasNext()){
							representationString = representationString + 
									tier11Itr.next().toString().concat(",");
						}**/
			/**
						Double updatedVal = tier10LList.get(0); 
						representationString = representationString + 
								updatedVal.toString().concat(",");
						indexVal++;
					}
					if(representationString.length()>0){
						representationString = 
								representationString.substring(0, representationString.length()-1);
					}
					
					if(!writeStatus){
						tier1FileWS = new FileWriter(
								iconInstance.systemProperties.getProperty("iconEdgeRepresentFile"));
					}else{
						tier1FileWS = new FileWriter(
								iconInstance.systemProperties.getProperty("iconEdgeRepresentFile"),true);
					}
					tier1BuffWS = new BufferedWriter(tier1FileWS);
					
					tier1BuffWS.write(tier8MapValue.getKey().toString()+
							"\t"+tier9MapValue.getKey().toString()+"\t"+representationString);
					tier1BuffWS.newLine();
					tier1BuffWS.flush();
					writeStatus = true;
				}
			}
			tier1BuffWS.close();**/

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
