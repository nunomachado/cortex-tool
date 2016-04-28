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
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.ThreadInfo;

/**
 * class to abstract instructions accessing static fields
 */
public abstract class StaticFieldInstruction extends FieldInstruction {

  ClassInfo ci;

  protected StaticFieldInstruction(){}

  protected StaticFieldInstruction(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }

  public ClassInfo getClassInfo () {
    if (ci == null) {
      ci = ClassInfo.getResolvedClassInfo(className);
    }
    return ci;
  }

  public FieldInfo getFieldInfo () {
    if (fi == null) {
      ClassInfo ci = getClassInfo();
      if (ci != null) {
        fi = ci.getStaticField(fname);
      }
    }
    return fi;
  }

  /**
   *  that's invariant, as opposed to InstanceFieldInstruction, so it's
   *  not really a peek
   */
  public ElementInfo peekElementInfo (ThreadInfo ti) {
    return getLastElementInfo();
  }

  public StaticElementInfo getLastElementInfo() {
    return getFieldInfo().getClassInfo().getStaticElementInfo();
  }

  // this can be different than ci - the field might be in one of its
  // superclasses
  public ClassInfo getLastClassInfo(){
    return getFieldInfo().getClassInfo();
  }

  public String getLastClassName() {
    return getLastClassInfo().getName();
  }

  protected boolean isNewPorFieldBoundary (ThreadInfo ti) {
    return !ti.isFirstStepInsn() && ti.usePorFieldBoundaries() && isSchedulingRelevant(ti);
  }

  protected boolean isSchedulingRelevant (ThreadInfo ti) {

    // this should filter out the bulk in most real apps (library code)
    if (fi.neverBreak()) {
      return false;
    }

    if (!ti.hasOtherRunnables()) {
      return false;
    }
    // from here on, we can regard this field as shared

    if (ti.usePorSyncDetection()) {
      FieldInfo fi = getFieldInfo();

      if (fi.breakShared()) {
        // this one is supposed to be always treated as transition boundary
        return true;
      }

      // NOTE - we only encounter this for references, other static finals
      // will be inlined by the compiler
      if (skipFinals && fi.isFinal()) {
        return false;
      }

      if (skipStaticFinals && fi.isFinal()) {
        return false;
      }

      if (mi.isClinit() && (fi.getClassInfo() == mi.getClassInfo())) {
        // clinits are all synchronized, so they don't count
        return false;
      }

      if (isMonitorEnterPrologue()) {
        return false;
      }

      ElementInfo ei = fi.getClassInfo().getStaticElementInfo();
      if (ei.isImmutable()){
        return false;
      }
      if (!ei.checkUpdatedSharedness(ti)){
        return false;
      }
      if (isLockProtected(ti, ei)) {
        return false;
      }
    }

    return true;
  }

  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}

