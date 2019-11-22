package com.prj.bundle.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DataStreaming {
	
	private Properties systemProperties;
	private LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> predictResultMap;
	private LinkedHashMap<String, Integer> performStats;
	
	// Constructors
	public DataStreaming() throws IOException {
		this.systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
        predictResultMap = new LinkedHashMap<>();
        performStats = new LinkedHashMap<>();
        
	}

	
	private NodeList loadXMLFile() throws ParserConfigurationException, SAXException, IOException {
		/**
		 * Check if the input file is in json or BioC format
		 */
		File fileInStream = new File(systemProperties.getProperty("trainingFile"));
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document xmlFileDoc = docBuilder.parse(fileInStream);
		xmlFileDoc.getDocumentElement().normalize();
		return(xmlFileDoc.getElementsByTagName("document"));
	}

	/**
	 * Process Raw Data stream
	 * @param resultHolder
	 * @return 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private Hashtable<String, LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>>> 
		processDataStream(Hashtable<String, 
				LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>>> resultHolder) 
						throws InterruptedException, ExecutionException, IOException, ParserConfigurationException, SAXException{

		NodeList xmlNodeTree = loadXMLFile();
		Integer threadPoolSize;
		if(xmlNodeTree.getLength() > 1){
			threadPoolSize = (xmlNodeTree.getLength()/2);
		}else{
			threadPoolSize = 1;
		}
		System.out.println("\n"+threadPoolSize);
		ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize);
		LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>> entityType;
		resultHolder.put("Protein",entityType = new LinkedHashMap<>());
		resultHolder.put("Abstract",entityType = new LinkedHashMap<>());
		resultHolder.put("Relation",entityType = new LinkedHashMap<>());
		long beginSysTime = System.currentTimeMillis();
		
		for(int nodeNm=0; nodeNm <xmlNodeTree.getLength(); nodeNm++){
			Node xmlNode = xmlNodeTree.item(nodeNm);
			String f = ((Element)xmlNode).getElementsByTagName("id").item(0).getTextContent();
			CorpusDictionary workerThread = new CorpusDictionary(xmlNode, resultHolder);
			//collect entities of various kinds and abstracts
			Future<Hashtable<String, 
			LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>>>> taskCollector =
					threadPoolExecutor.submit(workerThread);
			resultHolder = taskCollector.get();
		}
		threadPoolExecutor.shutdown();
		System.out.println("\n Total Execution Time:-"+(System.currentTimeMillis()-beginSysTime)/1000);
		return(resultHolder);
	}

	/**
	* load prediction file based on the Xg_boost evalauation
	**/
	private void loadPredictionFile() throws IOException {

		
		FileReader fileInRead = new FileReader(systemProperties.getProperty("predictFilePath"));
		BufferedReader buffRead = new BufferedReader(fileInRead);
		String currentRead = buffRead.readLine();
		while(currentRead != null){
			ArrayList<String> tier1BufferList = new ArrayList<>(Arrays.asList(currentRead.split("\t")));
			String docId = tier1BufferList.get(0);
			Integer sentIndex = Integer.parseInt(tier1BufferList.get(1).split("#")[0]);
			TreeMap<Integer, ArrayList<String>> tier1BufferMap = new TreeMap<>();
			ArrayList<String> tier2BufferList = new ArrayList<>();
			if(predictResultMap.containsKey(docId)){
				tier1BufferMap = predictResultMap.get(docId);
			}
			if(tier1BufferMap.containsKey(sentIndex)){
				tier2BufferList = tier1BufferMap.get(sentIndex);
			}
			tier2BufferList.add(tier1BufferList.get(2));
			tier1BufferMap.put(sentIndex, tier2BufferList);
			predictResultMap.put(docId, tier1BufferMap);
			currentRead = buffRead.readLine();
		}
		buffRead.close();
	}

	private ArrayList<ArrayList<String>> processArrayList(ArrayList<String> bufferList, int sentType) {

		ArrayList<ArrayList<String>> tier1BufferResultList = new ArrayList<>();
		Iterator<String> tier1Itr = bufferList.iterator();
		while(tier1Itr.hasNext()){
			String tier1StringValue = tier1Itr.next();
			ArrayList<String> tier1BufferList = new ArrayList<>(
					Arrays.asList(tier1StringValue.split("\\s+")));
			tier1BufferList = new ArrayList<>(tier1BufferList.stream()
					.filter((currVal) -> (!currVal.matches("\\W+"))).collect(Collectors.toList()));
			tier1BufferResultList.add(tier1BufferList);
		}
		return(tier1BufferResultList);
	}

	private boolean getTriggerIndex(ArrayList<String> tier1BufferList, 
			ArrayList<String> tier2BufferList) {

		boolean returnValue = false;
		ArrayList<Integer> tier1IndexArray =  (ArrayList<Integer>) 
				IntStream.range(0, tier1BufferList.size())
				.boxed().collect(Collectors.toList());
		LinkedList<Integer> tier3BufferArray = new LinkedList<>(tier1IndexArray.stream()
				.filter((currVal) -> Pattern.compile("TRIGGERPRI")
						.matcher(tier1BufferList.get(currVal)).find())
				.collect(Collectors.toList()));
		
		ArrayList<Integer> tier2IndexArray =  (ArrayList<Integer>) 
				IntStream.range(0, tier2BufferList.size())
				.boxed().collect(Collectors.toList());
		LinkedList<Integer> tier4BufferArray = new LinkedList<>(tier2IndexArray.stream()
				.filter((currVal) -> Pattern.compile("TRIGGERPRI")
						.matcher(tier2BufferList.get(currVal)).find())
				.collect(Collectors.toList()));
		
		if(tier3BufferArray.equals(tier4BufferArray)){
			returnValue = true;
		}
		
		return(returnValue);
	}
	
	private int compareInstances(ArrayList<String> bufferList,
			ArrayList<ArrayList<String>> bufferPredictList) {

		int index=0;
		Iterator<ArrayList<String>> tier1Itr = bufferPredictList.iterator();
		while(tier1Itr.hasNext()){
			ArrayList<String> tier1ListValue = tier1Itr.next();
			if(getTriggerIndex(bufferList, tier1ListValue)){
				return(index);
			}
			index++;
		}
		return(-1);
	}

	private void populateStatsMap(String mapKey, int incrementFactor) {

		Integer statScore = 0;
		if(performStats.containsKey(mapKey)){
			statScore = performStats.get(mapKey);
		}
		statScore = statScore+incrementFactor;
		performStats.put(mapKey, statScore);
	}
	
	/**
	* identify if instances conatin pair mention
	**/
	private void identifyPairStatus(TreeMap<Integer, ArrayList<String>> bufferMap, String currDocId) {

		if(predictResultMap.containsKey(currDocId)){
			TreeMap<Integer, ArrayList<String>> tier1BufferMap = predictResultMap.get(currDocId);
			Iterator<Map.Entry<Integer, ArrayList<String>>> tier1Itr = 
					bufferMap.entrySet().iterator();
			while(tier1Itr.hasNext()){
				Map.Entry<Integer, ArrayList<String>> tier1MapValue = tier1Itr.next();
				if(tier1BufferMap.containsKey(tier1MapValue.getKey())){
					ArrayList<ArrayList<String>> tier1BufferList = 
							processArrayList(tier1MapValue.getValue(),0);
					ArrayList<ArrayList<String>> tier2BufferList =
							processArrayList(tier1BufferMap.get(tier1MapValue.getKey()),1);
					Iterator<ArrayList<String>> tier2Itr = tier1BufferList.iterator();
					while(tier2Itr.hasNext()){
						ArrayList<String> tier2ListValue = tier2Itr.next();
						int returnValue = compareInstances(tier2ListValue, tier2BufferList);
						if(returnValue != -1){
							// match
							populateStatsMap("TP", 1);
							tier2BufferList.remove(returnValue);
						}else{
							// false negative
							populateStatsMap("FN", 1);
						}
					}
					if(!tier2BufferList.isEmpty()){
						// false positive
						populateStatsMap("FP", tier2BufferList.size());
					}
				}else{
					// false negative
					populateStatsMap("FN", tier1MapValue.getValue().size());
				}
				tier1BufferMap.remove(tier1MapValue.getKey());
			}
			if(!tier1BufferMap.isEmpty()){
				// false positive
				predictResultMap.put(currDocId, tier1BufferMap);
			}else{
				predictResultMap.remove(currDocId);
			}
			
		}else{
			// false negative
			Iterator<Map.Entry<Integer, ArrayList<String>>> tier3Itr = bufferMap.entrySet().iterator();
			while(tier3Itr.hasNext()){
				Map.Entry<Integer, ArrayList<String>> tier3MapValue = tier3Itr.next();
				populateStatsMap("FN", tier3MapValue.getValue().size());
			}
		}
		
	}

	/**
	* Calculate prediction performance
	**/
	private void calculatePredictionPerformance(
			LinkedHashMap<String, TreeMap<Integer, ArrayList<String>>> bufferMap) {
		
		ArrayList<String> docKeySet = new ArrayList<>(bufferMap.keySet());
		Iterator<String> tier1Itr = docKeySet.iterator();
		while(tier1Itr.hasNext()){
			String currDocId = tier1Itr.next();
			//if(currDocId.equals("HPRD50_d38")){
				System.out.println("\t"+currDocId);
				identifyPairStatus(bufferMap.get(currDocId), currDocId);
			//}
		}
		/**
		 * screen for remaining false positive
		 */
		Iterator<Map.Entry<String, TreeMap<Integer, ArrayList<String>>>> tier2Itr = 
				predictResultMap.entrySet().iterator();
		while(tier2Itr.hasNext()){
			Map.Entry<String, TreeMap<Integer, ArrayList<String>>> tier2MapValue = tier2Itr.next();
			Iterator<Map.Entry<Integer, ArrayList<String>>> tier3Itr = 
					tier2MapValue.getValue().entrySet().iterator();
			while(tier3Itr.hasNext()){
				Map.Entry<Integer, ArrayList<String>> tier3MapValue = tier3Itr.next();
				populateStatsMap("FP", tier3MapValue.getValue().size());
			}
		}
		if(!performStats.containsKey("TP")){
			performStats.put("TP", 0);
		}else if(!performStats.containsKey("FP")){
			performStats.put("FP", 0);
		}else if(!performStats.containsKey("FN")){
			performStats.put("FN", 0);
		}
		
		System.out.println("\t>>"+performStats);
		double f1Score = ((performStats.get("TP").doubleValue()*2)
				/((performStats.get("TP").doubleValue()*2)+(performStats.get("FP").doubleValue())+(performStats.get("FN").doubleValue())));
		
		double recall = ((performStats.get("TP").doubleValue())
				/((performStats.get("TP").doubleValue())+(performStats.get("FN").doubleValue())));
		
		double precision = ((performStats.get("TP").doubleValue())
				/((performStats.get("TP").doubleValue())+(performStats.get("FP").doubleValue())));
		
		System.out.println("\t"+performStats);
		System.out.println("\t f1 ::"+f1Score);
		System.out.println("\t recall ::"+recall);
		System.out.println("\t precision ::"+precision);
	}

	public static void main(String[] args) {

		Hashtable<String, LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>>> 
		resultHolder = new Hashtable<>();
		try {
			DataStreaming streamInstance = new DataStreaming();
			streamInstance.loadPredictionFile();
			resultHolder = streamInstance.processDataStream(resultHolder);
			int relationSize = 0;
			Iterator<Map.Entry<String, TreeMap<Integer, ArrayList<String>>>> t5Itr = 
					resultHolder.get("Abstract").entrySet().iterator();
			while(t5Itr.hasNext()){
				Map.Entry<String, TreeMap<Integer, ArrayList<String>>> t5V = t5Itr.next();
				//if(t5V.getKey().equals("BioInfer_d613")){
					Iterator<Map.Entry<Integer, ArrayList<String>>> t6Itr = 
							t5V.getValue().entrySet().iterator();
					while(t6Itr.hasNext()){
						Map.Entry<Integer, ArrayList<String>> t6V = t6Itr.next();
						if(!t6V.getValue().isEmpty()){
							HashSet<String> tier1BufferSet = new HashSet<>(t6V.getValue());
							Iterator<String> t7Itr = tier1BufferSet.iterator();
							while(t7Itr.hasNext()){
								String t7V = t7Itr.next();
								//System.out.println("\t"+t7V);
								relationSize++;
							}
						}
					}
				//}
			}
			System.out.println("\t total relation size ::"+(relationSize));
			streamInstance.calculatePredictionPerformance(resultHolder.get("Abstract"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			// TODO: handle finally clause
			System.out.println("Phase I - Preprocessing Analysis Completed");
		}
	}
	
}
