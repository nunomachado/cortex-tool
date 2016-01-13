//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.heap.HeapNode;
import gov.nasa.jpf.symbc.heap.Helper;
import gov.nasa.jpf.symbc.heap.SymbolicInputHeap;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.SymbolicStringBuilder;
//import gov.nasa.jpf.symbc.uberlazy.TypeHierarchy;

public class GETSTATIC extends gov.nasa.jpf.jvm.bytecode.GETSTATIC {
	public GETSTATIC(String fieldName, String clsName, String fieldDescriptor){
		super(fieldName, clsName, fieldDescriptor);
	}

	//private int numNewRefs = 0; // # of new reference objects to account for polymorphism -- work of Neha Rungta -- needs to be updated

	boolean abstractClass = false;


	@Override
	public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
		//System.out.println("GETSTATIC");
		ChoiceGenerator<?> prevHeapCG = null;
		HeapNode[] prevSymRefs = null;
		int numSymRefs = 0;

		Config conf = ti.getVM().getConfig();
		String[] lazy = conf.getStringArray("symbolic.lazy");
		if (lazy == null || !lazy[0].equalsIgnoreCase("true")){
			// Execution follows this path even with symbolic variables.
			//System.out.println("DONE1?");
			return super.execute(ss,ks,ti);
		}
		//TODO: fix polymorphism and subtypes
		//		String subtypes = conf.getString("symbolic.lazy.subtypes", "false");
		//		if(!subtypes.equals("false") &&
		//				TypeHierarchy.typeHierarchies == null) {
		//			TypeHierarchy.buildTypeHierarchy(ti);
		//		}

		FieldInfo fi = getFieldInfo();
		if (fi == null) {
			//System.out.println("DONE2?");
			return ti.createAndThrowException("java.lang.NoSuchFieldException",
					(className + '.' + fname));
		}

		ClassInfo ci = fi.getClassInfo();
		// start: not sure if this code should stay here
		//	    if (!mi.isClinit(ci) && requiresClinitCalls(ti, ci))
		//			  return ti.getPC();
		// end: not sure if this code should stay here

		ElementInfo ei = ks.statics.get(ci.getName());

		//end GETSTATIC code from super
		if (ei == null){
		      throw new JPFException("attempt to access field: " + fname + " of uninitialized class: " + ci.getName());
		    }
		
		Object attr = ei.getFieldAttr(fi);

		if (!(fi.isReference() && attr != null && attr != Helper.SymbolicNull)){
			//System.out.println("First attr not null");
			return super.execute(ss,ks,ti);
		}

		if(attr instanceof StringExpression || attr instanceof SymbolicStringBuilder){
			//System.out.println("Second attr not null");
			return super.execute(ss,ks,ti); // Strings are handled specially
		}

		
		// else: lazy initialization

		int currentChoice;
		ChoiceGenerator<?> heapCG;

		ClassInfo typeClassInfo = fi.getTypeClassInfo(); // use this instead of fullType


		// first time around

		if (!ti.isFirstStepInsn()) {
			prevSymRefs = null;
			numSymRefs = 0;

			prevHeapCG = ss.getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);

			if (prevHeapCG != null) {
				// collect candidates for lazy initialization
				SymbolicInputHeap symInputHeap =
						((HeapChoiceGenerator)prevHeapCG).getCurrentSymInputHeap();

				prevSymRefs = symInputHeap.getNodesOfType(typeClassInfo);
				numSymRefs = prevSymRefs.length;

			}

			// TODO: fix
			//			if(!subtypes.equals("false")) {
			//				// get the number of subtypes that exist, and add the number in
			//				// the choice generator in addition to the ones that were there
			//				numNewRefs = TypeHierarchy.getNumOfElements(typeClassInfo.getName());
			//				heapCG = new HeapChoiceGenerator(numSymRefs+increment+numNewRefs); // +null,new
			//			} else {
			heapCG = new HeapChoiceGenerator(numSymRefs+2);  //+null,new
			//}
			ss.setNextChoiceGenerator(heapCG);
			return this;
		} else {  // this is what really returns results
			heapCG = ss.getChoiceGenerator();
			assert (heapCG instanceof HeapChoiceGenerator) : "expected HeapChoiceGenerator, got: " + heapCG;
			currentChoice = ((HeapChoiceGenerator)heapCG).getNextChoice();
		}


		PathCondition pcHeap; //this pc contains only the constraints on the heap
		SymbolicInputHeap symInputHeap;

		// pcHeap is updated with the pcHeap stored in the choice generator above
		// get the pcHeap from the previous choice generator of the same type

		// can not simply re-use prevHeapCG from above because it might have changed during re-execution
		// bug reported by Willem Visser

		prevHeapCG = heapCG.getPreviousChoiceGeneratorOfType(HeapChoiceGenerator.class);

		if (prevHeapCG == null){
			pcHeap = new PathCondition();
			symInputHeap = new SymbolicInputHeap();
		}
		else {
			pcHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentPCheap();
			symInputHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentSymInputHeap();
		}
		assert pcHeap != null;
		assert symInputHeap != null;

		prevSymRefs = symInputHeap.getNodesOfType(typeClassInfo);
		numSymRefs = prevSymRefs.length;


		//System.out.println("Checking attr: "+((SymbolicInteger) attr).getName());
		
		int daIndex = 0; //index into JPF's dynamic area
		if (currentChoice < numSymRefs) { // lazy initialization
			HeapNode candidateNode = prevSymRefs[currentChoice];
			// here we should update pcHeap with the constraint attr == candidateNode.sym_v
			pcHeap._addDet(Comparator.EQ, (SymbolicInteger) attr, candidateNode.getSymbolic());
			daIndex = candidateNode.getIndex();
		}
		else if (currentChoice == numSymRefs) { //existing (null)
			pcHeap._addDet(Comparator.EQ, (SymbolicInteger) attr, new IntegerConstant(-1));
			daIndex = -1;
		} else if (currentChoice == (numSymRefs + 1) && !abstractClass) {
			// creates a new object with all fields symbolic and adds the object to SymbolicHeap
			daIndex = Helper.addNewHeapNode(typeClassInfo, ti, daIndex, attr, ks, pcHeap,
					symInputHeap, numSymRefs, prevSymRefs);
		} else {
			//TODO: fix
			System.err.println("subtyping not handled");
			//			  int counter;
			//			  if(abstractClass) {
			//					counter = currentChoice - (numSymRefs+1) ; //index to the sub-class
			//			  } else {
			//					counter = currentChoice - (numSymRefs+1) - 1;
			//			  }
			//			  ClassInfo subClassInfo = TypeHierarchy.getClassInfo(typeClassInfo.getName(), counter);
			//			  daIndex = Helper.addNewHeapNode(subClassInfo, ti, daIndex, attr, ks, pcHeap,
			//					  		symInputHeap, numSymRefs, prevSymRefs);
		}


		ei.setReferenceField(fi,daIndex );
		ei.setFieldAttr(fi, Helper.SymbolicNull); // was null
		ti.push( ei.getReferenceField(fi), fi.isReference());
		((HeapChoiceGenerator)heapCG).setCurrentPCheap(pcHeap);
		((HeapChoiceGenerator)heapCG).setCurrentSymInputHeap(symInputHeap);
		if (SymbolicInstructionFactory.debugMode)
			System.out.println("GETSTATIC pcHeap: " + pcHeap);
		return getNext(ti);
	}


}

