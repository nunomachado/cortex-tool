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
 * Remainder float
 * ..., value1, value2 => ..., result
 */
public class FREM extends gov.nasa.jpf.jvm.bytecode.FREM  {

  @Override
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo th) {
   
    StackFrame sf = th.getTopFrame();

	RealExpression sym_v1 = (RealExpression) sf.getOperandAttr(); 
	float v1 = Types.intToFloat(th.pop());
		
	RealExpression sym_v2 = (RealExpression) sf.getOperandAttr();
	float v2 = Types.intToFloat(th.pop());
	    
    if(sym_v1==null && sym_v2==null){
        if (v1 == 0){
            return th.createAndThrowException("java.lang.ArithmeticException","division by zero");
        } 
        th.push(Types.floatToInt(v2 % v1), false);
    }else {
    	th.push(0, false);
    	throw new RuntimeException("## Error: SYMBOLIC FREM not supported");
    }
	
    return getNext(th);
  }

}
