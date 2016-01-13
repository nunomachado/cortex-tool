//
// Copyright (C) 2007 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.symbc.numeric;

import java.util.HashMap;

import gov.nasa.jpf.jvm.IntChoiceGenerator;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;
//import gov.nasa.jpf.symbc.SymbolicInstructionFactory;


public class PCChoiceGenerator extends IntIntervalGenerator {

	PathCondition[] PC;
	boolean isReverseOrder;
	

	int offset; // to be used in the CFG
	public int getOffset() { return offset;}
	public void setOffset(int off) {
		//if(SymbolicInstructionFactory.debugMode) System.out.println("offset "+off);
		offset=off;
	}
	String methodName; // to be used in the CFG
	public String getMethodName() { return methodName;}
	public void setMethodName(String name) {
		//if(SymbolicInstructionFactory.debugMode) System.out.println("methodName "+ name);
		methodName = name;
	}

	@SuppressWarnings("deprecation")
	public PCChoiceGenerator(int size) {
		super(0, size - 1);
		PC = new PathCondition[size];
		isReverseOrder = false;		
	}

	/*
	 * If reverseOrder is true, the PCChoiceGenerator
	 * explores paths in the opposite order used by
	 * the default constructor. If reverseOrder is false
	 * the usual behavior is used.
	 */
	@SuppressWarnings("deprecation")
	public PCChoiceGenerator(int size, boolean reverseOrder) {
		super(0, size - 1, reverseOrder ? -1 : 1);
		PC = new PathCondition[size];
		isReverseOrder = reverseOrder;
	}

	public boolean isReverseOrder() {
		return isReverseOrder;
	}

	// sets the PC constraints for the current choice
	public void setCurrentPC(PathCondition pc) {
		PC[getNextChoice()] = pc;
	}

	// returns the PC constraints for the current choice
	public PathCondition getCurrentPC() {
		PathCondition pc;

		pc = PC[getNextChoice()];
		if (pc != null) {
			return pc.make_copy();
		} else {
			return null;
		}
	}

	public IntChoiceGenerator randomize() {
		return new PCChoiceGenerator(PC.length, random.nextBoolean());
	}

	public void setNextChoice(int nextChoice){
		super.next = nextChoice;
		System.out.println("setting next choice");
	}
}
