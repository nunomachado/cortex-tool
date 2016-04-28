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
package gov.nasa.jpf.listener;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;

/**
 * this isn't yet a useful tool, but it shows how to track method calls with
 * their corresponding argument values
 */
public class CallMonitor extends ListenerAdapter {

  public void instructionExecuted (JVM vm) {
    Instruction insn = vm.getLastInstruction();
    ThreadInfo ti = vm.getLastThreadInfo();
    
    if (insn instanceof InvokeInstruction) {
      if (insn.isCompleted(ti) && !ti.isInstructionSkipped()) {
        InvokeInstruction call = (InvokeInstruction)insn;
        MethodInfo mi = call.getInvokedMethod();
        Object[] args = call.getArgumentValues(ti);
        ClassInfo ci = mi.getClassInfo();

        StringBuilder sb = new StringBuilder();

        sb.append(ti.getId());
        sb.append(": ");

        int d = ti.getStackDepth();
        for (int i=0; i<d; i++){
          sb.append(" ");
        }

        if (ci != null){
          sb.append(ci.getName());
          sb.append('.');
        }
        sb.append(mi.getName());
        sb.append('(');

        int n = args.length-1;
        for (int i=0; i<=n; i++) {
          if (args[i] != null) {
            sb.append(args[i].toString());
          } else {
            sb.append("null");
          }
          if (i<n) {
            sb.append(',');
          }
        }
        sb.append(')');

        System.out.println(sb);
      }
    }
  }
}
