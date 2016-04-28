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

import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Set field in object
 * ..., objectref, value => ...
 *
 * Hmm, this is at the upper level of complexity because of the unified CG handling
 */
public class PUTFIELD extends InstanceFieldInstruction implements StoreInstruction {

  public PUTFIELD() {}

  public PUTFIELD(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }

  /**
   * only meaningful in instructionExecuted notification
   */

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {

    FieldInfo fi = getFieldInfo();
    if (fi == null) {
      // Hmm, we should do the NPE check first, but need the fi to get the object ref
      return ti.createAndThrowException("java.lang.NoSuchFieldError", fname);
    }

    int storageSize = fi.getStorageSize();
    int objRef = ti.peek( (storageSize == 1) ? 1 : 2);
    lastThis = objRef;

    // if this produces an NPE, force the error w/o further ado
    if (objRef == -1) {
      return ti.createAndThrowException("java.lang.NullPointerException",
                                 "referencing field '" + fname + "' on null object");
    }
    ElementInfo ei = ti.getElementInfo(objRef);

    // check if this breaks the current transition
    if (isNewPorFieldBoundary(ti, fi, objRef)) {
      if (createAndSetFieldCG(ss, ei, ti)) {
        return this;
      }
    }

    // start the real execution by getting the value from the operand stack
    Object attr = null; // attr handling has to be consistent with PUTSTATIC

    if (storageSize == 1){
      attr = ti.getOperandAttr();

      int ival = ti.pop();
      lastValue = ival;

      if (fi.isReference()) {
        ei.setReferenceField(fi, ival);
      } else {
        ei.set1SlotField(fi, ival);
      }

    } else {
        attr = ti.getLongOperandAttr();

        long lval = ti.longPop();
        lastValue = lval;

        ei.set2SlotField(fi, lval);
    }

    // this is kind of policy, but it seems more natural to overwrite
    // (if we want to accumulate, this has to happen in ElementInfo/Fields
    ei.setFieldAttrNoClone(fi, attr);  // <2do> what if the value is the same but not the attr?

    ti.pop(); // we already have the objRef
    lastThis = objRef;

    return getNext(ti);
  }

  public ElementInfo peekElementInfo (ThreadInfo ti) {
    FieldInfo fi = getFieldInfo();
    int storageSize = fi.getStorageSize();
    int objRef = ti.peek( (storageSize == 1) ? 1 : 2);
    ElementInfo ei = ti.getElementInfo( objRef);

    return ei;
  }


  public int getLength() {
    return 3; // opcode, index1, index2
  }

  public int getByteCode () {
    return 0xB5;
  }

  public boolean isRead() {
    return false;
  }

  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}



