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
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Get static fieldInfo from class
 * ..., => ..., value 
 */
public class GETSTATIC extends StaticFieldInstruction {

  public GETSTATIC(String fieldName, String clsDescriptor, String fieldDescriptor){
    super(fieldName, clsDescriptor, fieldDescriptor);
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {

    ClassInfo clsInfo = getClassInfo();
    if (clsInfo == null){
      return ti.createAndThrowException("java.lang.NoClassDefFoundError", className);
    }

    FieldInfo fieldInfo = getFieldInfo();
    if (fieldInfo == null) {
      return ti.createAndThrowException("java.lang.NoSuchFieldError",
          (className + '.' + fname));
    }

    // this can be actually different (can be a base)
    clsInfo = fieldInfo.getClassInfo();

    if (!mi.isClinit(clsInfo) && requiresClinitExecution(ti, clsInfo)) {
      return ti.getPC();
    }

    StaticElementInfo ei = clsInfo.getStaticElementInfo();

    if (ei == null){
      throw new JPFException("attempt to access field: " + fname + " of uninitialized class: " + clsInfo.getName());
    }

    if (isNewPorFieldBoundary(ti)) {
      if (createAndSetFieldCG(ss, ei, ti)) {
        return this;
      }
    }
   
    Object attr = ei.getFieldAttr(fieldInfo);
	// System.out.println("Checking attr");

    
    if (size == 1) {
      int ival = ei.get1SlotField(fieldInfo);
      lastValue = ival;
      
  	//System.out.println("IVAL: "+ival);


      ti.push(ival, fieldInfo.isReference());
      
      if (attr != null) {
    	   //System.out.println("Attrib 1");
        ti.setOperandAttrNoClone(attr);
      }

    } else {
    	//System.out.println("size!=1");
      long lval = ei.get2SlotField(fieldInfo);
      lastValue = lval;
      
      ti.longPush(lval);
      
      if (attr != null) {
        ti.setLongOperandAttrNoClone(attr);
      }
    }
        
    return getNext(ti);
  }
  
  public int getLength() {
    return 3; // opcode, index1, index2
  }
  
  public int getByteCode () {
    return 0xB2;
  }

  public boolean isRead() {
    return true;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
