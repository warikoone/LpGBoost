package com.prj.bundle.preprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	
	// Constructors
	public DataStreaming() throws IOException {
		this.systemProperties = new Properties();
		InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
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
	 * Pre-processing of the input files
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

	public static void main(String[] args) {

		Hashtable<String, LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>>> 
		resultHolder = new Hashtable<>();
		try {
			DataStreaming streamInstance = new DataStreaming();
			NormaliseAbstracts normaliseInstance = new NormaliseAbstracts();
			resultHolder = streamInstance.processDataStream(resultHolder);
			normaliseInstance.addCorpusResource(resultHolder);
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
		catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			// TODO: handle finally clause
			System.out.println("Phase I - Preprocessing Analysis Completed");
		}
	}
	
}
