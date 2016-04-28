//
// Copyright (C) 2010 United States Government as represented by the
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
import gov.nasa.jpf.jvm.NativeStackFrame;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;

/**
 * synthetic return instruction for native method invocations, so that
 * we don't have to do special provisions to copy the caller args in case
 * a post exec listener wants them.
 */
public class NATIVERETURN extends ReturnInstruction {

  Object ret;
  Object retAttr;
  Byte retType;

  // this is more simple than a normal ReturnInstruction because NativeMethodInfos
  // are not synchronized, and NativeStackFrames are never the first frame in a thread
  @Override
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    if (!ti.isFirstStepInsn()) {
      mi.leave(ti);  // takes care of unlocking before potentially creating a CG
      // NativeMethodInfo is never synchronized, so no thread CG here
    }

    storeReturnValue(ti);

    // NativeStackFrame can never can be the first stack frame, so no thread CG

    StackFrame top = ti.popFrame();

    // remove args, push return value and continue with next insn
    ti.removeArguments(mi);
    pushReturnValue(ti);

    if (retAttr != null) {
      setReturnAttr(ti, retAttr);
    }

    return top.getPC().getNext();
  }

  @Override
  public void cleanupTransients(){
    ret = null;
    retAttr = null;
    returnFrame = null;
  }
  
  @Override
  public boolean isExtendedInstruction() {
    return true;
  }

  public static final int OPCODE = 260;

  @Override
  public int getByteCode () {
    return OPCODE;
  }

  @Override
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

  @Override
  protected void storeReturnValue(ThreadInfo ti) {
    NativeStackFrame nativeFrame = (NativeStackFrame)ti.getTopFrame();

    returnFrame = nativeFrame;

    ret = nativeFrame.getReturnValue();
    retAttr = nativeFrame.getReturnAttr();
    retType = nativeFrame.getMethodInfo().getReturnTypeCode();
  }

  @Override
  protected void pushReturnValue (ThreadInfo ti) {
    int  ival;
    long lval;
    int  retSize = 1;

    // in case of a return type mismatch, we get a ClassCastException, which
    // is handled in executeMethod() and reported as a InvocationTargetException
    // (not completely accurate, but we rather go with safety)
    if (ret != null) {
      switch (retType) {
      case Types.T_BOOLEAN:
        ival = Types.booleanToInt(((Boolean) ret).booleanValue());
        ti.push(ival, false);
        break;

      case Types.T_BYTE:
        ti.push(((Byte) ret).byteValue(), false);
        break;

      case Types.T_CHAR:
        ti.push(((Character) ret).charValue(), false);
        break;

      case Types.T_SHORT:
        ti.push(((Short) ret).shortValue(), false);
        break;

      case Types.T_INT:
        ti.push(((Integer) ret).intValue(), false);
        break;

      case Types.T_LONG:
        ti.longPush(((Long) ret).longValue());
        retSize=2;
        break;

      case Types.T_FLOAT:
        ival = Types.floatToInt(((Float) ret).floatValue());
        ti.push(ival, false);
        break;

      case Types.T_DOUBLE:
        lval = Types.doubleToLong(((Double) ret).doubleValue());
        ti.longPush(lval);
        retSize=2;
        break;

      default:
        // everything else is supposed to be a reference
        ti.push(((Integer) ret).intValue(), true);
      }

      if (retAttr != null) {
        StackFrame frame = ti.getTopFrame(); // no need to clone anymore after pushing the value
        if (retSize == 1) {
          frame.setOperandAttr(retAttr);
        } else {
          frame.setLongOperandAttr(retAttr);
        }
      }
    }
  }

  @Override
  public Object getReturnAttr (ThreadInfo ti) {
    if (isCompleted(ti)){
      return retAttr;
    } else {
      NativeStackFrame nativeFrame = (NativeStackFrame) ti.getTopFrame();
      return nativeFrame.getReturnAttr();
    }
  }


  @Override
  public Object getReturnValue(ThreadInfo ti) {
    if (isCompleted(ti)){
      return ret;
    } else {
      NativeStackFrame nativeFrame = (NativeStackFrame) ti.getTopFrame();
      return nativeFrame.getReturnValue();
    }
  }

  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append("nativereturn ");
    sb.append(mi.getFullName());

    return sb.toString();
  }

}
