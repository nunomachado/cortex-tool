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

import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Branch always
 * No change
 *
 * <2do> store this as code insnIndex, not as bytecode position
 */
public class GOTO extends Instruction {
  protected int targetPosition;
  Instruction target;

  public GOTO (int targetPosition){
    this.targetPosition = targetPosition;
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo th) {
    return getTarget();
  }

  public boolean isBackJump () {
    return (targetPosition <= position);
  }
  
  public Instruction getTarget() {
    if (target == null) {
      target = mi.getInstructionAt(targetPosition);
    }
    return target;
  }

  public int getLength() {
    return 3; // opcode, bb1, bb2
  }
  
  public int getByteCode () {
    return 0xA7;
  }
  
  public String toString () {
    return getMnemonic() + " " + targetPosition;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
