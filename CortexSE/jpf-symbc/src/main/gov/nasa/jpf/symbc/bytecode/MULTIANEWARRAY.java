package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.string.SymbolicLengthInteger;

/**
 * Symbolic version of the MULTIANEWARRAY class from jpf-core. Like NEWARRAY,
 * the difference from the jpf-core version is a snippet to detect if a symbolic
 * variable is being used as the size of the new array, and treat it accordingly.
 * 
 * And someone should review this one too :)
 */

public class MULTIANEWARRAY extends gov.nasa.jpf.jvm.bytecode.MULTIANEWARRAY {

	public MULTIANEWARRAY(String typeName, int dimensions) {
		super(typeName, dimensions);
	}

	@Override
	public Instruction execute(SystemState ss, KernelState ks, ThreadInfo ti) {
		arrayLengths = new int[dimensions];

		for (int i = dimensions - 1; i >= 0; i--) {
			Object attr = ti.getOperandAttr();
			
			if(attr instanceof SymbolicLengthInteger) {
				arrayLengths[i] = ((SymbolicLengthInteger) attr).solution;
				ti.pop();
			} else 	if(attr instanceof IntegerExpression) {
				arrayLengths[i] = ((IntegerExpression) attr).solution();
				ti.pop();
			} else {
				arrayLengths[i] = ti.pop();
			}
		}

		//the remainder of the code is identical to the parent class
		
		// there is no clinit for array classes, but we still have  to create a class object
		// since its a builtin class, we also don't have to bother with NoClassDefFoundErrors
		ClassInfo ci = ClassInfo.getResolvedClassInfo(type);
		if (!ci.isRegistered()) {
			ci.registerClass(ti);
			ci.setInitialized();
		}
		    
		int arrayRef = allocateArray(ti.getHeap(), type, arrayLengths, ti, 0);

		// put the result (the array reference) on the stack
		ti.push(arrayRef, true);

		return getNext(ti);
	}
}
