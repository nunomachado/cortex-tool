package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;

import gov.nasa.jpf.symbc.numeric.*;

public class IXOR extends gov.nasa.jpf.jvm.bytecode.IXOR {

	@Override
	public Instruction execute (SystemState ss, KernelState ks, ThreadInfo th) {
		StackFrame sf = th.getTopFrame();
		IntegerExpression sym_v1 = (IntegerExpression) sf.getOperandAttr(0); 
		IntegerExpression sym_v2 = (IntegerExpression) sf.getOperandAttr(1);
		
		if(sym_v1==null && sym_v2==null)
			return super.execute(ss, ks, th); // we'll still do the concrete execution
		else {
			int v1 = th.pop();
			int v2 = th.pop();
			th.push(0, false); // for symbolic expressions, the concrete value does not matter
		
			IntegerExpression result = null;
			if(sym_v1!=null) {
				if (sym_v2!=null)
					result = sym_v1._xor(sym_v2);
				else // v2 is concrete
					result = sym_v1._xor(v2);
			}
			else if (sym_v2!=null)
				result = sym_v2._xor(v1);
			sf.setOperandAttr(result);
		
			//System.out.println("Execute IADD: "+result);
		
			return getNext(th);
		}
	
	}

}
