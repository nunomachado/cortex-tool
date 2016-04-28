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
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;

import java.util.Iterator;


/**
 * abstraction for the various return instructions
 */
public abstract class ReturnInstruction extends Instruction implements gov.nasa.jpf.jvm.ReturnInstruction {

  // to store where we came from
  protected StackFrame returnFrame;

  // note these are only callable from within the same execute - thread interleavings
  // would cause races
  abstract protected void storeReturnValue (ThreadInfo th);
  abstract protected void pushReturnValue (ThreadInfo th);

  public abstract Object getReturnValue(ThreadInfo ti);

  public StackFrame getReturnFrame() {
    return returnFrame;
  }

  public void setReturnFrame(StackFrame frame){
    returnFrame = frame;
  }

  /**
   * this is important since keeping the StackFrame alive would be a major
   * memory leak
   */
  @Override
  public void cleanupTransients(){
    returnFrame = null;
  }
  
  //--- attribute accessors
  
  // the accessors are here to save the client some effort regarding the
  // return type (slot size)
  
  public boolean hasReturnAttr (ThreadInfo ti){
    return ti.hasOperandAttr();
  }
  public boolean hasReturnAttr (ThreadInfo ti, Class<?> type){
    return ti.hasOperandAttr(type);
  }
  
  /**
   * this returns all of them - use either if you know there will be only
   * one attribute at a time, or check/process result with ObjectList
   * 
   * obviously, this only makes sense from an instructionExecuted(), since
   * the value is pushed during the execute(). Use ObjectList to access values
   */
  public Object getReturnAttr (ThreadInfo ti){
    return ti.getOperandAttr();
  }

  /**
   * this replaces all of them - use only if you know 
   *  - there will be only one attribute at a time
   *  - you obtained the value you set by a previous getXAttr()
   *  - you constructed a multi value list with ObjectList.createList()
   * 
   * we don't clone since pushing a return value already changed the caller frame
   */
  public void setReturnAttr (ThreadInfo ti, Object a){
    ti.setOperandAttrNoClone(a);
  }
  
  public void addReturnAttr (ThreadInfo ti, Object attr){
    ti.addOperandAttrNoClone(attr);
  }

  /**
   * this only returns the first attr of this type, there can be more
   * if you don't use client private types or the provided type is too general
   */
  public <T> T getReturnAttr (ThreadInfo ti, Class<T> type){
    return ti.getOperandAttr(type);
  }
  public <T> T getNextReturnAttr (ThreadInfo ti, Class<T> type, Object prev){
    return ti.getNextOperandAttr(type, prev);
  }
  public Iterator returnAttrIterator (ThreadInfo ti){
    return ti.operandAttrIterator();
  }
  public <T> Iterator<T> returnAttrIterator (ThreadInfo ti, Class<T> type){
    return ti.operandAttrIterator(type);
  }
  
  // -- end attribute accessors --
  
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {

    if (!ti.isFirstStepInsn()) {
      mi.leave(ti);  // takes care of unlocking before potentially creating a CG

      if (mi.isSynchronized()) {
        int objref = mi.isStatic() ? mi.getClassInfo().getClassObjectRef() : ti.getThis();
        ElementInfo ei = ti.getElementInfo(objref);

        if (ei.getLockCount() == 0){
          if (ei.checkUpdatedSharedness(ti)) {
            ChoiceGenerator<ThreadInfo> cg = ss.getSchedulerFactory().createSyncMethodExitCG(ei, ti);
            if (cg != null) {
              if (ss.setNextChoiceGenerator(cg)) {
                ti.skipInstructionLogging();
                return this; // re-execute
              }
            }
          }
        }
      }
    }

    returnFrame = ti.getTopFrame();
    Object attr = getReturnAttr(ti); // do this before we pop
    storeReturnValue(ti);

    
    // note that this is never the first frame, since we start all threads (incl. main)
    // through a direct call
    StackFrame top = ti.popFrame();

    // remove args, push return value and continue with next insn
    // (DirectCallStackFrames don't use this)
    ti.removeArguments(mi);
    pushReturnValue(ti);

    if (attr != null) {
      setReturnAttr(ti, attr);
    }

    return top.getPC().getNext();
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
