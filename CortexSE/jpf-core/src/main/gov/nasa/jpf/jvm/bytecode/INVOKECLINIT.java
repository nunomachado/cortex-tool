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

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;

/**
 * this is an artificial bytecode that we use to deal with the particularities of 
 * <clinit> calls, which are never in the loaded bytecode but always directly called by
 * the VM. The most obvious difference is that <clinit> execution does not trigger
 * class initialization.
 * A more subtle difference is that we save a wait() - if a class
 * is concurrently initialized, both execute INVOKECLINIT (i.e. compete and sync for/on
 * the class object lock), but once the second thread gets resumed and detects that the
 * class is now initialized (by the first thread), it skips the method execution and
 * returns right away (after deregistering as a lock contender). That's kind of hackish,
 * but we have no method to do the wait in, unless we significantly complicate the
 * direct call stubs, which would obfuscate observability (debugging dynamically
 * generated code isn't very appealing). 
 * 
 * <2do> pcm - maybe we should move this into the jpf.jvm package, it's artificial anyways 
 */
public class INVOKECLINIT extends INVOKESTATIC {

  public INVOKECLINIT (ClassInfo ci){
    super(ci.getSignature(), "<clinit>", "()V");
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    
    MethodInfo callee = getInvokedMethod(ti);
    ClassInfo ci = callee.getClassInfo();
    
    ElementInfo ei = ti.getElementInfo(ci.getClassObjectRef());

    // first time around - reexecute if the scheduling policy gives us a choice point
    if (!ti.isFirstStepInsn()) {
      
      if (!ei.canLock(ti)) {
        // block first, so that we don't get this thread in the list of CGs
        ei.block(ti);
      }
      
      ChoiceGenerator cg = ss.getSchedulerFactory().createSyncMethodEnterCG(ei, ti);
      if (ss.setNextChoiceGenerator(cg)){
        if (!ti.isBlocked()) {
          // record that this thread would lock the object upon next execution
          ei.registerLockContender(ti);
        }
        return this;   // repeat exec, keep insn on stack
      }
      
      assert !ti.isBlocked() : "scheduling policy did not return ChoiceGenerator for blocking INVOKE";
      
    } else {
      // if we got here, we can execute, and have the lock
      // but there still might have been another thread that passed us with the init
      // note that the state in this case would be INITIALIZED, otherwise we wouldn't
      // have gotten the lock
      if (!ci.needsInitialization()) {
        // we never got the lock it (that would have happened in MethodInfo.enter(), but
        // registerLockContender added it to the lockedThreads list of the monnitor,
        // and ti might be blocked on it (if we couldn't lock in the top half above)
        ei.unregisterLockContender(ti);
        return getNext();
      }
    }
    
    // enter the method body, return its first insn
    // (this would take the lock, reset the lockRef etc., so make sure all these
    // side effects are dealt with if we bail out)
    return callee.execute(ti);
  }

  public boolean isExtendedInstruction() {
    return true;
  }

  public static final int OPCODE = 256;

  public int getByteCode () {
    return OPCODE;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
