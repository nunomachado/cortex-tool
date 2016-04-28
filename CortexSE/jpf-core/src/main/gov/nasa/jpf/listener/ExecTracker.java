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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.annotation.JPFOption;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.InstanceFieldInstruction;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.LockInstruction;
import gov.nasa.jpf.search.Search;

import java.io.PrintWriter;

/**
 * Listener tool to monitor JPF execution. This class can be used as a drop-in
 * replacement for JPF, which is called by ExecTracker.
 * ExecTracker is mostly a VMListener of 'instructionExecuted' and
 * a SearchListener of 'stateAdvanced' and 'statehBacktracked'
 */

public class ExecTracker extends ListenerAdapter {
  
  @JPFOption(type = "Boolean", key = "et.print_insn", defaultValue = "true", comment = "print executed bytecode instructions") 
  boolean printInsn = true;
  
  @JPFOption(type = "Boolean", key = "et.print_src", defaultValue = "false", comment = "print source lines")
  boolean printSrc = false;
  
  @JPFOption(type = "Boolean", key = "et.print_mth", defaultValue = "false", comment = "print executed method names")
  boolean printMth = false;
  
  @JPFOption(type = "Boolean", key = "et.skip_init", defaultValue = "true", comment = "do not log execution before entering main()")
  boolean skipInit = false;
  
  PrintWriter out;
  String lastLine;
  MethodInfo lastMi;
  String linePrefix;
  
  boolean skip;
  MethodInfo miMain; // just to make init skipping more efficient
  
  public ExecTracker (Config config) {
    /** @jpfoption et.print_insn : boolean - print executed bytecode instructions (default=true). */
    printInsn = config.getBoolean("et.print_insn", true);

    /** @jpfoption et.print_src : boolean - print source lines (default=false). */
    printSrc = config.getBoolean("et.print_src", false);

    /** @jpfoption et.print_mth : boolean - print executed method names (default=false). */
    printMth = config.getBoolean("et.print_mth", false);

    /** @jpfoption et.skip_init : boolean - do not log execution before entering main() (default=true). */
    skipInit = config.getBoolean("et.skip_init", true);
    
    if (skipInit) {
      skip = true;
    }
    
    out = new PrintWriter(System.out, true);
  }
  
  /******************************************* SearchListener interface *****/
  
  public void stateRestored(Search search) {
    int id = search.getStateId();
    out.println("----------------------------------- [" +
                       search.getDepth() + "] restored: " + id);
  }
    
  //--- the ones we are interested in
  public void searchStarted(Search search) {
    out.println("----------------------------------- search started");
    if (skipInit) {
      ClassInfo ci = search.getVM().getMainClassInfo();
      miMain = ci.getMethod("main([Ljava/lang/String;)V", false);
      
      out.println("      [skipping static init instructions]");
    }
  }

  public void stateAdvanced(Search search) {
    int id = search.getStateId();
    
    out.print("----------------------------------- [" +
                     search.getDepth() + "] forward: " + id);
    if (search.isNewState()) {
      out.print(" new");
    } else {
      out.print(" visited");
    }
    
    if (search.isEndState()) {
      out.print(" end");
    }
    
    out.println();
    
    lastLine = null; // in case we report by source line
    lastMi = null;
    linePrefix = null;
  }

  public void stateProcessed (Search search) {
    int id = search.getStateId();
    out.println("----------------------------------- [" +
                       search.getDepth() + "] done: " + id);
  }

  public void stateBacktracked(Search search) {
    int id = search.getStateId();

    lastLine = null;
    lastMi = null;

    out.println("----------------------------------- [" +
                       search.getDepth() + "] backtrack: " + id);
  }
  
  public void searchFinished(Search search) {
    out.println("----------------------------------- search finished");
  }

  /******************************************* VMListener interface *********/

  public void gcEnd(JVM vm) {
    out.println("\t\t # garbage collection");
  }

  //--- the ones we are interested in
  public void instructionExecuted(JVM jvm) {
    
    if (skip) {
      Instruction insn = jvm.getLastInstruction();
      MethodInfo mi = insn.getMethodInfo();
      if (mi == miMain) {
        skip = false; // start recording
      } else {
        return;  // skip
      }
    }
    
    ThreadInfo ti = jvm.getLastThreadInfo();
    int nNoSrc = 0;
    
    if (linePrefix == null) {
      linePrefix = Integer.toString( ti.getId()) + " : ";
    }
    
    // that's pretty redundant to what is done in the ConsolePublisher, but we don't want 
    // presentation functionality in Step anymore
    Instruction insn = jvm.getLastInstruction();
    if (printSrc) {
      String line = insn.getSourceLine();
      if (line != null){
        if (nNoSrc > 0) {
          out.println("            [" + nNoSrc + " insn w/o sources]");
        }

        if (!line.equals(lastLine)) {
          out.print("            [");
          out.print(insn.getFileLocation());
          out.print("] : ");
          out.println(line.trim());
        }
        
        nNoSrc = 0;
        
      } else { // no source
        nNoSrc++;
      }
      
      lastLine = line;
    }
    
    if (printInsn) {      
      if (printMth) {
        MethodInfo mi = insn.getMethodInfo();
        if (mi != lastMi){
          ClassInfo mci = mi.getClassInfo();
          out.print("      ");
          if (mci != null) {
            out.print(mci.getName());
            out.print(".");
          }
          out.println(mi.getUniqueName());
          lastMi = mi;
        }
      }
      
      out.print( linePrefix);
      
      out.print('[');
      out.print(insn.getInstructionIndex());
      out.print("] ");
      
      out.print(insn);
        
      // annotate (some of) the bytecode insns with their arguments
      if (insn instanceof InvokeInstruction) {
        MethodInfo callee = ((InvokeInstruction)insn).getInvokedMethod(); 
        if ((callee != null) && callee.isMJI()) { // Huhh? why do we have to check this?
          out.print(" [native]");
        }
      } else if (insn instanceof FieldInstruction) {
        out.print(" ");
        if (insn instanceof InstanceFieldInstruction){
          InstanceFieldInstruction iinsn = (InstanceFieldInstruction)insn;
          out.print(iinsn.getId(iinsn.getLastElementInfo()));
        } else {
          out.print(((FieldInstruction)insn).getVariableId());
        }
      } else if (insn instanceof LockInstruction) {
        LockInstruction lockInsn = (LockInstruction)insn;
        int lockRef = lockInsn.getLastLockRef();

        out.print(" ");
        out.print( ti.getElementInfo(lockRef));
      }
      out.println();
    }
  }

  public void threadStarted(JVM jvm) {
    ThreadInfo ti = jvm.getLastThreadInfo();

    out.println( "\t\t # thread started: " + ti.getName() + " index: " + ti.getId());
  }

  public void threadTerminated(JVM jvm) {
    ThreadInfo ti = jvm.getLastThreadInfo();
    
    out.println( "\t\t # thread terminated: " + ti.getName() + " index: " + ti.getId());
  }
  
  public void notifyExceptionThrown (JVM jvm) {
    ElementInfo ei = jvm.getLastElementInfo();
    MethodInfo mi = jvm.getLastThreadInfo().getMethod();
    out.println("\t\t\t\t # exception: " + ei + " in " + mi);
  }
  
  public void choiceGeneratorAdvanced (JVM jvm) {
    out.println("\t\t # choice: " + jvm.getLastChoiceGenerator());
  }
  
  /****************************************** private stuff ******/

  void filterArgs (String[] args) {
    for (int i=0; i<args.length; i++) {
      if (args[i] != null) {
        if (args[i].equals("-print-lines")) {
          printSrc = true;
          args[i] = null;
        }
      }
    }
  }
}

