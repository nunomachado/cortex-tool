package gov.nasa.jpf.symbc;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.jvm.bytecode.VirtualInvocation;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class TestSymbolicListener extends SymbolicListener {

  /*
   * The general strategy here is that the listener records all of the arguments
   * that are sent to System.out.println()
   * 
   * To do this, we need to know the output at every state transition, and be
   * able to backtrack the outputs as the VM backtracks states. We do this by
   * placing the outputs in a stack. Each stack frame represents the output
   * between two transitions. When the VM advances state, a new frame is pushed
   * on. When the VM backtracks a state one frame is popped off.
   * 
   */

  /**
   * Default boring constructor
   * 
   * @param conf
   *            JPF Configure object
   * @param jpf
   *            JPF application
   */
  public TestSymbolicListener(Config conf, JPF jpf) {
    super(conf, jpf);
  }
  
  public Map<Integer, String> stateMap = new TreeMap<Integer,String>();
 
  public StringBuffer currentBuffer = new StringBuffer();

  /**
   * Stack of outputs
   */

  protected Map<PathCondition, String> outputs = new HashMap<PathCondition, String>();

  public Map<PathCondition, String> getOutputs() {
    return Collections.unmodifiableMap(outputs);
  }

  /**
   * Flag to be set to true once we enter a symbolic method
   */
  protected boolean currentlySymbolic = false;

  @Override
  public void stateAdvanced(Search search) {
    stateMap.put(search.getStateId(), currentBuffer.toString());

  }

  @Override
  public void stateBacktracked(Search search) {
    int state = search.getStateId();
    if(state != -1)
      currentBuffer = new StringBuffer(stateMap.get(state));
  }
  
  @Override
  public void stateRestored(Search search) {
    System.err.println("eh?");
  }
  
  /**
   * Place the string on the output stack
   * 
   * @param str
   *            String to place on output stack
   */
  protected void recordOutput(String str) {
    currentBuffer.append(str);
  }

  /**
   * Return the current output. This is all of the output sent to
   * System.out.println up to this point in the program
   * 
   * @return Current output of the virtual machine
   */
  public String currentOutput() {
    return currentBuffer.toString();
  }

  /**
   * Get the program condition choice generator for the state before the
   * argument. If the argument is a <code>PCChoiceGenerator</code>, search
   * backwards in the stack for the previous <code>PCChoiceGenerator</code>.
   * Otherwise, just search backwards, starting from the state represented by
   * the argument, for the first <code>PCChoiceGenerator</code>
   * 
   * @param cg
   *            A choice generator
   * @return The latest <code>PCChoiceGenerator</code> to proceeded the
   *         argument
   */
  protected PCChoiceGenerator getPreviousPCCG(ChoiceGenerator<?> cg) {
    ChoiceGenerator<?> prev_cg;

    if (cg == null)
      return null;

    prev_cg = cg.getPreviousChoiceGenerator();
    while (prev_cg != null && !(prev_cg instanceof PCChoiceGenerator)) {
      prev_cg = prev_cg.getPreviousChoiceGenerator();
    }

    return (PCChoiceGenerator) prev_cg;
  }

  /**
   * Get the current program condition choice generator given the latest choice
   * generator. If the parameter is a <code>PCChoiceGenerator</code>, then we
   * return the argument. If not, we go back though the ChoiceGenerator stack to
   * find the latest <code>PCChoiceGenerator</code>
   * 
   * @param current_cg
   *            Current choice generator
   * @return Latest Program Condition Choice Generator. Returns
   *         <code>null</code> if <code>current_cg</code> is not a
   *         <code>PCChoiceGenerator</code> and there is no
   *         <code>PCChoiceGenerator</code> in the stack before the argument
   */
  protected PCChoiceGenerator getCurrentPCCG(ChoiceGenerator<?> current_cg) {
    if (current_cg == null)
      return null;

    if (current_cg instanceof PCChoiceGenerator)
      return (PCChoiceGenerator) current_cg;

    return getPreviousPCCG(current_cg);
  }

  /**
   * Get the current program condition at the state transition marked by a
   * choice generator.
   * 
   * @param current_cg
   *            A choice generator
   * @return The program condition at the transition symbolized by the choice
   *         generator. Returns <code>null</code> if there is no program
   *         condition
   */
  protected PathCondition getCurrentPC(ChoiceGenerator<?> current_cg) {
    PCChoiceGenerator current_pccg = getCurrentPCCG(current_cg);

    if (current_pccg == null)
      return null;

    return current_pccg.getCurrentPC();
  }

  /**
   * Check to see if a method is to be executed symbolically.
   * 
   * @param config
   *            JPF Config information
   * @param methodName
   *            Name of the method
   * @param numArgs
   *            Number of arguments to the method
   * @param mi
   *            Method Info structure
   * @param className
   *            Name of the class containing the method
   * @return True if the method is to be executed symbolically. False Otherwise
   */
  protected boolean checkSymbolicEntry(Config config, String methodName,
      int numArgs, MethodInfo mi, String className) {
	  //neha: changed methodname to the full name
    return (BytecodeUtils.isClassSymbolic(config, className, mi, methodName))
        || BytecodeUtils.isMethodSymbolic(config, mi.getFullName(), numArgs, null);
  }

  /**
   * If the call is a call to System.out.println, put the parameter on the
   * output stack. If there is no parameter, put a newline character
   * 
   * @param call
   *            method call
   * @param ti
   *            Thread information
   */
  protected void recordOutput(VirtualInvocation call, ThreadInfo ti) {
    MethodInfo mi = call.getInvokedMethod(ti);
    int sysOutRef = ti.getEnv().getStaticReferenceField("java.lang.System",
        "out");

    if (call.getCalleeThis(ti) == sysOutRef && mi.getName().equals("println")) {

      if (call.getArgSize() != 1) {
        recordOutput(((ElementInfo) call.getArgumentValues(ti)[0]).asString());

      }
      recordOutput("\n");
    }
  }

  @Override
  public void instructionExecuted(JVM vm) {

    SystemState ss = vm.getSystemState();

    Instruction last_insn;
    ThreadInfo ti;

    last_insn = vm.getLastInstruction();
    ti = vm.getLastThreadInfo();

    /*
     * check to see if we have entered a symbolic method if we haven't already
     * entered one
     */
    if (last_insn instanceof InvokeInstruction && !currentlySymbolic) {
      InvokeInstruction md = (InvokeInstruction) last_insn;
      MethodInfo mi = md.getInvokedMethod();

      currentlySymbolic = checkSymbolicEntry(vm.getConfig(), md
          .getInvokedMethodName(), md.getArgumentValues(ti).length, md
          .getInvokedMethod(), mi.getClassName());

    }

    if (!currentlySymbolic) {
      super.instructionExecuted(vm);
      return;
    }

    /*
     * If this is a method call, record the output (if it's a call to println)
     */
    if (last_insn instanceof VirtualInvocation) {
      recordOutput((VirtualInvocation) last_insn, ti);

    }
    /* On a method return, record the output in a table */
    else if (last_insn instanceof ReturnInstruction) {
      MethodInfo mi = last_insn.getMethodInfo();
      ClassInfo ci = mi.getClassInfo();
      if (null != ci) {
        if (checkSymbolicEntry(vm.getConfig(), mi.getName(), mi
            .getNumberOfArguments(), mi, ci.getName())) {
          PathCondition pc = getCurrentPC(ss.getChoiceGenerator());
          if(pc == null) {
            pc = new PathCondition();
          } 
          outputs.put(pc, currentOutput());
        }
      }
    }
    
    super.instructionExecuted(vm);
  }

  public void publishFinished(Publisher publisher) {
    super.publishFinished(publisher);

    publisher.publishTopicStart("Output");
    for (PathCondition pc : outputs.keySet()) {
      publisher.getOut().println(pc);
      publisher.getOut().println(outputs.get(pc));

  }
}
}
