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
import gov.nasa.jpf.jvm.Types;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.symbc.numeric.RealExpression;

/**
 * Negate double ..., value => ..., result
 */
public class DNEG extends gov.nasa.jpf.jvm.bytecode.DNEG {

	public Instruction execute(SystemState ss, KernelState ks, ThreadInfo th) {

		StackFrame sf = th.getTopFrame();
		RealExpression sym_v1 = (RealExpression) sf.getLongOperandAttr();
		double v1 = Types.longToDouble(th.longPop());

		if (sym_v1 == null)
			th.longPush(Types.doubleToLong(-v1));
		else {
			th.longPush(0);
			RealExpression result = sym_v1._neg();
			sf.setLongOperandAttr(result);
		}
		//System.out.println("Execute DNEG: " + sf.getLongOperandAttr());

		return getNext(th);
	}

}
