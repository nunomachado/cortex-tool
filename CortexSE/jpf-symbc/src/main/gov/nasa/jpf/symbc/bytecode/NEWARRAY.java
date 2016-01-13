package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.Heap;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.string.SymbolicLengthInteger;

/**
 * Symbolic version of the NEWARRAY class from jpf-core. Has some extra code to
 * detect if a symbolic variable is being used as the size of the new array, and
 * treat it accordingly.
 * 
 * Someone with more experience should review this :)
 * 
 */

public class NEWARRAY extends gov.nasa.jpf.jvm.bytecode.NEWARRAY {

	public NEWARRAY(int typeCode) {
    	super(typeCode);
    }
	
	@Override
	public Instruction execute(SystemState ss, KernelState ks, ThreadInfo ti) {
		Object attr = ti.getOperandAttr();
		
		if(attr instanceof SymbolicLengthInteger) {
			arrayLength = ((SymbolicLengthInteger) attr).solution;
			ti.pop();
		} else 	if(attr instanceof IntegerExpression) {
			if (!PathCondition.flagSolved) {
				// TODO I don't know what to do in this case; I believe this
				// only happens if the array initialization is
				// located before a program branch.
				throw new RuntimeException(
						"Path condition is not solved (expression = "+attr+"); Check the comments above this line for more details!");
			}
			arrayLength = ((IntegerExpression) attr).solution();
			ti.pop();
		} else {
			arrayLength = ti.pop();
		}

		//the remainder of the code is identical to the parent class
		
	    Heap heap = ti.getHeap();

	    if (arrayLength < 0){
	      return ti.createAndThrowException("java.lang.NegativeArraySizeException");
	    }

	    // there is no clinit for array classes, but we still have  to create a class object
	    // since its a builtin class, we also don't have to bother with NoClassDefFoundErrors
	    String clsName = "[" + type;
	    ClassInfo ci = ClassInfo.getResolvedClassInfo(clsName);

	    if (!ci.isRegistered()) {
	      ci.registerClass(ti);
	      ci.setInitialized();
	    }
	   
	    if (heap.isOutOfMemory()) { // simulate OutOfMemoryError
	      return ti.createAndThrowException("java.lang.OutOfMemoryError",
	                                        "trying to allocate new " +
	                                          getTypeName() +
	                                        "[" + arrayLength + "]");
	    }
	    
	    int arrayRef = heap.newArray(type, arrayLength, ti);
	    ti.push(arrayRef, true);

	    ss.checkGC(); // has to happen after we push the new object ref
	    
	    return getNext(ti);
	}
}