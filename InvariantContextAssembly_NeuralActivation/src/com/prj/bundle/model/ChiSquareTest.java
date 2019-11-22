/**
 * 
 */
package com.prj.bundle.model;


import org.ejml.simple.SimpleMatrix;

/**
 * @author iasl
 *
 */
public class ChiSquareTest {

	private SimpleMatrix expectedMatrix;
	private SimpleMatrix observedMatrix;
	
	/**
	 * 
	 */
	public ChiSquareTest() {

	}

	public ChiSquareTest(SimpleMatrix bufferMatrix) {
		expectedMatrix = new SimpleMatrix(bufferMatrix.numRows(), bufferMatrix.numCols());
		observedMatrix = bufferMatrix;
	}
	
	private boolean generateExpectedValueMatrix() {
		
		boolean generateSuccess = true;
		SimpleMatrix tier1BufferMatrix = 
				new SimpleMatrix(observedMatrix.numRows(), observedMatrix.numCols());
		double denom = observedMatrix.elementSum();
		for(int i=0;i<observedMatrix.numRows();i++){
			tier1BufferMatrix.setRow(i, 0, observedMatrix.extractVector(true, i).elementSum(), 
					observedMatrix.extractVector(false, i).elementSum());
		}
	
		for(int i=0;i<tier1BufferMatrix.numRows();i++){
			double num = 0;
			for(int j=0;j<tier1BufferMatrix.numRows();j++){
				num = tier1BufferMatrix.get(i, i)*tier1BufferMatrix.get(j, 1);
				double score = (num/denom);
				if(score < 5){
					generateSuccess = false;
				}
				expectedMatrix.set(i, j, score);
			}
		}
		
		return(generateSuccess);
	}
	
	private double computeTestStastistic() {

		double chiScore = 0;
		for(int i=0;i<observedMatrix.numRows();i++){
			for(int j=0;j<observedMatrix.numCols();j++){
				double deviation = Math.pow(
						Math.abs((observedMatrix.get(i, j)-expectedMatrix.get(i, j)))-(0.5),2);
				chiScore = (chiScore + (deviation/expectedMatrix.get(i, j)));
			}
		}
		return(chiScore);
	}

	public void callChiSquareTestEvaluator() {
		
		if(generateExpectedValueMatrix()){
			double chiSigValue = computeTestStastistic();
			System.out.println("************************************\t"+chiSigValue);
		}
	}

}
