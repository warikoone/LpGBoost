/**
 * 
 */
package com.prj.bundle.metrics;

import org.ejml.simple.SimpleMatrix;

/**
 * @author iasl
 *
 */
public class LogLikelihoodTest extends ChiSquareTest{

	private SimpleMatrix expectedMatrix;
	private SimpleMatrix observedMatrix;
	
	/**
	 * 
	 */
	public LogLikelihoodTest() {

	}

	public LogLikelihoodTest(SimpleMatrix bufferMatrix) {

		super(bufferMatrix);
		expectedMatrix = new SimpleMatrix(bufferMatrix.numRows(), bufferMatrix.numCols());
		observedMatrix = bufferMatrix;
	}

	private double computeGSquareStastistic() {
		
		double llrScore = 0;
		for(int i=0;i<observedMatrix.numRows();i++){
			for(int j=0;j<observedMatrix.numCols();j++){
				double deviation = Math.log((observedMatrix.get(i, j)/expectedMatrix.get(i, j)));
				llrScore = (llrScore + (observedMatrix.get(i, j)*deviation));
			}
		}
		llrScore = (2*llrScore);
		return(llrScore);
	}
	
	public double callLLRTestEvaluator() {

		double llrSigValue = -1;
		if(generateExpectedValueMatrix()){
			expectedMatrix = super.expectedMatrix;
			llrSigValue = computeGSquareStastistic();
		}
		return(llrSigValue);
	}

}
