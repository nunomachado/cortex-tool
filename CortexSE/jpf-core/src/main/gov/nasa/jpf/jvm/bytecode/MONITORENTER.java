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
package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Enter monitor for object
 * ..., objectref => ...
 */
public class MONITORENTER extends LockInstruction {


  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int objref = ti.peek();      // Don't pop yet before we know we really execute

    if (objref == -1){
      return ti.createAndThrowException("java.lang.NullPointerException", "Attempt to acquire lock for null object");
    }

    lastLockRef = objref;
    ElementInfo ei = ks.heap.get(objref);

    if (!ti.isFirstStepInsn()){ // check if we have a choicepoint
      if (!isLockOwner(ti, ei)){  // maybe its a recursive lock

        if (ei.canLock(ti)) { // we can lock the object, the CG is optional
          if (ei.checkUpdatedSharedness(ti)) { // is this a shared object?
            ChoiceGenerator<?> cg = ss.getSchedulerFactory().createMonitorEnterCG(ei, ti);
            if (cg != null) {
              if (ss.setNextChoiceGenerator(cg)) {
                ei.registerLockContender(ti);  // Record that this thread would lock the object upon next execution
                return this;
              }
            }
          }

        } else { // already locked by another thread, we have to block and therefore need a CG
          ei.updateRefTidWith(ti.getId()); // Ok, now we know its shared

          ei.block(ti); // do this before we obtain the CG so that this thread is not in its choice set

          ChoiceGenerator<?> cg = ss.getSchedulerFactory().createMonitorEnterCG(ei, ti);
          if (cg != null) {
            if (ss.setNextChoiceGenerator(cg)) {
              return this;
            } else {
              throw new JPFException("listener did override ChoiceGenerator for blocking MONITOR_ENTER");
            }
          } else {
            throw new JPFException("scheduling policy did not return ChoiceGenerator for blocking MONITOR_ENTER");
          }
        }
      }
    }

    // this is only executed in the bottom half
    ti.pop();
    ei.lock(ti);  // Still have to increment the lockCount
    
    return getNext(ti);
  }  

  public int getByteCode () {
    return 0xC2;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
