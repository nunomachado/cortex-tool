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

package gov.nasa.jpf.test.mc.basic;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.jvm.bytecode.EXECUTENATIVE;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.choice.IntChoiceFromList;
import gov.nasa.jpf.util.test.TestJPF;

import java.util.ArrayList;

import org.junit.Test;

/**
 * regression test for CG notifications
 */
public class CGNotificationTest extends TestJPF {

  public static class Sequencer extends ListenerAdapter {

    static ArrayList<String> sequence;

    public void choiceGeneratorRegistered(JVM vm) {
      ChoiceGenerator<?> cg = vm.getLastChoiceGenerator();
      System.out.println("# CG registered: " + cg);
      sequence.add("registered " + cg.getId());

      assert cg.hasMoreChoices();
    }

    public void choiceGeneratorSet(JVM vm) {
      ChoiceGenerator<?> cg = vm.getLastChoiceGenerator();
      System.out.println("# CG set:        " + cg);
      sequence.add("set " + cg.getId());

      assert cg.hasMoreChoices();
    }

    public void choiceGeneratorAdvanced(JVM vm) {
      ChoiceGenerator<?> cg = vm.getLastChoiceGenerator();
      System.out.println("#   CG advanced: " + cg);
      sequence.add("advance " + cg.getId() + ' ' + cg.getNextChoice());
    }

    public void choiceGeneratorProcessed(JVM vm) {
      ChoiceGenerator<?> cg = vm.getLastChoiceGenerator();
      System.out.println("# CG processed:  " + cg);
      sequence.add("processed " + cg.getId());

      assert !cg.hasMoreChoices();
    }

    public void instructionExecuted(JVM vm){
      Instruction insn = vm.getLastInstruction();
      ThreadInfo ti = vm.getLastThreadInfo();
      SystemState ss = vm.getSystemState();

      if (insn instanceof EXECUTENATIVE) { // break on native method exec
        EXECUTENATIVE exec = (EXECUTENATIVE) insn;

        if (exec.getExecutedMethodName().equals("getInt")){// this insn did create a CG
          if (!ti.isFirstStepInsn()){

            ChoiceGenerator<Integer> cg = new IntChoiceFromList("listenerCG", 3,4);
            ss.setNextChoiceGenerator(cg);
          }
        }
      }

    }
  }

  @Test
  public void testCGNotificationSequence () {
    if (!isJPFRun()){
      Sequencer.sequence = new ArrayList<String>();
    }

    // make sure max insn preemption does not interfere 
    if (verifyNoPropertyViolation("+listener=.test.mc.basic.CGNotificationTest$Sequencer",
                                  "+vm.max_transition_length=MAX")){
      boolean b = Verify.getBoolean(); // first CG
      int i = Verify.getInt(1,2); // this one gets a CG on top registered by the listener
/*
      System.out.print("b=");
      System.out.print(b);
      System.out.print(",i=");
      System.out.println(i);
*/
    }

    if (!isJPFRun()){
      String[] expected = {
        "registered <root>",
        "set <root>",
        "advance <root> ThreadInfo [name=main,id=0,state=RUNNING]",
        "registered verifyGetBoolean",
        "set verifyGetBoolean",
        "advance verifyGetBoolean false",
        "registered verifyGetInt(II)",
        "registered listenerCG",
        "set verifyGetInt(II)",
        "set listenerCG",
        "advance verifyGetInt(II) 1",
        "advance listenerCG 3",
        "advance listenerCG 4",
        "processed listenerCG",
        "advance verifyGetInt(II) 2",
        "advance listenerCG 3",
        "advance listenerCG 4",
        "processed listenerCG",
        "processed verifyGetInt(II)",
        "advance verifyGetBoolean true",
        "registered verifyGetInt(II)",
        "registered listenerCG",
        "set verifyGetInt(II)",
        "set listenerCG",
        "advance verifyGetInt(II) 1",
        "advance listenerCG 3",
        "advance listenerCG 4",
        "processed listenerCG",
        "advance verifyGetInt(II) 2",
        "advance listenerCG 3",
        "advance listenerCG 4",
        "processed listenerCG",
        "processed verifyGetInt(II)",
        "processed verifyGetBoolean",
        "processed <root>"
      };

      assert Sequencer.sequence.size() == expected.length;

      int i=0;
      for (String s : Sequencer.sequence){
        assert expected[i].equals(s) : "\"" + expected[i] + "\" != \"" + s + "\"";
        //System.out.println("\"" + s + "\",");
        i++;
      }
    }
  }
}
