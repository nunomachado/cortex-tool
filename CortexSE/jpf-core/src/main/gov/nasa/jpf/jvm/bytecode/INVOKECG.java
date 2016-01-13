//
// Copyright (C) 2007 United States Government as represented by the
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
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.Ref;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;
import gov.nasa.jpf.jvm.choice.InvocationCG;
import gov.nasa.jpf.util.Invocation;

import java.util.List;

/**
 * a sytnthetic INVOKE instruction that gets it's parameters from an
 * InvocationCG. Whoever uses this better makes sure the frame this
 * executes in has enough operand space (e.g. a DirectCallStackFrame).
 * 
 */
public class INVOKECG extends Instruction {

  List<Invocation>  invokes;
  InvokeInstruction realInvoke;

  public INVOKECG(List<Invocation> invokes){
    this.invokes = invokes;
  }


  public void setInvokes(List<Invocation> invokes) {
    this.invokes = invokes;
  }
  
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    
    if (!ti.isFirstStepInsn()) {
      InvocationCG cg = new InvocationCG( "INVOKECG", invokes);
      if (ss.setNextChoiceGenerator(cg)){
        return this;
      }
      
    } else {
      InvocationCG cg = ss.getCurrentChoiceGenerator( "INVOKECG", InvocationCG.class);
      assert (cg != null) : "no current InvocationCG";

      Invocation call = cg.getNextChoice();
      MethodInfo callee = call.getMethodInfo();
      gov.nasa.jpf.jvm.InstructionFactory insnFactory = MethodInfo.getInstructionFactory();

      String clsName = callee.getClassInfo().getName();
      String mthName = callee.getName();
      String signature = callee.getSignature();

      Instruction realInvoke;
      if (callee.isStatic()){
        realInvoke = insnFactory.invokestatic(clsName, mthName, signature);
      } else {
        realInvoke = insnFactory.invokevirtual(clsName, mthName, signature);
      }
      
      pushArguments(ti, call.getArguments(), call.getAttrs());
      
      return realInvoke;
    }

    return getNext();
  }

  void pushArguments (ThreadInfo ti, Object[] args, Object[] attrs){
    if (args != null){
      for (int i=0; i<args.length; i++){
        Object a = args[i];
        boolean isLong = false;
        
        if (a != null){
          if (a instanceof Ref){
            ti.push(((Ref)a).getReference(), true);
          } else if (a instanceof Boolean){
            ti.push((Boolean)a ? 1 : 0, false);
          } else if (a instanceof Integer){
            ti.push((Integer)a, false);
          } else if (a instanceof Long){
            ti.longPush((Long)a);
            isLong = true;
          } else if (a instanceof Double){
            ti.longPush(Types.doubleToLong((Double)a));
            isLong = true;
          } else if (a instanceof Byte){
            ti.push((Byte)a, false);
          } else if (a instanceof Short){
            ti.push((Short)a, false);
          } else if (a instanceof Float){
            ti.push(Types.floatToInt((Float)a), false);
          }
        }

        if (attrs != null && attrs[i] != null){
          if (isLong){
            ti.setLongOperandAttrNoClone(attrs[i]);
          } else {
            ti.setOperandAttrNoClone(attrs[i]);
          }
        }
      }
    }
  }
  
  public boolean isExtendedInstruction() {
    return true;
  }

  public static final int OPCODE = 258;

  public int getByteCode () {
    return OPCODE;
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
  
}
