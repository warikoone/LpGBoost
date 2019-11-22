/**
 * 
 */
package com.prj.bundle.preprocessing;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.jmcejuela.bio.jenia.JeniaTagger;
import com.jmcejuela.bio.jenia.common.Sentence;
import com.jmcejuela.bio.jenia.common.Token;
import com.prj.bundle.utility.RegexUtility;

import edu.stanford.nlp.util.Sets;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

/**
 * @author neha
 *
 */

public class NormaliseAbstracts extends PopulateResources 
implements Callable<ArrayList<HashMap<String, TreeMap<Integer, ArrayList<ArrayList<String>>>>>>{
	
	private TreeMap<Integer, ArrayList<String>> bundle;
	private ArrayList<LinkedList<String>> relationPairs;
	private TreeMap<Integer, ArrayList<String>> proteinPatternList;
	private Properties systemProperties;
	
	//constructors
	public NormaliseAbstracts() throws IOException {
		this.systemProperties = new Properties();
        InputStream propertyStream  = new FileInputStream("config.properties");
        systemProperties.load(propertyStream);
	}

	public NormaliseAbstracts(TreeMap<Integer, ArrayList<String>> abstractBundle, 
			ArrayList<LinkedList<String>> relationList, 
			TreeMap<Integer, ArrayList<String>> parsedProteinPatternList, 
			Properties systemProperties){
		this.bundle = abstractBundle;
		this.relationPairs = relationList;
		this.proteinPatternList = parsedProteinPatternList;
		this.systemProperties = systemProperties;
	}
	
	/**
	 * Instantiate the values retrieved from corpus data into the corresponding dictionaries  
	 * @param resultHolder
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public void addCorpusResource(Hashtable<String, 
			LinkedHashMap<String,TreeMap<Integer, ArrayList<String>>>> resultHolder)
			throws IOException, ParserConfigurationException, TransformerException, InterruptedException, ExecutionException {
		
		NormaliseAbstracts normaliseInstance = new NormaliseAbstracts();
		Enumeration<String> keySet = resultHolder.keys();
		while(keySet.hasMoreElements()){
			String keyData = keySet.nextElement();
			switch (keyData) {
			case "Protein":
				normaliseInstance.setProteinEntities(resultHolder.get(keyData));
				break;
			case "Abstract":
				normaliseInstance.setAbstractCollection(resultHolder.get(keyData));
				break;
			case "Relation":
				normaliseInstance.setRelationCollection(resultHolder.get(keyData));
				break;
			default:
				break;
			}
		}
		abstractEntitySwap(normaliseInstance);
	}
	
	private String enforceSecondaryPatternMatch(String currentString,
			NavigableMap<Integer, ArrayList<String>> parsedProteinPatternList, String entityType) {
		
		/**
		 * Replace other non relation based tokens with respective NE's
		 */
		String targetString = currentString.toString();
		Iterator<Integer> sizeKeySet = parsedProteinPatternList.keySet().iterator();
		while(sizeKeySet.hasNext()){
			Iterator<String> tokenItr = parsedProteinPatternList.get(sizeKeySet.next()).iterator();
			while(tokenItr.hasNext()){
				String token = tokenItr.next();
				Pattern matchPattern = Pattern.compile(token);
				Matcher entityMatcher = matchPattern.matcher(targetString);
				while(entityMatcher.find()){
					targetString = entityMatcher.replaceAll(entityType.concat("T0"));
				}
			}
		}
		String checkString = targetString.toString().trim();
		if(!String.valueOf(checkString.charAt(checkString.length()-1)).matches("\\.|\\?|\\!")){
			System.err.println("ALERT! enforceSecondaryPatternMatch() "+checkString);
		}
		return(checkString);
	}
	

	/**
	 * Compare a matching pattern between dictionary and abstract 
	 * @param currentString
	 * @param proteinPatternList
	 * @param string
	 * @return 
	 */
	private TreeMap<Integer, String> enforcePatternMatch(StringBuilder currentString, 
			NavigableMap<Integer, ArrayList<String>> parsedProteinPatternList, String entityType) {
		
		//System.out.println("\n\t currentString>>"+currentString.toString());
		ArrayList<LinkedList<String>> relationPairArray = this.relationPairs;
		//System.out.println("\n\t ETYPE>>>"+entityType+"\t>>"+relationPairArray);
		/**
		 * Match each pair from the relation table with the mentions in the instance.
		 * Replace paired entities with respective annotation mentions
		 */
		TreeMap<Integer, String> decoyTreeMap = new TreeMap<>();
		if(!relationPairArray.isEmpty()){
			int relationIndex = 0;
			Iterator<LinkedList<String>> tier1Itr = relationPairArray.iterator();
			while(tier1Itr.hasNext()){
				int index=1, flag=0, count = 0;
				String targetString = currentString.toString();
				LinkedList<String> tier1Value = tier1Itr.next();
				//System.out.println("\n\t pair::"+tier1Value);
				if(tier1Value.size() == 1){
					/**
					String[] relationCategory = tier1Value.iterator().next().split("\\#");
					// Check for corresponding entry type
					if(relationCategory[0].equalsIgnoreCase(entityType)){
						ArrayList<String> tempArray = new ArrayList<>(Arrays.asList(targetString.split("\\s+")));
						Iterator<String> tier3Itr = tempArray.iterator();
						StringBuilder tier1BufferBuilder = new StringBuilder();
						while(tier3Itr.hasNext()){
							String tier3StrValue = tier3Itr.next();
							Matcher entityMatcher = Pattern.compile(relationCategory[1]).matcher(tier3StrValue);
							if(entityMatcher.find()){
								tier3StrValue = entityMatcher.replaceAll(entityType.concat("T"));
								tier1BufferBuilder.append(tier3StrValue
										.concat(String.valueOf(index)).concat(" "));
								tier1BufferBuilder.append(tier3StrValue
										.concat(String.valueOf(index+1)).concat(" "));
								flag=1;
							}else{
								tier1BufferBuilder.append(tier3StrValue.concat(" "));
							}
						}
						targetString = tier1BufferBuilder.toString().trim();
					}**/
					System.err.println("\n\t Single Entry error");
					System.exit(0);
				}else{
					// update pairwise entity annotation
					Iterator<String> tier2Itr = tier1Value.iterator();
					while(tier2Itr.hasNext()){
						String[] relationCategory = tier2Itr.next().split("\\#");
						// Check for corresponding entry type
						if(relationCategory[0].equalsIgnoreCase(entityType)){
							//System.out.println("\t\t\t>>"+relationCategory[1]);
							Matcher entityMatcher = Pattern.compile(relationCategory[1]).matcher(targetString);
							if(entityMatcher.find()){
								count++;
							}
						}
					}
					if(count == 2){
						flag=1;
						tier2Itr = tier1Value.iterator();
						while(tier2Itr.hasNext()){
							String[] relationCategory = tier2Itr.next().split("\\#");
							// Check for corresponding entry type
							if(relationCategory[0].equalsIgnoreCase(entityType)){
								//System.out.println("\t\t\t>>"+relationCategory[1]);
								Matcher entityMatcher = Pattern.compile(relationCategory[1]).matcher(targetString);
								if(entityMatcher.find()){
									//System.out.println("\t\t\t>>"+entityMatcher.start());
									targetString = entityMatcher.replaceFirst(entityType.concat("T"+String.valueOf(index)));
									index++;
								}
							}
						}
					}
				}
				
				if(flag == 1){
					targetString = enforceSecondaryPatternMatch(
							targetString, parsedProteinPatternList,"PROTEIN");
					String checkString = targetString.toString().trim();
					// SANITY CHECK
					if(checkString.matches("\\@\\d+")){
						System.err.println("\nenforcePatternMatch() \n ALERT! "+
								"-NE NOT PRESENT in DICTIONARY >>");
					}
					if(!String.valueOf(checkString.charAt(checkString.length()-1)).matches("\\.|\\?|\\!")){
						System.err.println("ALERT! enforcePatternMatch() "+checkString);
					}else{
						int matchCount = 0;
						Matcher decoyPattern = Pattern.compile("PROTEINT\\d+").matcher(checkString);
						while(decoyPattern.find()){
							matchCount++;
						}
						if(matchCount > 0){
							decoyTreeMap.put(relationIndex, checkString);
						}
					}
					//System.out.println("\t updated>>>"+checkString);
					relationIndex++;
				}
			}
			if(relationIndex == 0){
				String targetString = enforceSecondaryPatternMatch(
						currentString.toString(), parsedProteinPatternList,"PROTEIN");
				String checkString = targetString.toString().trim();
				// SANITY CHECK
				
				if(checkString.matches("\\@\\d+")){
					System.err.println("\nenforcePatternMatch() \n ALERT! "+
							"-NE NOT PRESENT in DICTIONARY >>");
				}
				if(!String.valueOf(checkString.charAt(checkString.length()-1)).matches("\\.|\\?|\\!")){
					System.err.println("ALERT! enforcePatternMatch() "+checkString);
				}else{
					int matchCount = 0;
					Matcher decoyPattern = Pattern.compile("PROTEINT\\d+").matcher(checkString);
					while(decoyPattern.find()){
						matchCount++;
					}
					if(matchCount > 0){
						decoyTreeMap.put(relationIndex, checkString);
					}
				}
				//System.out.println("\t rel 0 updated>>>"+checkString);
				relationIndex++;
			}
		}
		return(decoyTreeMap);
	}
	
	private ArrayList<String> splitByPattern(String token, 
			String matchPattern, String maskPattern) {

		ArrayList<String> revisedList = new ArrayList<>();
		Matcher terminalMatcher = Pattern.compile(matchPattern).matcher(token);
		if(terminalMatcher.find()){
			if (terminalMatcher.end() == token.length()){
				// terminal symbol match
				//System.out.println(" terminal:: "+token);
				revisedList.addAll(splitByPattern(token.substring(0, terminalMatcher.start()), 
						matchPattern, maskPattern));
				revisedList.add(token.substring(terminalMatcher.start(), terminalMatcher.end()));
			}else if(terminalMatcher.start() == 0){
				// start symbol match
				//System.out.println(" start:: "+token);
				revisedList.add(token.substring(terminalMatcher.start(), terminalMatcher.end()));
				revisedList.addAll(splitByPattern(token.substring(terminalMatcher.end(), token.length()), 
						matchPattern, maskPattern));
			}else{
				// mid-symbol match
				//System.out.println(" middle:: "+token);
				Matcher tokenEntityMatcher = Pattern.compile(maskPattern).matcher(token);
				if(tokenEntityMatcher.find()){
					revisedList.add(token.substring(0, terminalMatcher.start()));
					revisedList.addAll(splitByPattern(token.substring(terminalMatcher.start(), token.length()), 
							matchPattern, maskPattern));
				}
			}
		}
		if(revisedList.isEmpty()){
			ArrayList<String> decoyList = new ArrayList<>();
			char[] charArray = token.toCharArray();
			//System.out.println("\t"+token);
			for(int i=(charArray.length-1);i>=0;i--){
				if(String.valueOf(charArray[i]).matches(matchPattern)){
					decoyList.add(String.valueOf(charArray[i]));
				}else{
					decoyList.add(new String(charArray, 0, i+1));
					break;
				}
			}
			if(!decoyList.isEmpty()){
				Collections.reverse(decoyList);
				revisedList.addAll(decoyList);
			}
		}
		//System.out.println("\n rev>>"+revisedList);
		return(revisedList);
	}


	/**
	 * Genia Tagger - Biomedical Domain
	 * GeniaTagger Based implementation
	 * @param abstractString
	 * @return 
	 */
	private List<ArrayList<String>> geniaTagger(String abstractString) {
		
		abstractString = abstractString.trim();
		abstractString = abstractString.replaceAll("\\[|\\{|\\(|\\)|\\}|\\]", " ");
		ArrayList<String> decoyArray = new ArrayList<>(Arrays.asList(abstractString.split("\\s+")));
		for(int i=0;i<decoyArray.size();i++){
			if(Pattern.compile("TRIGGERPRI").matcher(decoyArray.get(i)).find()){
				decoyArray.set(i, "PROTEIN1");
			}
			if(Pattern.compile("PROTEINT").matcher(decoyArray.get(i)).find()){
				decoyArray.set(i, "PROTEIN0");
			}
		}
		
		StringBuilder reformAbstractString = new StringBuilder();
		for(String token : decoyArray){
			token = token.trim();
			if(token.length() > 1 || token.matches("\\w")){
				ArrayList<String> bufferList = 
						splitByPattern(token, "\\W+", "PROTEIN0|PROTEIN1");
				ListIterator<String> tierItr = bufferList.listIterator();
				while(tierItr.hasNext()){
					reformAbstractString.append(tierItr.next().concat(" "));
				}
			}
		}
		abstractString = reformAbstractString.toString().trim();
		//System.out.println("\n abstractPosTagger>"+abstractString);
		
		List<ArrayList<String>> complexSet = new ArrayList<>();
		ArrayList<String> wordList = new ArrayList<>();
		ArrayList<String> posTagList = new ArrayList<>();
		JeniaTagger.setModelsPath(systemProperties.getProperty("geniaModelFile"));
		Sentence baseForm = JeniaTagger.analyzeAll(abstractString, true);

		Iterator<Token> tokenItr = baseForm.iterator();
		while(tokenItr.hasNext()){
			Token currentToken = tokenItr.next();
			//substitute delimiters pos tags
			//System.out.print("\nbefore \t"+currentToken.baseForm+"\t"+currentToken.pos);
			Matcher baseFormMatcher = Pattern.compile("\\W+").matcher(currentToken.baseForm);
			if (baseFormMatcher.matches()){
				currentToken.pos="SYM";
				continue;
			}
			baseFormMatcher = Pattern.compile("PROTEIN1|PROTEIN0").matcher(currentToken.baseForm);
			if (baseFormMatcher.find()){
				currentToken.baseForm = currentToken.baseForm
						.substring(baseFormMatcher.start(), baseFormMatcher.end());
				if(currentToken.baseForm.equals("PROTEIN1")){
					currentToken.baseForm = "TRIGGERPRI";
				}else{
					currentToken.baseForm = "PROTEINT";
				}
				currentToken.pos=currentToken.baseForm;
			}
			//System.out.print("\nafter::\t"+currentToken.baseForm+"\t"+currentToken.pos);
			
			posTagList.add(currentToken.pos);
			wordList.add(currentToken.baseForm);
		}
		if(posTagList.size() != wordList.size()){
			System.err.println("INTEGRITY CHECK - geniaTagger() ~ Size Inconsistency word>>\t"+wordList.size()+" postag>>\t"+posTagList.size());
			System.exit(0);
		}
		/**
		if(!posTagList.get(wordList.size()-1).contentEquals("SYM")){
			System.err.println("SANITY CHECK - geniaTagger() ~ Terminal Symbol Absent\t"+wordList.get(wordList.size()-1));
		}**/
		complexSet.add(wordList);
		complexSet.add(posTagList);
		return(complexSet);
	}

	/**
	 * 
	 * @param paramToken
	 * @return
	 */
	public Set<String> populateSet(String paramToken){
		
		Matcher subMatcher;
		Set<String> characterSet =new HashSet<>();
		paramToken = paramToken.replaceAll("[A-Z&&[^R]]", "");
		for(String eachChar : paramToken.split("R")){
			subMatcher = Pattern.compile("\\W").matcher(eachChar);
			while(subMatcher.find()){
				characterSet.add(subMatcher.group(0));
				eachChar = eachChar.replace(subMatcher.group(0), "");
			}
			if(eachChar.length()!=0){
				characterSet.add(eachChar);
			}
		}
		return characterSet;
	}
	
	/**
	 * populate list ; current method doesn't hold for double digits
	 * @param paramToken
	 * @return
	 */
	public ArrayList<Character> populateList(String paramToken) {
			
		ArrayList<Character> tempCharArr = new ArrayList<>();
		for(char letter : paramToken.toCharArray()){
			tempCharArr.add(letter);
		}
		return(tempCharArr);
	}
	
	private ArrayList<Object> generateOffsetInfo(String currToken, String testPattern) {
		Matcher subMatcher;
		ArrayList<Object> ret = new ArrayList<>();
		ArrayList<Integer> startIndex = new ArrayList<>();
		ArrayList<Integer> endIndex = new ArrayList<>();
		ArrayList<String> patternIndex = new ArrayList<>();
		subMatcher = Pattern.compile(testPattern).matcher(currToken);
		while(subMatcher.find()){
			startIndex.add(subMatcher.start());
			endIndex.add(subMatcher.end());
			patternIndex.add(subMatcher.group(0).trim());
		}
		/**
		int i=0;
		while(i < startIndex.size()){
			System.out.println("\t"+startIndex.get(i)+"\t"+endIndex.get(i)+"\t"+patternIndex.get(i));
			i++;
		}*/
		ret.add(startIndex);
		ret.add(endIndex);
		ret.add(patternIndex);
		return(ret);
	}
	
	/**
	 * This method calls the jar-library based methods for invoking POS tagging for each input sentences.
	 * @param string
	 * @return 
	 * @throws IOException 
	 */
	private List<ArrayList<String>> abstractPosTagger(String abstractString) throws IOException {
		
		List<ArrayList<String>> complexHashSet = new ArrayList<>();
		// Call for the Tagger
		complexHashSet = geniaTagger(abstractString);
		return(complexHashSet);
	}
	
	private boolean checkWordForLowerCase(String charString) {
		
		//screen the characters for presence of Lower case and rule it out as part of another sentence
		int charCount=0;
		if(charString.length() > 1){
			for(char character : charString.toCharArray()){
				if(Character.isLowerCase(character)){
					charCount++;
				}
			}
		}
		if(charCount == charString.length()){
			return true;
		}else{
			return false;
		}
	}	
	
	private String recursiveTerminalSymbolCheck(String sentence, int sentenceSize) {
		
		// iteratively remove non period operators from the rare of the sentence
		//System.out.println("\n\t>>"+String.valueOf(sentence.charAt(sentenceSize))+"\tindex>>"+sentenceSize);
		if(String.valueOf(sentence.charAt(sentenceSize)).matches("\\.|\\?|\\!")){
			return(sentence);
		}else{
			//System.out.println("\n\t>>"+sentence.substring(0,sentenceSize));
			if(sentenceSize != 0){
				sentence = recursiveTerminalSymbolCheck(sentence.substring(0,sentenceSize), sentenceSize-1);
			}else{
				return null;
			}
		}
		//System.out.println("\n\t>>"+sentence);
		return sentence;
	}

	/**
	 * 
	 * @param cacheSentence 
	 * @param string
	 * @return 
	 */
	private String checkDocumentStructure(String documentString) {
		
		Matcher docStructureMatcher;
		TreeSet<String> structureWords = new TreeSet<>(Arrays.asList("BACKGROUND:","METHODS:",
				"RESULTS:","CONCLUSIONS:","STUDY DESIGN AND METHODS:","FINDINGS:","INTERPRETATION:",
				"METHODS AND RESULTS:","OBJECTIVES:","CASE REPORT:","DISCUSSION:","CASE:",
				"DESIGN/METHODS:","BACKGROUND AND OBJECTIVES:","RELEVANCE TO CLINICAL PRACTICE",
				"SEARCH STRATEGY:","SELECTION CRITERIA:","DATA COLLECTION AND ANALYSIS:",
				"MAIN RESULTS:","AUTHORS' CONCLUSIONS:","PURPOSE:","RATIONALE:","OBJECTIVE:",
				"CONCLUSION:","MATERIALS AND METHODS:","INTRODUCTION:","AIM OF THE STUDY:","AIMS:",
				"ETHNOPHARMACOLOGICAL RELEVANCE:","SEARCH STRATEGY:","BACKGROUND AND AIMS:","AIM:",
				"BACKGROUND & AIMS:","STUDY DESIGN:","DESIGN:","REVIEW SUMMARY:","UNLABELLED:",
				"ABSTRACT CONTEXT:","PARTICIPANTS:","METHODOLOGY:"));
		TreeMap<Integer, TreeSet<String>> docStructureWords = new TreeMap<>();
		for(String structureToken : structureWords){
			TreeSet<String> tempSet = new TreeSet<>();
			int tokenSize = structureToken.length();
			if(docStructureWords.containsKey(tokenSize)){
				tempSet = docStructureWords.get(tokenSize);
			}
			tempSet.add(structureToken);
			docStructureWords.put(tokenSize, tempSet);
		}
		
		documentString = documentString.trim();
		for(TreeSet<String> tempSet : docStructureWords.descendingMap().values()){
			for(String structureToken : tempSet){
				docStructureMatcher = Pattern.compile(structureToken,Pattern.CASE_INSENSITIVE).matcher(documentString);
				while(docStructureMatcher.find()){
					if(docStructureMatcher.group(0).matches(structureToken)){
						// complete match
						if(docStructureMatcher.start() == 0){
							documentString = documentString.replaceAll(structureToken, "").trim();
						}else{
							documentString = documentString.replaceAll(structureToken, ". ");
						}
					}else{
						// incomplete match
					}
				}
			}
		}
		return documentString.trim();
	}

	/**
	 *  reset list order
	 * @param tempList
	 * @return
	 */
	private ArrayList<String> resetListOrder(ArrayList<String> tempList) {

		TreeMap<Integer, String> decoyTreeMap = new TreeMap<>(); 
		Iterator<String> tierItr = tempList.iterator();
		while(tierItr.hasNext()){
			String decoyString = tierItr.next();
			int locationIndex = Integer.parseInt(decoyString.split("\\@")[1]);
			if(decoyTreeMap.containsKey(locationIndex)){
				locationIndex = locationIndex+1;
			}
			decoyTreeMap.put(locationIndex, decoyString);
		}
		return(new ArrayList<>(decoyTreeMap.values()));
	}
	
	/**
	 * Put the current thread to sleep 
	 */
	private void haltThreadProcess() {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public ArrayList<HashMap<String, TreeMap<Integer, ArrayList<ArrayList<String>>>>> call() 
			throws Exception {
		//System.out.println("\n\t inside call");
		ArrayList<HashMap<String, TreeMap<Integer, ArrayList<ArrayList<String>>>>> taggedSentences = 
				new ArrayList<>();
		HashMap<String, TreeMap<Integer,ArrayList<ArrayList<String>>>> subTaggedSentences = new HashMap<>();
		Iterator<Map.Entry<Integer, ArrayList<String>>> abstractIterator = bundle.entrySet().iterator();

		while (abstractIterator.hasNext()) {
			Map.Entry<Integer, ArrayList<String>> abstractMapValue = abstractIterator.next();
			Iterator<String> singleSentenceItr= abstractMapValue.getValue().iterator();
			ArrayList<String> sentences = new ArrayList<>();
			
			while(singleSentenceItr.hasNext()){
				StringBuilder cacheSentence = new StringBuilder();
				String singleSentence = singleSentenceItr.next();
				//System.out.println("\t"+ singleSentence);
				//check if there are more than 2 sentences in the input sentences
				String reformattedString = checkDocumentStructure(singleSentence);
				int terminalFlag = 0;
				if((!String.valueOf
						(reformattedString.charAt(reformattedString.length()-1))
						.matches("\\.|\\?|\\!"))){
					// check if sentence ends with period or similar operators
					terminalFlag = 1;
					cacheSentence.append(reformattedString.concat(" "));
				}
				if(terminalFlag == 0){
					if(cacheSentence.length() == 0){
						sentences.add(reformattedString.trim());
					}else{
						cacheSentence.append(reformattedString.concat(" "));
						sentences.add(cacheSentence.toString().trim());
						cacheSentence = new StringBuilder();
					}
				}
				if(cacheSentence.length() != 0){
					System.err.println("\n\t Illegal sentence terminator>>"+cacheSentence.toString().trim());
					String currentSentence = cacheSentence.toString().trim();
					int sentenceSize = currentSentence.length()-1;
					// Always ensure sentences with terminal symbol proceed to next phase
					currentSentence = recursiveTerminalSymbolCheck(currentSentence,sentenceSize);
					if(null != currentSentence){
						sentences.add(currentSentence);
					}else{
						sentences.add(cacheSentence.toString().trim().concat("."));
					}
				}
			}

			TreeMap<Integer, ArrayList<ArrayList<String>>> posTagMap = new TreeMap<>();
			TreeMap<Integer, ArrayList<ArrayList<String>>> abstractMap = new TreeMap<>();
			if(subTaggedSentences.containsKey("Vocab")){
				abstractMap = subTaggedSentences.get("Vocab");
			}
			if(subTaggedSentences.containsKey("PosTag")){
				posTagMap = subTaggedSentences.get("PosTag");
			}
			int relIndex = abstractMapValue.getKey();
			ArrayList<ArrayList<String>> tier1BufferList = new ArrayList<>();
			if(abstractMap.containsKey(relIndex)){
				tier1BufferList = abstractMap.get(relIndex);
			}
			ArrayList<ArrayList<String>> tier2BufferList = new ArrayList<>();
			if(posTagMap.containsKey(relIndex)){
				tier2BufferList = posTagMap.get(relIndex);
			}
			for(String sentence : sentences){
				//System.out.println(relIndex+"\t"+sentence);
				
				//POS tagging
				List<ArrayList<String>> decoyAbstractList = 
						abstractPosTagger(sentence.toString());
				//System.out.println("\t>>"+decoyAbstractList.get(0));
				//System.out.println("\t>>"+decoyAbstractList.get(1));
				tier1BufferList.add(decoyAbstractList.get(0));
				tier2BufferList.add(decoyAbstractList.get(1));
			}
			abstractMap.put(relIndex, tier1BufferList);
			posTagMap.put(relIndex, tier2BufferList);

			subTaggedSentences.put("Vocab", abstractMap);
			subTaggedSentences.put("PosTag", posTagMap);
		}
		taggedSentences.add(subTaggedSentences);
		haltThreadProcess();
		return(taggedSentences);
	}
	
	private TreeMap<Integer, ArrayList<LinkedList<Integer>>> 
	callPairCombination(ArrayList<String> bufferList) {

		TreeMap<Integer, ArrayList<LinkedList<Integer>>> tier1ResultBufferMap = new TreeMap<>();
		ArrayList<Integer> tier1BufferList = new ArrayList<>(
				IntStream.range(0, bufferList.size())
				.boxed().collect(Collectors.toList()));
		ArrayList<Integer> completeList = new ArrayList<>(tier1BufferList.stream()
				.filter(index -> bufferList.get(index).matches("TRIGGERPRI|PROTEINT"))
				.collect(Collectors.toList()));
		LinkedList<Integer> pairedList = new LinkedList<>(tier1BufferList.stream()
				.filter(index -> bufferList.get(index).matches("TRIGGERPRI"))
				.collect(Collectors.toList()));
		
		// make combinations
		ArrayList<LinkedList<Integer>> tier2BufferList = new ArrayList<>();
		for(int i=0;i<completeList.size()-1;i++){
			for(int j=i+1;j<completeList.size();j++){
				tier2BufferList.add(
						new LinkedList<>(Arrays.asList(completeList.get(i), completeList.get(j))));
			}
		}
		
		ListIterator<LinkedList<Integer>> tier1Itr = tier2BufferList.listIterator();
		while(tier1Itr.hasNext()){
			LinkedList<Integer> tier1LList = tier1Itr.next();
			ArrayList<LinkedList<Integer>> tier3BufferList = new ArrayList<>();
			if(tier1LList.equals(pairedList)){
				if(tier1ResultBufferMap.containsKey(1)){
					tier3BufferList = tier1ResultBufferMap.get(1);
				}
				tier3BufferList.add(tier1LList);
				tier1ResultBufferMap.put(1, tier3BufferList);
			}else{
				if(tier1ResultBufferMap.containsKey(-1)){
					tier3BufferList = tier1ResultBufferMap.get(-1);
				}
				tier3BufferList.add(tier1LList);
				tier1ResultBufferMap.put(-1, tier3BufferList);
			}
		}
		return(tier1ResultBufferMap);
	}
	
	/**
	 * reorganize the sentences
	 * @param decoyMap
	 * @return
	 */
	private TreeMap<Integer, ArrayList<String>> reframeSentence(
			 TreeMap<Integer, ArrayList<ArrayList<String>>> decoyMap) {

		TreeMap<Integer, ArrayList<String>> returnTreeMap = new TreeMap<>();
		Iterator<Map.Entry<Integer, ArrayList<ArrayList<String>>>> tier1Itr = 
				decoyMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<ArrayList<String>>> tier1MapValue = tier1Itr.next();
			Iterator<ArrayList<String>> tier2Itr = tier1MapValue.getValue().iterator();
			ArrayList<String> tier1BufferList = new ArrayList<>();
			while(tier2Itr.hasNext()){
				ArrayList<String> tier2ListValue = tier2Itr.next();
				int index = 1;
				for(int i=0;i<tier2ListValue.size();i++){
					if(Pattern.compile("PROTEINT").matcher(tier2ListValue.get(i)).find()){
						tier2ListValue.set(i, "PROTEINT0");
					}else if (Pattern.compile("TRIGGERPRI").matcher(tier2ListValue.get(i)).find()){
						tier2ListValue.set(i, "PROTEINT"+index);
						index++;
					}
				}
				
				tier1BufferList.add(tier2ListValue.stream()
						.reduce((prev, curr) -> prev+" "+curr).get());
			}
			returnTreeMap.put(tier1MapValue.getKey(),tier1BufferList);
		}
		return(returnTreeMap);
	}
	
	private TreeMap<Integer, LinkedHashMap<String, String>> generateClassInstances(
			String docId, String resourceType, Integer sentIndex, ArrayList<String> bufferList,
			TreeMap<Integer, ArrayList<LinkedList<Integer>>> bufferMap, 
			TreeMap<Integer, LinkedHashMap<String, String>> tier1ResultBufferMap) {

		Iterator<Map.Entry<Integer, ArrayList<LinkedList<Integer>>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<LinkedList<Integer>>> tier1MapValue = tier1Itr.next();
			Iterator<LinkedList<Integer>> tier2Itr = tier1MapValue.getValue().iterator();
			LinkedHashMap<String, String> tier2ResultBufferMap = new LinkedHashMap<>();
			if(tier1ResultBufferMap.containsKey(tier1MapValue.getKey())){
				tier2ResultBufferMap = tier1ResultBufferMap.get(tier1MapValue.getKey());
			}
			//System.out.println("\n\t"+tier1MapValue.getKey()+"\t"+tier1MapValue.getValue());
			while(tier2Itr.hasNext()){
				ArrayList<String> tier1BufferList = new ArrayList<>(bufferList);
				LinkedList<Integer> tier2LList = tier2Itr.next();
				for(int i=0;i<tier1BufferList.size();i++){
					if(tier2LList.contains(i)){
						tier1BufferList.set(i, "TRIGGERPRI");
					}else if(tier1BufferList.get(i).matches("PROTEINT|TRIGGERPRI")){
						/**
						if(resourceType.equals("PosTag")){
							tier1BufferList.set(i, "NN");
						}else{
							tier1BufferList.set(i, "PROTEINT");
						}**/
						tier1BufferList.set(i, "PROTEINT");
					}
				}
				
				String currentString = tier1BufferList.stream()
						.reduce((prev, curr) -> prev+" "+curr).get().trim();
				
				boolean fillStatus = true;
				String subPattern = docId.concat("@"+(sentIndex)+"#");
				ArrayList<String> filterPatternList = new ArrayList<>();
				//if(tier1MapValue.getKey() == -1){
					for(String term : tier2ResultBufferMap.keySet()){
						Matcher matchPattern = Pattern.compile(subPattern).matcher(term);
						//System.out.println("ter>>"+term+"\t"+subIndex);
						if(matchPattern.find()){
							//System.out.println(">>"+term);
							if(Pattern.compile(subPattern).matcher(term).find()){
								filterPatternList.add(term);
							}
							if(tier2ResultBufferMap.get(term).contentEquals(currentString)){
								fillStatus = false;
							}
						}
					}
				//}
				
				String subIndex = subPattern.concat(String.valueOf((filterPatternList.size()+1)));
				//System.out.println("\n\t"+tier1MapValue.getKey()+"\t"+subIndex+"\t"+currentString);
				
				//if (!tier2ResultBufferMap.values().contains(currentString)){
				if (fillStatus){
					//System.out.println("add>>>"+subIndex);
					tier2ResultBufferMap.put(subIndex, currentString);
				}
			}
			tier1ResultBufferMap.put(tier1MapValue.getKey(), tier2ResultBufferMap);
		}
		return(tier1ResultBufferMap);
	}

	private ArrayList<String> normalizationForDuplicateTesting(LinkedList<Integer> bufferLList, 
			ArrayList<String> bufferArray) {
		
		for(int i=0;i<bufferArray.size();i++){
			if(bufferArray.get(i).matches("TRIGGERPRI|PROTEINT")){
				bufferArray.set(i, "PROTEINT");
			}
		}
		
		Iterator<Integer> tier1Itr = bufferLList.iterator();
		while(tier1Itr.hasNext()){
			Integer tier1IntVal = tier1Itr.next();
			bufferArray.set(tier1IntVal, "TRIGGERPRI");
		}
		
		
		return(bufferArray);
	}
	
	private TreeMap<Integer, ArrayList<LinkedList<Integer>>> removeInstanceDuplicates(
			TreeMap<Integer, ArrayList<LinkedList<Integer>>> tier1BufferMap,
			ArrayList<String> bufferArray, TreeMap<Integer, ArrayList<ArrayList<String>>> bufferMap) {

		if(tier1BufferMap.containsKey(-1)){
			Iterator<LinkedList<Integer>> tier1Itr = tier1BufferMap.get(-1).iterator();
			while(tier1Itr.hasNext()){
				ArrayList<String> tier1BufferArray = new ArrayList<>(bufferArray);
				// re-frame the candidate instance
				LinkedList<Integer> compareList = new LinkedList<>(tier1Itr.next());
				//System.out.println("compareList>>"+compareList);
				tier1BufferArray = normalizationForDuplicateTesting(compareList, tier1BufferArray);
				Iterator<Map.Entry<Integer, ArrayList<ArrayList<String>>>> tier2Itr = 
						bufferMap.entrySet().iterator();
				while(tier2Itr.hasNext()){
					Map.Entry<Integer, ArrayList<ArrayList<String>>> tier2MapValue = tier2Itr.next();
					Iterator<ArrayList<String>> tier3Itr = tier2MapValue.getValue().iterator();
					while(tier3Itr.hasNext()){
						ArrayList<String> tier3ListValue = tier3Itr.next();
						//System.out.println(tier3ListValue);
						ArrayList<Integer> tier2BufferArray = new ArrayList<>(
								IntStream.range(0, tier3ListValue.size())
								.boxed().collect(Collectors.toList()));
						LinkedList<Integer> pairedList = new LinkedList<>(tier2BufferArray.stream()
								.filter(index -> tier3ListValue.get(index).matches("TRIGGERPRI"))
								.collect(Collectors.toList()));
						//System.out.println(pairedList);
						ArrayList<String> tier3BufferArray = new ArrayList<>(tier3ListValue);
						tier3BufferArray = normalizationForDuplicateTesting(pairedList, tier3BufferArray);
						if(tier1BufferArray.equals(tier3BufferArray)){
							//System.out.println("\t>>"+tier1BufferMap.get(-1)+"\t>>"+pairedList+"\t>>"+compareList);
							tier1Itr.remove();
							break;
						}
					}
				}
			}
		}
		return(tier1BufferMap);
	}

	private TreeMap<Integer, LinkedHashMap<String, String>> isolateClassInstances(
			String docId, String resourceType, 
			TreeMap<Integer, ArrayList<ArrayList<String>>> bufferMap) {

		TreeMap<Integer, LinkedHashMap<String, String>> tier1ResultBufferMap = new TreeMap<>();
		Iterator<Map.Entry<Integer, ArrayList<ArrayList<String>>>> tier1Itr = 
				bufferMap.entrySet().iterator();
		while(tier1Itr.hasNext()){
			Map.Entry<Integer, ArrayList<ArrayList<String>>> tier1MapValue = tier1Itr.next();
			Iterator<ArrayList<String>> tier2Itr = tier1MapValue.getValue().iterator();
			while(tier2Itr.hasNext()){
				ArrayList<String> tier2ListValue = tier2Itr.next();
				TreeMap<Integer, ArrayList<LinkedList<Integer>>> tier2ResultBufferMap = 
						callPairCombination(tier2ListValue);
				//System.out.println("\n\t>>"+tier1MapValue.getKey()+"\t"+tier2ListValue);
				
				tier2ResultBufferMap = removeInstanceDuplicates(tier2ResultBufferMap, 
						new ArrayList<>(tier2ListValue), new TreeMap<>(bufferMap));
				tier1ResultBufferMap = generateClassInstances(docId, resourceType, 
						tier1MapValue.getKey(), tier2ListValue, 
						tier2ResultBufferMap, tier1ResultBufferMap);
			}
			
		}
		return(tier1ResultBufferMap);
	}

	/**
	 * Identify the MWE/SW from corpus abstracts and replace them with corresponding NE's
	 * @param normaliseInstance
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void abstractEntitySwap(NormaliseAbstracts normaliseInstance) 
			throws IOException, ParserConfigurationException, TransformerException, 
			InterruptedException, ExecutionException {
		
		FileWriter tier1FileWS = null, tier2FileWS = null;
		BufferedWriter tier1BuffWS = null, tier2BuffWS = null;
		/**
		 * Compare the patterns against the abstract text 
		 */
		Iterator<String> documentIdItr = 
				normaliseInstance.abstractCollection.keySet().iterator();
		
		//Random poolSizeGenerator = new Random();
		Integer threadPoolSize;
		if(normaliseInstance.abstractCollection.keySet().size() > 1){
			threadPoolSize = (normaliseInstance.abstractCollection.keySet().size()/2);
		}else{
			threadPoolSize = 1;
		}
		
		System.out.println("\n"+threadPoolSize);
		long beginSysTime = System.currentTimeMillis();
		ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(threadPoolSize);
		int i=0;
		Integer totSize = 0, relSize=0;
		while(documentIdItr.hasNext()){
			String docId = documentIdItr.next();
			System.out.println("\t"+docId+"\t"+i);
			/**
			if(docId.trim().equals("BioInfer_d613")){
				System.out.println("\t******************"+i);
				//System.exit(0);
			//}**/
				i++;
				/**
				if(i==34){
					break;
				}**/
				/**
				 * Gather entity pattern list
				 */
				TreeMap<Integer, ArrayList<String>> parsedProteinPatternList = 
						new TreeMap<>();
				if(normaliseInstance.proteinEntities.containsKey(docId)){
					parsedProteinPatternList = normaliseInstance.proteinEntities.get(docId);
				}
				//System.out.println(" parsedProteinPatternList>>>"+parsedProteinPatternList);
				
				ArrayList<LinkedList<String>> relationList = new ArrayList<>();
				if(normaliseInstance.relationCollection.containsKey(docId)){
					TreeMap<Integer, ArrayList<String>> relationTree = 
							normaliseInstance.relationCollection.get(docId);
					for(ArrayList<String> tempList : relationTree.values()){
						//System.out.println("\n\t ::"+tempList);
						//tempList = resetListOrder(tempList);
						relationList.add(new LinkedList<>());
					}
				}
				relSize = relSize+relationList.size();

				TreeMap<Integer, ArrayList<String>> abstractBundle = new TreeMap<>();
				if(normaliseInstance.abstractCollection.containsKey(docId)){
					abstractBundle = normaliseInstance.abstractCollection.get(docId);
				}

				int pIns=0,vIns=0;
				NormaliseAbstracts workerThread = new NormaliseAbstracts(abstractBundle, 
						relationList, parsedProteinPatternList, systemProperties);
				Future<ArrayList<HashMap<String, TreeMap<Integer, ArrayList<ArrayList<String>>>>>> taskCollector = 
						threadPoolExecutor.submit(workerThread);
				Iterator<HashMap<String, TreeMap<Integer, ArrayList<ArrayList<String>>>>> tier1Itr = 
						taskCollector.get().iterator();
				while(tier1Itr.hasNext()){
					HashMap<String, TreeMap<Integer, ArrayList<ArrayList<String>>>> tier1MapValue = 
							tier1Itr.next();
					Iterator<Map.Entry<String, TreeMap<Integer, ArrayList<ArrayList<String>>>>> tier2Itr = 
							tier1MapValue.entrySet().iterator();
					TreeMap<String, TreeMap<Integer, LinkedHashMap<String, String>>> tier0ResultBufferMap = new TreeMap<>();
					while(tier2Itr.hasNext()){
						Map.Entry<String, TreeMap<Integer, ArrayList<ArrayList<String>>>> tier2MapValue = 
								tier2Itr.next();

						TreeMap<Integer, LinkedHashMap<String, String>> tier1ResultBufferMap = 
								isolateClassInstances(docId, tier2MapValue.getKey(), 
										tier2MapValue.getValue());
						tier0ResultBufferMap.put(tier2MapValue.getKey(), tier1ResultBufferMap);
						TreeMap<Integer, ArrayList<String>> returnTreeMap = 
								reframeSentence(tier2MapValue.getValue());
						
						if(tier2MapValue.getKey().equals("PosTag")){
							Iterator<ArrayList<String>> t1Itr = returnTreeMap.values().iterator();
							while(t1Itr.hasNext()){
								Iterator<String> t2Itr = t1Itr.next().iterator();
								while(t2Itr.hasNext()){
									if(Pattern.compile("PROTEINT[\\d+&&[^0]]")
											.matcher(t2Itr.next()).find()){
										totSize++;
									}
								}
							}
						}
						
						if(tier2MapValue.getKey().equals("PosTag")){
							Iterator<Map.Entry<Integer, LinkedHashMap<String, String>>> t1Itr = 
									tier1ResultBufferMap.entrySet().iterator();
							while(t1Itr.hasNext()){
								Map.Entry<Integer, LinkedHashMap<String, String>> t1V = t1Itr.next();
								pIns = pIns+t1V.getValue().size();
							}
						}
						
						if(tier2MapValue.getKey().equals("Vocab")){
							Iterator<Map.Entry<Integer, LinkedHashMap<String, String>>> t1Itr = 
									tier1ResultBufferMap.entrySet().iterator();
							while(t1Itr.hasNext()){
								Map.Entry<Integer, LinkedHashMap<String, String>> t1V = t1Itr.next();
								vIns = vIns+t1V.getValue().size();
							}
						}
						// select to initialize or append file
						if(i == 1){
							if(tier2MapValue.getKey().equals("Vocab")){
								tier1FileWS = new FileWriter(
										systemProperties.getProperty("processedOriginalFile"));
							}else if(tier2MapValue.getKey().equals("PosTag")){
								tier1FileWS = new FileWriter(
										systemProperties.getProperty("processedPOSTaggedFile"));
							}
						}else{
							if(tier2MapValue.getKey().equals("Vocab")){
								tier1FileWS = new FileWriter(
										systemProperties.getProperty("processedOriginalFile"),true);
							}else if(tier2MapValue.getKey().equals("PosTag")){
								tier1FileWS = new FileWriter(
										systemProperties.getProperty("processedPOSTaggedFile"),true);
							}
						}
						tier1BuffWS = new BufferedWriter(tier1FileWS);
						Iterator<Map.Entry<Integer, ArrayList<String>>> tier3Itr = 
								returnTreeMap.entrySet().iterator();
						while(tier3Itr.hasNext()){
							Map.Entry<Integer, ArrayList<String>> tier3MapValue = tier3Itr.next();
							Iterator<String> tier31Itr = tier3MapValue.getValue().iterator();
							while(tier31Itr.hasNext()){
								String tier31StringValue = tier31Itr.next();
								String refId = docId.concat("@"+String.valueOf(tier3MapValue.getKey()));
								tier1BuffWS.write(refId.concat("\t"));
								tier1BuffWS.write(tier31StringValue.trim());
								tier1BuffWS.newLine();
							}
						}
						tier1BuffWS.flush();
						tier1BuffWS.close();
						//}
					}

					//if(tier0ResultBufferMap.containsKey("PosTag")){
					Iterator<Map.Entry<String, TreeMap<Integer, LinkedHashMap<String, String>>>> tier4Itr = 
							tier0ResultBufferMap.entrySet().iterator();
					while(tier4Itr.hasNext()){
						Map.Entry<String, TreeMap<Integer, LinkedHashMap<String, String>>> tier4MapValue = 
								tier4Itr.next();
						if(i == 1){
							if(tier4MapValue.getKey().equals("Vocab")){
								tier2FileWS = new FileWriter(
										systemProperties.getProperty("instancedOriginalFile"));
							}else if(tier4MapValue.getKey().equals("PosTag")){
								tier2FileWS = new FileWriter(
										systemProperties.getProperty("instancePosTaggedFile"));
							}
						}else{
							if(tier4MapValue.getKey().equals("Vocab")){
								tier2FileWS = new FileWriter(
										systemProperties.getProperty("instancedOriginalFile"),true);
							}else if(tier4MapValue.getKey().equals("PosTag")){
								tier2FileWS = new FileWriter(
										systemProperties.getProperty("instancePosTaggedFile"),true);
							}
						}
						
						tier2BuffWS = new BufferedWriter(tier2FileWS);
						
						Iterator<Map.Entry<Integer, LinkedHashMap<String, String>>> tier5Itr = 
								tier4MapValue.getValue().entrySet().iterator();
						while(tier5Itr.hasNext()){
							Map.Entry<Integer, LinkedHashMap<String, String>> tier5MapValue = 
									tier5Itr.next();
							Iterator<Map.Entry<String, String>> tier6Itr = 
									tier5MapValue.getValue().entrySet().iterator();
							while(tier6Itr.hasNext()){
								Map.Entry<String, String> tier6MapValue = tier6Itr.next();
								if (tier0ResultBufferMap.get("PosTag")
										.get(tier5MapValue.getKey()).keySet()
										.contains(tier6MapValue.getKey())){
									tier2BuffWS.write(tier5MapValue.getKey().toString()+"\t");
									tier2BuffWS.write(tier6MapValue.getKey().toString()+"\t");
									tier2BuffWS.write(tier6MapValue.getValue().toString());
									tier2BuffWS.newLine();
								}
							}
						}
						tier2BuffWS.flush();
						tier2BuffWS.close();
					}
					//}
				}
				if(pIns != vIns){
					System.err.println("critical error in size");
					System.exit(0);
				}
			//}
		}
		threadPoolExecutor.shutdown();
		System.out.println("\n\t Total Size>>"+totSize+"\t>>"+relSize);
		System.out.println("\n Total Execution Time:-"+(System.currentTimeMillis()-beginSysTime)/1000);
	}
	
}





