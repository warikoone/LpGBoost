/**
 * 
 */
package com.prj.bundle.metrics;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import com.prj.bundle.wrapper.DifferentialContextAttributes;

/**
 * @author iasl
 *
 */
public class AlgebraicInvarianceEval {

	private Double p20;
	private Double p11;
	private Double p02;
	/**
	 * 
	 */
	public AlgebraicInvarianceEval() {
		p20 = new Double(0);
		p11 = new Double(0);
		p02 = new Double(0);
		
	}

	private double calculateFeatureFormInvaraince() {

		double invarianceScore = 0;
		double p20Sq = Math.pow(p20, 2);
		double p11Sq = Math.pow(p11, 2);
		double p02Sq = Math.pow(p02, 2);
		invarianceScore = ((p11Sq/2) + p20Sq + p02Sq);
		return(invarianceScore);
	}
	
	public double generatePolynomialForm(double refCoefX, double refCoefY) {
		
		p20 = Math.pow(refCoefX, 2);
		p11 = (refCoefX*refCoefY);
		p02 = Math.pow(refCoefY, 2);
		double invarianceScore = calculateFeatureFormInvaraince();
		return(invarianceScore);
	}
	
	private double calculateVariantEntropy(TreeMap<Integer, LinkedHashMap<String, Integer>> bufferMap, 
			int layer, int baseSize, String seedPosToken, String comparePosToken) {
		
		double leastInstance = Collections.min(Arrays.asList(
				bufferMap.get(layer).get(seedPosToken), bufferMap.get(layer).get(comparePosToken))).doubleValue();
		double totalSize = bufferMap.get(layer).values().stream()
				.reduce((curr, prev)-> curr+prev).get().doubleValue();
		double tokenProbability = (leastInstance/totalSize);
		/**
		 * Bit Information
		 */
		tokenProbability = (1*(Math.log(tokenProbability)/Math.log(baseSize)));
		return(tokenProbability);
	}

	public double compareInvarianceQuotient(
			DifferentialContextAttributes seedFormAttributes,
			DifferentialContextAttributes compareFormAttributes, 
			TreeMap<Integer, LinkedHashMap<String, Integer>> bufferMap) {

		LinkedList<String> seedReferenceList = seedFormAttributes.getReferenceFeature();
		LinkedList<String> compareReferenceList = compareFormAttributes.getReferenceFeature();
		LinkedHashMap<String, Double> seedForm = seedFormAttributes.getDifferentialAttributes();
		LinkedHashMap<String, Double> compareForm = compareFormAttributes.getDifferentialAttributes();
		ArrayList<Double> tier1BufferList = new ArrayList<>();
		double invarianceThreshold = 1;
		double rmsError = -1;
		int layer = 0, baseSize = seedReferenceList.size();
		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.UP);
		//System.out.println("\t"+seedReferenceList+"\t"+compareReferenceList);
		//System.out.println("\t"+seedForm+"\t"+compareForm);
		Iterator<String> tier1Itr = seedForm.keySet().iterator();
		while(tier1Itr.hasNext()){
			double invarianceConfidence = 0;
			String keyValue = tier1Itr.next();
			if(seedForm.get(keyValue) > compareForm.get(keyValue)){
				if(seedForm.get(keyValue) == new Integer(0).doubleValue()){
					invarianceConfidence = new Integer(1).doubleValue();
				}else{
					invarianceConfidence = Double.valueOf(
							df.format(compareForm.get(keyValue)/seedForm.get(keyValue)));
				}
			}else{
				if(compareForm.get(keyValue) == new Integer(0).doubleValue()){
					invarianceConfidence = new Integer(1).doubleValue();
				}else{
					invarianceConfidence = Double.valueOf(
							df.format(seedForm.get(keyValue)/compareForm.get(keyValue)));
				}
			}
			
				//System.out.println("v1\t"+seedReferenceList.get(layer)+"\tv2\t"+compareReferenceList.get(layer));
				double varianceError = calculateVariantEntropy(
						bufferMap, layer, baseSize, seedReferenceList.get(layer), 
						compareReferenceList.get(layer));
				double errorMargin = invarianceThreshold + varianceError;
				//System.out.println("IC\t"+invarianceConfidence+"\tIT\t"+invarianceThreshold+"\tEM\t"+errorMargin);
				if(invarianceConfidence >= errorMargin){
					double invarianceDeviation = (invarianceThreshold - invarianceConfidence); 
					invarianceDeviation = Math.pow(invarianceDeviation, 2);
					tier1BufferList.add(invarianceDeviation);
				}else{
					break;
				}
			layer++;
			/**
			if(invarianceThreshold > invarianceConfidence){
				invarianceThreshold = invarianceConfidence;
			}**/
		}
		
		if(tier1BufferList.size() == seedForm.size()){
			rmsError = tier1BufferList.stream().reduce((prev,curr)-> prev+curr).get().doubleValue();
			rmsError = Math.sqrt((rmsError/Integer.valueOf(tier1BufferList.size()).doubleValue()));
		}
		//System.out.println("final\t"+rmsError);
		return(rmsError);
	}

}
