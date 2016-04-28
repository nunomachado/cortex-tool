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
package gov.nasa.jpf.jvm;

/**
 * DirectCallStackFrames are only used for overlay calls (from native code), i.e.
 * there is no corresponding INVOKE instruction. The associated MethodInfos are
 * synthetic, their only code is (usually) a INVOKEx and a DIRECTCALLRETURN.
 * NOTE: such MethodInfos do not belong to any class
 * 
 * Arguments have to be explicitly pushed by the caller
 * 
 * They do not return any values themselves, but they do get the return values of the
 * called methods pushed onto their own operand stack. If the DirectCallStackFrame user
 * needs such return values, it has to do so via ThreadInfo.getReturnedDirectCall()
 *
 */
public class DirectCallStackFrame extends StackFrame {
  
  public DirectCallStackFrame (MethodInfo stub) {
    super(stub, null);
  }

  public DirectCallStackFrame (MethodInfo stub, int nOperandSlots, int nLocalSlots) {
    super(stub, nLocalSlots, nOperandSlots);
  }
  
  public boolean isDirectCallFrame() {
    return true;
  }

  @Override
  public boolean isSynthetic() {
    return true;
  }

  public String getClassName() {
    return "<direct call>";
  }
  
  public String getSourceFile () {
    return "<direct call>"; // we don't have any
  }
  
  
  // <2do> and a couple more we still have to do
}
