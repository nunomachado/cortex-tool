//Copyright (C) 2007 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.

//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.

//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.

package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;

// we should factor out some of the code and put it in a parent class for all "if statements"

public class IFNE extends gov.nasa.jpf.jvm.bytecode.IFNE {
	public IFNE(int targetPosition){
	    super(targetPosition);
	  }
	@Override
	public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {

		StackFrame sf = ti.getTopFrame();
		IntegerExpression sym_v = (IntegerExpression) sf.getOperandAttr();

		if(sym_v == null) { // the condition is concrete
			//System.out.println("Execute IFNE: The condition is concrete");
			return super.execute(ss, ks, ti);
		}
		else { // the condition is symbolic

			String[] dp = SymbolicInstructionFactory.dp;

			ChoiceGenerator<?> cg;

			if (!ti.isFirstStepInsn()) { // first time around

				if (dp[0].equalsIgnoreCase("omega")) // hack because omega does not handle not or or correctly
					cg = new PCChoiceGenerator(3);
				else
					cg = new PCChoiceGenerator(2);
				((PCChoiceGenerator)cg).setOffset(this.position);
				((PCChoiceGenerator)cg).setMethodName(this.getMethodInfo().getCompleteName());
				ss.setNextChoiceGenerator(cg);
				return this;
			} else {  // this is what really returns results
				cg = ss.getChoiceGenerator();
				assert (cg instanceof PCChoiceGenerator) : "expected PCChoiceGenerator, got: " + cg;
				conditionValue = (Integer)cg.getNextChoice()==0 ? false: true;
			}

			ti.pop();
			//System.out.println("Execute IFNE: "+ conditionValue);
			PathCondition pc;

			// pc is updated with the pc stored in the choice generator above
			// get the path condition from the
			// previous choice generator of the same type

			ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
			while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
				prev_cg = prev_cg.getPreviousChoiceGenerator();
			}

			if (prev_cg == null)
				pc = new PathCondition();
			else
				pc = ((PCChoiceGenerator)prev_cg).getCurrentPC();

			assert pc != null;

			if (conditionValue) {
				if (dp[0].equalsIgnoreCase("omega")) {// hack
					if((Integer)cg.getNextChoice()==1)
						pc._addDet(Comparator.GT, sym_v, 0);
					else {// 2
						assert((Integer)cg.getNextChoice()==2);
						pc._addDet(Comparator.LT, sym_v, 0);
					}
				}
				else
					pc._addDet(Comparator.NE, sym_v, 0);
				if(!pc.simplify())  {// not satisfiable
					ss.setIgnored(true);
				}else{
					((PCChoiceGenerator) cg).setCurrentPC(pc);
					//System.out.println(((PCChoiceGenerator) cg).getCurrentPC());
				}
				return getTarget();
			} else {
				pc._addDet(Comparator.EQ, sym_v, 0);
				if(!pc.simplify())  {// not satisfiable
					ss.setIgnored(true);
				}else{
					((PCChoiceGenerator) cg).setCurrentPC(pc);
					//System.out.println(((PCChoiceGenerator) cg).getCurrentPC());
				}
				return getNext(ti);
			}
		}
	}
}
