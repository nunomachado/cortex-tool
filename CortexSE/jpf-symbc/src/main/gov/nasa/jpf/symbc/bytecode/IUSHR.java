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

import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;

public class IUSHR extends gov.nasa.jpf.jvm.bytecode.IUSHR {

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
					result = sym_v1._shiftUR(sym_v2);
				else // v2 is concrete
					result = sym_v1._shiftUR(v2);
			}
			else if (sym_v2!=null)
				result = sym_v2._shiftUR(v1);
			sf.setOperandAttr(result);
				
			return getNext(th);
		}
	}
}