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
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;


/**
 * Check whether object is of given type
 * ..., objectref => ..., objectref
 */
public class CHECKCAST extends Instruction {
  String type;

  public CHECKCAST() {} // this is going away

  public CHECKCAST(String typeName){
    type = Types.getClassNameFromTypeName(typeName);
  }

  public String getTypeName() {
    return type;
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    int objref = ti.peek();

    if (objref == MJIEnv.NULL) {
       // we can cast 'null' to anything

    } else {
      boolean isValid = false;

      ElementInfo e = ti.getElementInfo(objref);
      ClassInfo eci = e.getClassInfo();

      if (type.charAt(0) == '['){  // cast between array types
        if (eci.isArray()) {
          // check if the element types are compatible
          ClassInfo cci = eci.getComponentClassInfo();
          isValid = cci.isInstanceOf(type.substring(1));
        }

      } else { // non-array types
        isValid = e.getClassInfo().isInstanceOf(type);
      }

      if (!isValid) {
        return ti.createAndThrowException("java.lang.ClassCastException",
                e.getClassInfo().getName() + " cannot be cast to " + type);
      }
    }

    return getNext(ti);
  }


  public int getLength() {
    return 3; // opcode, index1, index2
  }
  
  public int getByteCode () {
    return 0xC0;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
