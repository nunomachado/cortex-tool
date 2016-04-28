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

public class GETFIELD extends gov.nasa.jpf.jvm.bytecode.GETFIELD {
	public GETFIELD(String fieldName, String clsName, String fieldDescriptor){
	    super(fieldName, clsName, fieldDescriptor);
	  }
	
  // private int numNewRefs = 0; // # of new reference objects to account for polymorphism -- work of Neha Rungta -- needs to be updated
	
	boolean abstractClass = false;


  @Override
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
	  HeapNode[] prevSymRefs = null; // previously initialized objects of same type: candidates for lazy init
	  int numSymRefs = 0; // # of prev. initialized objects
	  ChoiceGenerator<?> prevHeapCG = null;

	  Config conf = ti.getVM().getConfig();
	  String[] lazy = conf.getStringArray("symbolic.lazy");
	  if (lazy == null || !lazy[0].equalsIgnoreCase("true"))
		  return super.execute(ss,ks,ti);

//TODO: fix: polymorphism and subtypes
//	  String subtypes = conf.getString("symbolic.lazy.subtypes", "false");
//	  if(!subtypes.equals("false") &&
//			  TypeHierarchy.typeHierarchies == null) {
//		  TypeHierarchy.buildTypeHierarchy(ti);
//	  }

	  //original GETFIELD code from super
	 int objRef = ti.peek(); // don't pop yet, we might re-execute
	 lastThis = objRef;
	 if (objRef == -1) {
		 return ti.createAndThrowException("java.lang.NullPointerException",
	                        "referencing field '" + fname + "' on null object");
	 }
	 ElementInfo ei = ti.getElementInfo(objRef);
	 FieldInfo fi = getFieldInfo();
	 if (fi == null) {
	    return ti.createAndThrowException("java.lang.NoSuchFieldError",
	                              "referencing field '" + fname + "' in " + ei);
	 }
	 
	 // check if this breaks the current transition
	    if (isNewPorFieldBoundary(ti, fi, objRef)) {
	      if (createAndSetFieldCG(ss, ei, ti)) {
	        return this;
	      }
	    }
	    	    
	 //end GETFIELD code from super

	

	 Object attr = ei.getFieldAttr(fi); 
	  // check if the field is of ref type & it is symbolic (i.e. it has an attribute)
	  // if it is we need to do lazy initialization

	  if (!(fi.isReference() && attr != null)) {
		  return super.execute(ss,ks,ti);
	  }

	  if(attr instanceof StringExpression || attr instanceof SymbolicStringBuilder)
			return super.execute(ss,ks,ti); // Strings are handled specially

	  // else: lazy initialization

	  int currentChoice;
	  ChoiceGenerator<?> thisHeapCG;
	  
	  ClassInfo typeClassInfo = fi.getTypeClassInfo(); // use this instead of fullType

	  // first time around, get previous heapCG (if any) size so we know
	  // how big to make this heapCG
	  // we do not collect now  the candidates for lazy initialization
	  if (!ti.isFirstStepInsn()) {
		  prevSymRefs = null;
		  numSymRefs = 0;
		  
		  prevHeapCG = ss.getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);
		  

		  if (prevHeapCG != null) {
			// determine # of candidates for lazy initialization
			  SymbolicInputHeap symInputHeap =
				  ((HeapChoiceGenerator)prevHeapCG).getCurrentSymInputHeap();
			  prevSymRefs = symInputHeap.getNodesOfType(typeClassInfo);
			  numSymRefs = prevSymRefs.length;
		  }
		  int increment = 2;
		  if(typeClassInfo.isAbstract()) {
			  abstractClass = true;
			  increment = 1; // only null
		  }
		  //TODO: fix subtypes
//		  if(!subtypes.equals("false")) {
//			  // get the number of subtypes that exist, and add the number in
//			  // the choice generator inaddition to the ones that were there
//			  int numNewRefs = TypeHierarchy.getNumOfElements(typeClassInfo.getName());
//			  thisHeapCG = new HeapChoiceGenerator(numSymRefs+increment+numNewRefs); // +null, (no new)
//		  } else {
			  thisHeapCG = new HeapChoiceGenerator(numSymRefs+increment);  //+null,new
//		  }
		  ss.setNextChoiceGenerator(thisHeapCG);
		  return this;
	  }
	  else {//this is what really returns results
		 
		  //from original GETFIELD bytecode
		  ti.pop(); // Ok, now we can remove the object ref from the stack

		  thisHeapCG = ss.getChoiceGenerator();
		  assert (thisHeapCG instanceof HeapChoiceGenerator) :
			  "expected HeapChoiceGenerator, got: " + thisHeapCG;
		  currentChoice = ((HeapChoiceGenerator) thisHeapCG).getNextChoice();
	  }

	  PathCondition pcHeap; //this pc contains only the constraints on the heap
	  SymbolicInputHeap symInputHeap;

	  // depending on the currentChoice, we set the current field to an object that was already created
	  // 0 .. numymRefs -1, or to null or to a new object of the respective type, where we set all its
	  // fields to be symbolic


	  // pcHeap is updated with the pcHeap stored in the choice generator above
	  // get the pcHeap from the previous choice generator of the same type
	  // can not simply re-use prevHeapCG from above because it might have changed during re-execution
	  // bug reported by Willem Visser
	  prevHeapCG = thisHeapCG.getPreviousChoiceGeneratorOfType(HeapChoiceGenerator.class);
	  
	  
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
	  
	  int daIndex = 0; //index into JPF's dynamic area
	  if (currentChoice < numSymRefs) { // lazy initialization using a previously lazily initialized object
		  HeapNode candidateNode = prevSymRefs[currentChoice];
		  // here we should update pcHeap with the constraint attr == candidateNode.sym_v
		  pcHeap._addDet(Comparator.EQ, (SymbolicInteger) attr, candidateNode.getSymbolic());
          daIndex = candidateNode.getIndex();
	  }
	  else if (currentChoice == numSymRefs){ //null object
		  pcHeap._addDet(Comparator.EQ, (SymbolicInteger) attr, new IntegerConstant(-1));
		  daIndex = -1;
	  }
	  else if (currentChoice == (numSymRefs + 1) && !abstractClass) {
		  // creates a new object with all fields symbolic and adds the object to SymbolicHeap
		  daIndex = Helper.addNewHeapNode(typeClassInfo, ti, daIndex, attr, ks, pcHeap,
				  		symInputHeap, numSymRefs, prevSymRefs);
	  } else {
		  System.err.println("subtyping not handled");
		  //TODO: fix
//		  int counter;
//		  if(abstractClass) {
//				counter = currentChoice - (numSymRefs+1) ; //index to the sub-class
//		  } else {
//				counter = currentChoice - (numSymRefs+1) - 1;
//		  }
//		  ClassInfo subClassInfo = TypeHierarchy.getClassInfo(typeClassInfo.getName(), counter);
//		  daIndex = Helper.addNewHeapNode(subClassInfo, ti, daIndex, attr, ks, pcHeap,
//				  		symInputHeap, numSymRefs, prevSymRefs);
	  }

	  ei.setReferenceField(fi,daIndex );
	  ei.setFieldAttr(fi, null);

	  ti.push(ei.getReferenceField(fi), fi.isReference());
	  ((HeapChoiceGenerator)thisHeapCG).setCurrentPCheap(pcHeap);
	  ((HeapChoiceGenerator)thisHeapCG).setCurrentSymInputHeap(symInputHeap);
	  //if (SymbolicInstructionFactory.debugMode)
		//	System.out.println("GETFIELD pcHeap: " + pcHeap);
	  return getNext(ti);
  }


}
