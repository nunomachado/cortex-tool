//
// Copyright (C) 2012 United States Government as represented by the
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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.JPFLogger;

/**
 * listener that throws a java.lang.StackOverflowError in case a thread
 * exceeds a configured max stack depth
 * 
 * <2do> - maybe we should only count visible stackframes, i.e. the ones for
 * which we have invoke insns on the stack
 */
public class StackDepthChecker extends ListenerAdapter {
  
  static JPFLogger log = JPF.getLogger("gov.nasa.jpf.listener.StackDepthChecker");

  protected int maxDepth;
  
  public StackDepthChecker (Config config, JPF jpf){
    maxDepth = config.getInt( "sdc.max_stack_depth", 42);
  }
  
  @Override
  public void methodEntered (JVM vm){
    
    ThreadInfo ti = vm.getCurrentThread();
    int depth = ti.getStackDepth(); // note this is only an approximation since it also returns natives and overlays
    
    if (depth > maxDepth){
      log.info("configured vm.max_stack_depth exceeded: ", depth);
      
      // NOTE - we get this notification from inside of the InvokeInstruction.execute(),
      // i.e. before we get the instructionExecuted(). Throwing exceptions is
      // therefore a bit harder since we have to set the next pc explicitly

      Instruction nextPc = ti.createAndThrowException("java.lang.StackOverflowError");
      StackFrame topFrame = ti.getClonedTopFrame();
      topFrame.setPC(nextPc);
    }
  }
}
