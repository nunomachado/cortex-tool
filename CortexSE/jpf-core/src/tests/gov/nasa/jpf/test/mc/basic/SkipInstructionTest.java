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
package gov.nasa.jpf.test.mc.basic;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

public class SkipInstructionTest extends TestJPF {

  public static class Listener extends ListenerAdapter {

    public void executeInstruction(JVM vm) {
      ThreadInfo ti = vm.getCurrentThread();
      Instruction pc = ti.getPC();

      if (pc instanceof GETFIELD) {
        GETFIELD gf = (GETFIELD) pc;
        if (gf.getVariableId().equals(SkipInstructionTest.class.getName() + ".answer")) {
          System.out.println("now intercepting: " + pc);

          // simulate the operand stack behavior of the skipped insn
          ti.pop();
          ti.push(42, false);

          ti.skipInstruction(pc.getNext());
        }
      }
    }
  }

  //--- the test methods

  int answer = 0;
  
  @Test public void testGETFIELD () {

    if (verifyNoPropertyViolation("+listener=gov.nasa.jpf.test.mc.basic.SkipInstructionTest$Listener")){
      int i = answer; // to be intercepted by listener
    
      System.out.println(i);
      assert (i == 42) : "get_field not intercepted: " + i;
    }
  }
  
}
