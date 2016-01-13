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

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.ThreadInfo;

/**
 * class abstracting instructions that access instance fields
 */
public abstract class InstanceFieldInstruction extends FieldInstruction
{
  /**
   * this is required for package external derived GET/PUTFIELDS that
   * replace execute().
   *
   * USE WITH CARE, AND ONLY FROM DERIVED CLASSES
   */
  protected int lastThis = -1;

  protected InstanceFieldInstruction() {}

  protected InstanceFieldInstruction (String fieldName, String classType, String fieldDescriptor){
    super(fieldName, classType, fieldDescriptor);
  }

  public FieldInfo getFieldInfo () {
    if (fi == null) {
      ClassInfo ci = ClassInfo.getResolvedClassInfo(className);
      if (ci != null) {
        fi = ci.getInstanceField(fname);
      }
    }
    return fi;
  }

  protected boolean isSchedulingRelevant (ThreadInfo ti, int objRef) {

    // this should filter out the bulk in most real apps (library code)
    if (fi.neverBreak()) {
      return false;
    }

    ElementInfo ei = ti.getElementInfo(objRef);

    // no use to break if there is no other thread, or the object is not shared
    // (but note this might change in a following execution path)
    if (!ei.checkUpdatedSharedness(ti)){
      return false;
    }
    if (ei.isImmutable()){
      return false;
    }

    if (!ti.hasOtherRunnables()) {
      return false;
    }

    //--- from here on, we know this is a shared object that can be accessed concurrently

    if (ti.usePorSyncDetection()) {

      if (fi.breakShared()) {
        // this one is supposed to be always treated as transition boundary
        return true;
      }

      // cutting off finals might loose interesting defects where the
      // reference escapes from a ctor that has a context switch before
      // the field init. 'final' only means "can only be assigned once",
      // it doesn't mean no read can happen before this assignment
      if (skipFinals && fi.isFinal()) {
        return false;
      }

      if (skipConstructedFinals && fi.isFinal() && ei.isConstructed()) {
        return false;
      }

      if (fname.startsWith("this$")) {
        // that one is an automatically created outer object reference in an inner class,
        // it can't be set. Unfortunately, we don't have an 'immutable' attribute for
        // fields, just objects, so we can't push it into class load time attributing.
        // Must be filtered out before we call 'isLockProtected'
        return false;
      }

      if (isMonitorEnterPrologue()) {
        // a little optimization for getfields that are only used to
        // push lock objects on the stack for a subsequent monitorenter
        return false;
      }

      if (!mi.isSyncRelevant()) {
        // filter out ctors (which in all likeliness will execute before
        // an object becomes shared) and <clinit>, which is synchronized
        // by the VM
        return false;
      }

      // this is a potentially more expensive test to identify fields
      // that are protected by locks, for which get/putXX should not break.
      // NOTE - here we get heuristic, and it is possible that we use
      // assumptions that might later-on be violated (but not detected)
      if (isLockProtected(ti, ei)) {
        return false;
      }
    }

    return true;
  }

  protected boolean isNewPorFieldBoundary (ThreadInfo ti, FieldInfo fi, int objRef) {
    return (!ti.isFirstStepInsn()) && ti.usePorFieldBoundaries() && isSchedulingRelevant(ti, objRef);
  }

  /**
   * NOTE - the return value is *only* valid in a instructionExecuted() context, since
   * the same instruction can be executed from different threads
   */
  public int getLastThis() {
    return lastThis;
  }

  /**
   * since this is based on getLastThis(), the same context restrictions apply
   */
  public ElementInfo getLastElementInfo () {
    if (lastThis != -1) {
      return JVM.getVM().getHeap().get(lastThis); // <2do> remove - should be in clients
    }

    return null;
  }

  /**
   * this one can be used from a choiceGeneratorSet() or executeInstruction() context, since
   * it peeks 'this' from the operand stack (execute didn't pop the value yet)
   * it' less efficient than getLastElementInfo() from a instructionExecuted context
   */
  public abstract ElementInfo peekElementInfo(ThreadInfo ti);

  public String getFieldDescriptor () {
    ElementInfo ei = getLastElementInfo();
    FieldInfo fi = getFieldInfo();

    return ei.toString() + '.' + fi.getName();
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}

