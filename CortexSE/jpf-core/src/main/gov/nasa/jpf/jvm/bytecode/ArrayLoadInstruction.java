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

import gov.nasa.jpf.jvm.ArrayIndexOutOfBoundsExecutiveException;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * abstraction for all array load instructions
 *
 * ..., array, index => ..., value
 */
public abstract class ArrayLoadInstruction extends ArrayInstruction {
  
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {

    // we need to get the object first, to check if it is shared
    int aref = ti.peek(1); // ..,arrayRef,idx
    if (aref == -1) {
      return ti.createAndThrowException("java.lang.NullPointerException");
    }
    ElementInfo e = ti.getElementInfo(aref);

    if (isNewPorBoundary(e, ti)) {
      if (createAndSetArrayCG(ss,e,ti, aref,peekIndex(ti),true)) {
        return this;
      }
    }
    
    index = ti.pop();

    // we should not set 'arrayRef' before the CG check
    // (this would kill the CG loop optimization)
    arrayRef = ti.pop();
    
    try {
      push(ti, e, index);

      Object attr = e.getElementAttr(index);
      if (getElementSize() == 1){
        ti.setOperandAttrNoClone(attr);
      } else {
        ti.setLongOperandAttrNoClone(attr);
      }
      
      return getNext(ti);
    } catch (ArrayIndexOutOfBoundsExecutiveException ex) {
      return ex.getInstruction();
    }
  }

  protected boolean isReference () {
    return false;
  }

  /**
   * only makes sense pre-exec
   */
  protected int peekArrayRef (ThreadInfo ti){
    return ti.peek(1);
  }

  // wouldn't really be required for loads, but this is a general
  // ArrayInstruction API
  protected int peekIndex (ThreadInfo ti){
    return ti.peek();
  }

  protected abstract void push (ThreadInfo th, ElementInfo e, int index)
                throws ArrayIndexOutOfBoundsExecutiveException;

  public boolean isRead() {
    return true;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
 }
