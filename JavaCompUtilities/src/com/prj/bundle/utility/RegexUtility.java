/**
 * 
 */
package com.prj.bundle.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author iasl
 *
 */
public class RegexUtility {

	public RegexUtility() {

	}
	
	/**
	 * Generate patterns for each token data
	 * @param tokenString
	 * @return 
	 */
	public static String patternBuilder(String tokenString) {
		
		Matcher tempMatch = Pattern.compile("(\\W)").matcher(tokenString);
		StringBuilder tempPatternBuilder = new StringBuilder();
		while(tempMatch.find()){
			int startIndex = tempMatch.start();
			int endIndex = (tempMatch.end()-1);
			String nonCharGroup = tempMatch.group(0);
			String replacePattern = "";
			if(!nonCharGroup.matches("\\s")){
				// add escape characters to non letter, non digit and non space match 
				replacePattern = "\\".concat(nonCharGroup);
			}else{
				// keep space as it is
				replacePattern = nonCharGroup;
			}
			
			if((startIndex == 0) && (endIndex != tokenString.length()-1)){
				tempPatternBuilder.append(replacePattern);
				tokenString = tokenString.substring(endIndex+1,tokenString.length());
			}else if((startIndex != 0) && (endIndex == tokenString.length()-1)){
				tempPatternBuilder.append(tokenString.substring(0,startIndex).concat(replacePattern));
				tokenString = "";
			}else if((startIndex != 0) && (endIndex != tokenString.length()-1)){
				tempPatternBuilder.append(tokenString.substring(0, startIndex).concat(replacePattern));
				tokenString = tokenString.substring(endIndex+1,tokenString.length());
			}else if((startIndex == 0) && (endIndex == 0)){
				tempPatternBuilder.append(replacePattern);
				tokenString="";
				break;
			}else{
				break;
			}
			tempMatch = Pattern.compile("(\\W)").matcher(tokenString);
		}
		if(tempPatternBuilder.length()!=0){
			tokenString = tempPatternBuilder.append(tokenString).toString().trim();
		}
		return(tokenString);
	}

}
