//
//Copyright (C) 2007 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.
//
//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.
//
//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.symbc.bytecode;


import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;

/**
 * common root class for LOOKUPSWITCH and TABLESWITCH insns
 */

public abstract class SwitchInstruction extends gov.nasa.jpf.jvm.bytecode.SwitchInstruction {
	protected SwitchInstruction(int defaultTarget, int numberOfTargets) {
		super(defaultTarget, numberOfTargets);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {

		//return super.execute(ss,ks,ti);
		StackFrame sf = ti.getTopFrame();
		IntegerExpression sym_v = (IntegerExpression) sf.getOperandAttr();

		if(sym_v == null) { // the condition is concrete
			//System.out.println("Execute Switch: The condition is concrete");
			return super.execute(ss, ks, ti);
		}
		else { // the condition is symbolic
			//System.out.println("Execute Switch: The condition is symbolic");
			ChoiceGenerator<?> cg;

			if (!ti.isFirstStepInsn()) { // first time around
				cg = new PCChoiceGenerator(matches.length+1);
				((PCChoiceGenerator)cg).setOffset(this.position);
				((PCChoiceGenerator)cg).setMethodName(this.getMethodInfo().getCompleteName());
				ss.setNextChoiceGenerator(cg);
				return this;
			} else {  // this is what really returns results
				cg = ss.getChoiceGenerator();
				assert (cg instanceof PCChoiceGenerator) : "expected PCChoiceGenerator, got: " + cg;
			}
			sym_v = (IntegerExpression) sf.getOperandAttr();
			//System.out.println("Execute Switch: The condition is symbolic"+sym_v);
			ti.pop();
			PathCondition pc;
			//pc is updated with the pc stored in the choice generator above
			//get the path condition from the
			//previous choice generator of the same type

			//TODO: could be optimized to not do this for each choice
			ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
			while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
				prev_cg = prev_cg.getPreviousChoiceGenerator();
			}

			if (prev_cg == null)
				pc = new PathCondition();
			else
				pc = ((PCChoiceGenerator)prev_cg).getCurrentPC();

			assert pc != null;
			//System.out.println("Execute Switch: PC"+pc);
			int idx = (Integer)cg.getNextChoice();
			//System.out.println("Execute Switch: "+ idx);
			if (idx == matches.length){ // default branch
				lastIdx = DEFAULT;
				for(int i = 0; i< matches.length; i++)
					pc._addDet(Comparator.NE, sym_v, matches[i]);
				if(!pc.simplify())  {// not satisfiable
					ss.setIgnored(true);
				} else {
					//pc.solve();
					((PCChoiceGenerator) cg).setCurrentPC(pc);
					//System.out.println(((PCChoiceGenerator) cg).getCurrentPC());
				}
				return ti.getMethod().getInstructionAt(target);
			} else {
				lastIdx = idx;
				pc._addDet(Comparator.EQ, sym_v, matches[idx]);
				if(!pc.simplify())  {// not satisfiable
					ss.setIgnored(true);
				} else {
					//pc.solve();
					((PCChoiceGenerator) cg).setCurrentPC(pc);
					//System.out.println(((PCChoiceGenerator) cg).getCurrentPC());
				}
				return ti.getMethod().getInstructionAt(targets[idx]);
			}
		}
	}


}

