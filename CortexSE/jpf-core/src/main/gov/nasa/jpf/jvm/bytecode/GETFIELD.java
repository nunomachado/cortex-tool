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
 * Fetch field from object
 * ..., objectref => ..., value
 */
public class GETFIELD extends InstanceFieldInstruction {

  public GETFIELD (String fieldName, String classType, String fieldDescriptor){
    super(fieldName, classType, fieldDescriptor);
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int objRef = ti.peek(); // don't pop yet, we might re-execute
    lastThis = objRef;
    if (objRef == -1) {
      return ti.createAndThrowException("java.lang.NullPointerException",
                                        "referencing field '" + fname + "' on null object");
    }

    ElementInfo ei = ti.getElementInfo(objRef);

    FieldInfo fi = getFieldInfo();
    if (fi == null) {
      return ti.createAndThrowException("java.lang.NoSuchFieldError",
                                        "referencing field '" + fname + "' in " + ei);
    }

    // check if this breaks the current transition
    if (isNewPorFieldBoundary(ti, fi, objRef)) {
      if (createAndSetFieldCG(ss, ei, ti)) {
        return this;
      }
    }

    ti.pop(); // Ok, now we can remove the object ref from the stack
    Object attr = ei.getFieldAttr(fi);

    // We could encapsulate the push in ElementInfo, but not the GET, so we keep it at a similiar level
    if (fi.getStorageSize() == 1) { // 1 slotter
      int ival = ei.get1SlotField(fi);
      lastValue = ival;

      ti.push(ival, fi.isReference());
      if (attr != null) {
        ti.setOperandAttrNoClone(attr);
      }

    } else {  // 2 slotter
      long lval = ei.get2SlotField(fi);
      lastValue = lval;

      ti.longPush(lval);
      if (attr != null) {
        ti.setLongOperandAttrNoClone(attr);
      }
    }

    return getNext(ti);
  }

  public ElementInfo peekElementInfo (ThreadInfo ti) {
    int objRef = ti.peek();
    ElementInfo ei = ti.getElementInfo(objRef);
    return ei;
  }

  public int getLength() {
    return 3; // opcode, index1, index2
  }

  public int getByteCode () {
    return 0xB4;
  }

  public boolean isRead() {
    return true;
  }

  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
