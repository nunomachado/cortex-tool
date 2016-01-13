//
// Copyright (C) 2011 United States Government as represented by the
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
package gov.nasa.jpf.listener;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.LCMP;
import gov.nasa.jpf.jvm.bytecode.LSUB;
import gov.nasa.jpf.jvm.bytecode.NATIVERETURN;
import gov.nasa.jpf.jvm.choice.IntChoiceFromSet;


/**
 * a listener that is used to explore all paths from a time-value comparison.
 * 
 * This works by creating a CG on LCMP instructions that involve 
 * System.currentTimeMillis() obtained values, which are attribute tagged
 * upon return from the native method, propagated on LSUB (duration computation),
 * and finally used for LCMP interception if the tag attributes are present
 * 
 *   long t1 = System.currentTimeMillis();
 *   doSomeComputation();
 *   long t2 = System.currentTimeMillis();
 *   if (t2 - t1 <= MAX_DURATION){
 *     // all fine branch
 *   } else {
 *     // catastrophic failure branch
 *   }
 * 
 * which boils down to a bytecode pattern like
 * 
 *   invokestatic #2; // System.currentTimeMillis()   <<< tag result with TimeVal attr
 *   ..
 *   lsub  <<< propagate if any of the operands has a TimeVal attr
 *   ..
 *   lcmp <<< register CG and skip if any of the operands has TimeVal attr
 * 
 */
public class StopWatchFuzzer extends ListenerAdapter {
  
  MethodInfo miCurrentTimeMillis;
  
  static class TimeVal {
    // attribute for values obtained from System.currentTimeMillis()
  }
  static TimeVal timeValAttr = new TimeVal(); // we don't need separate instances
  
  static String CG_ID = "LCMP_fuzzer";
  
  public void classLoaded( JVM jvm){
    if (miCurrentTimeMillis == null){
      ClassInfo ci = jvm.getLastClassInfo();
      if (ci.getName().equals("java.lang.System")) {
        miCurrentTimeMillis = ci.getMethod("currentTimeMillis()J", false); // its got to be there
      }
    }
  }
  
  public void instructionExecuted( JVM jvm){
    Instruction insn = jvm.getLastInstruction();
    
    if (insn instanceof NATIVERETURN){
      ThreadInfo ti = jvm.getLastThreadInfo();
      if (insn.isCompleted(ti)){
        if (((NATIVERETURN)insn).getMethodInfo() == miCurrentTimeMillis){
          // the two top operand slots hold the 'long' time value
          ti.addLongOperandAttr( timeValAttr);
        }
      }
    }
  }
  
  public void executeInstruction( JVM jvm){
    Instruction insn = jvm.getLastInstruction();

    if (insn instanceof LSUB){  // propagate TimeVal attrs
      ThreadInfo ti = jvm.getLastThreadInfo();
      
      // check if any of the operands have TimeVal attributes
      // attributes are stored on the first slot of a long val
      if (ti.hasOperandAttr(1, TimeVal.class) || ti.hasOperandAttr(3, TimeVal.class)){      
        // execute insn (this pops the 4 top operand slots and pushes the long result
        ti.skipInstruction(insn.execute(jvm.getSystemState(), jvm.getKernelState(), ti));
      
        // propagate TimeVal attr
        ti.addLongOperandAttr(timeValAttr);
      }
       
    } else if (insn instanceof LCMP){ // create and set CG if operand has TimeVal attr
      ThreadInfo ti = jvm.getLastThreadInfo();
      
      if (!ti.isFirstStepInsn()){ // this is the first time we see this insn
        if (ti.hasOperandAttr(1, TimeVal.class) || ti.hasOperandAttr(3, TimeVal.class)){
          IntChoiceFromSet cg = new IntChoiceFromSet( CG_ID, -1, 0, 1);
          if (jvm.setNextChoiceGenerator(cg)){
            ti.skipInstruction(insn); // reexecute after breaking the transition
          }
        }
        
      } else { // it is the beginning of a transition, push the choice and proceed
        IntChoiceFromSet cg = jvm.getCurrentChoiceGenerator(CG_ID, IntChoiceFromSet.class);
        if (cg != null){
          int choice = cg.getNextChoice();
          // pop the operands 
          ti.longPop(); // be aware of this is the first change of the top frame in this transition
          ti.longPop();
          
          // push the choice
          ti.push(choice);
          
          // skip the insn
          ti.skipInstruction(insn.getNext());
        }
      }
    }
  }
}
