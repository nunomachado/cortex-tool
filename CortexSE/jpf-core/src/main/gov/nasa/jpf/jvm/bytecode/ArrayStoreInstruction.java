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
 * abstraction for all array store instructions
 *
 *  ... array, index, <value> => ...
 */
public abstract class ArrayStoreInstruction extends ArrayInstruction implements StoreInstruction {

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int aref = peekArrayRef(ti); // need to be poly, could be LongArrayStore
    if (aref == -1) {
      return ti.createAndThrowException("java.lang.NullPointerException");
    }

    ElementInfo e = ti.getElementInfo(aref);

    if (isNewPorBoundary(e, ti)) {
      if (createAndSetArrayCG(ss,e,ti, aref, peekIndex(ti), false)) {
        return this;
      }
    }

    int esize = getElementSize();
    Object attr = esize == 1 ? ti.getOperandAttr() : ti.getLongOperandAttr();

    popValue(ti);
    index = ti.pop();
    // don't set 'arrayRef' before we do the CG check (would kill loop optimization)
    arrayRef = ti.pop();

    Instruction xInsn = checkArrayStoreException(ti, e);
    if (xInsn != null){
      return xInsn;
    }

    try {
      setField(e, index);
      e.setElementAttrNoClone(index,attr); // <2do> what if the value is the same but not the attr?
      return getNext(ti);

    } catch (ArrayIndexOutOfBoundsExecutiveException ex) { // at this point, the AIOBX is already processed
      return ex.getInstruction();
    }
  }

  /**
   * this is for pre-exec use
   */
  protected int peekArrayRef(ThreadInfo ti) {
    return ti.peek(2);
  }

  protected int peekIndex(ThreadInfo ti){
    return ti.peek(1);
  }

  protected Instruction checkArrayStoreException(ThreadInfo ti, ElementInfo ei){
    return null;
  }

  protected abstract void popValue(ThreadInfo ti);

  protected abstract void setField (ElementInfo e, int index)
                    throws ArrayIndexOutOfBoundsExecutiveException;


  public boolean isRead() {
    return false;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

}
