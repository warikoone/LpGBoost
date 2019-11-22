/**
 * 
 */
package com.prj.bundle.wrapper;

import java.util.ArrayList;

/**
 * @author iasl
 *
 */
public class ICONRepresentationAttributes {

	private ArrayList<String> contextSequenceRepresentation;
	private ArrayList<String> contextVectorRepresentation;
	
	/**
	 * 
	 */
	public ICONRepresentationAttributes() {
		
	}

	public ArrayList<String> getContextSequenceRepresentation() {
		return contextSequenceRepresentation;
	}

	public void setContextSequenceRepresentation(ArrayList<String> contextSequenceRepresentation) {
		this.contextSequenceRepresentation = contextSequenceRepresentation;
	}

	public ArrayList<String> getContextVectorRepresentation() {
		return contextVectorRepresentation;
	}

	public void setContextVectorRepresentation(ArrayList<String> contextVectorRepresentation) {
		this.contextVectorRepresentation = contextVectorRepresentation;
	}

}
