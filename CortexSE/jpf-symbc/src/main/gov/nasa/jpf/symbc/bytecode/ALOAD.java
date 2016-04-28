package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;

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

public class ALOAD extends gov.nasa.jpf.jvm.bytecode.ALOAD {

	public ALOAD(int localVarIndex) {
	    super(localVarIndex);
	}

	
    //private int numNewRefs = 0; // # of new reference objects to account for polymorphism -- work of Neha Rungta -- needs to be updated
      boolean abstractClass = false;

	public Instruction execute (SystemState ss, KernelState ks, ThreadInfo th) {
		//System.out.println("ALOAD1");

		HeapNode[] prevSymRefs = null; // previously initialized objects of same type: candidates for lazy init
        int numSymRefs = 0; // # of prev. initialized objects
        ChoiceGenerator<?> prevHeapCG = null;

		Config conf = th.getVM().getConfig();
		String[] lazy = conf.getStringArray("symbolic.lazy");

		if (lazy == null || !lazy[0].equalsIgnoreCase("true"))
			return super.execute(ss,ks,th);

		// TODO: fix handle polymorphism
		
		//String subtypes = conf.getString("symbolic.lazy.subtypes", "false");
		//if(!subtypes.equals("false") &&
			//	TypeHierarchy.typeHierarchies == null) {
			//TypeHierarchy.buildTypeHierarchy(th);
		//}

		Object attr = th.getLocalAttr(index);
		String typeOfLocalVar = super.getLocalVariableType();

		
		if(attr == null || typeOfLocalVar.equals("?") || attr instanceof SymbolicStringBuilder || attr instanceof StringExpression) {
			return super.execute(ss,ks,th);
		}
		System.out.println("ALOAD2 - attr: "+attr);
		
		ClassInfo typeClassInfo = ClassInfo.getResolvedClassInfo(typeOfLocalVar);

		int currentChoice;
		ChoiceGenerator<?> thisHeapCG;
		
		if(!th.isFirstStepInsn()) {
			//System.out.println("the first time");

			prevSymRefs = null;
			numSymRefs = 0;
			prevHeapCG = null;

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
			
			// TODO fix: subtypes
//			if(!subtypes.equals("false")) {
//				// get the number of subtypes that exist, and add the number in
//				// the choice generator in addition to the ones that were there
//				numNewRefs = TypeHierarchy.getNumOfElements(typeClassInfo.getName());
//				thisHeapCG = new HeapChoiceGenerator(numSymRefs+increment+numNewRefs); // +null,new
//			} else {
				thisHeapCG = new HeapChoiceGenerator(numSymRefs+increment);  //+null,new
			//}
			ss.setNextChoiceGenerator(thisHeapCG);
			return this;
		} else { 
			//this is what returns the results
			thisHeapCG = ss.getChoiceGenerator();
			assert(thisHeapCG instanceof HeapChoiceGenerator) :
				"expected HeapChoiceGenerator, got:" + thisHeapCG;
			currentChoice = ((HeapChoiceGenerator) thisHeapCG).getNextChoice();
		}

		PathCondition pcHeap;
		SymbolicInputHeap symInputHeap;

		// pcHeap is updated with the pcHeap stored in the choice generator above
        // get the pcHeap from the previous choice generator of the same type
        // can not simply re-use prevHeapCG from above because it might have changed during re-execution
        // bug reported by Willem Visser
        prevHeapCG = thisHeapCG.getPreviousChoiceGeneratorOfType(HeapChoiceGenerator.class);

		
		if(prevHeapCG == null) {
			pcHeap = new PathCondition();
			symInputHeap = new SymbolicInputHeap();
		} else {
			pcHeap =  ((HeapChoiceGenerator) prevHeapCG).getCurrentPCheap();
			symInputHeap = ((HeapChoiceGenerator) prevHeapCG).getCurrentSymInputHeap();
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
			//creates a new object with all fields symbolic
			daIndex = Helper.addNewHeapNode(typeClassInfo, th, daIndex, attr, ks, pcHeap,
							symInputHeap, numSymRefs, prevSymRefs);
		} else {
			//TODO: fix subtypes
			System.err.println("subtypes not handled");
//			int counter;
//			if(abstractClass) {
//				counter = currentChoice - (numSymRefs+1) ; //index to the sub-class
//			} else {
//				counter = currentChoice - (numSymRefs+1) - 1;
//			}
//			ClassInfo subClassInfo = TypeHierarchy.getClassInfo(typeClassInfo.getName(), counter);
//			daIndex = Helper.addNewHeapNode(subClassInfo, th, daIndex, attr, ks, pcHeap,
//							symInputHeap, numSymRefs, prevSymRefs);

		}


		th.setLocalVariable(index, daIndex, true);
		th.setLocalAttr(index, null);
		th.push(daIndex, true);

		((HeapChoiceGenerator)thisHeapCG).setCurrentPCheap(pcHeap);
		((HeapChoiceGenerator)thisHeapCG).setCurrentSymInputHeap(symInputHeap);
		if (SymbolicInstructionFactory.debugMode)
			System.out.println("ALOAD pcHeap: " + pcHeap);
		return getNext(th);
	}

}