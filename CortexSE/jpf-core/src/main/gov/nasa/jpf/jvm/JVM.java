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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.JPFListenerException;
import gov.nasa.jpf.classfile.ClassFile;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.Misc;

import java.io.PrintWriter;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;


/**
 * This class represents the virtual machine. The virtual machine is able to
 * move backward and forward one transition at a time.
 */
public class JVM {

  /**
   * this is a debugging aid to control compilation of expensive consistency checks
   * (we don't control these with class-wise assertion enabling since we do use
   * unconditional assertions for mandatory consistency checks)
   */
  public static final boolean CHECK_CONSISTENCY = false;
  
  protected static JPFLogger log = JPF.getLogger("gov.nasa.jpf.jvm.JVM");

  /**
   * our execution context
   */
  JPF jpf;

  /**
   * The number of errors saved so far.
   * Used to generate the name of the error trail file.
   */
  protected static int error_id;

  /**
   * <2do> - this is a hack to be removed once there are no static references
   * anymore
   */
  protected static JVM jvm;

  static {
    initStaticFields();
  }

  protected SystemState ss;

  // <2do> - if you are confused about the various pieces of state and its
  // storage/backtrack structures, I'm with you. It's mainly an attempt to
  // separate non-policy VM state (objects), policy VM state (Scheduler)
  // and general JPF execution state, with special support for stack oriented
  // state restoration (backtracking).
  // this needs to be cleaned up and the principle reinstated


  protected String mainClassName;
  protected String[] args;  /** tiMain() arguments */

  protected Path path;  /** execution path to current state */
  protected StringBuilder out;  /** buffer to store output along path execution */


  /**
   * various caches for VMListener state acquisition. NOTE - these are only
   * valid during notification
   *
   * <2do> get rid of the 'lasts' in favor of queries on the insn, the executing
   * thread, and the VM. This is superfluous work to for every notification
   * (even if there are no listeners using it) that can easily lead to inconsistencies
   */
  protected Transition      lastTrailInfo;
  protected ClassInfo       lastClassInfo;
  protected ThreadInfo      lastThreadInfo;
  protected Instruction     lastInstruction;
  protected Instruction     nextInstruction;
  protected ElementInfo     lastElementInfo;
  protected MethodInfo      lastMethodInfo;
  protected ChoiceGenerator<?> lastChoiceGenerator;

  protected boolean isTraceReplay; // can be set by listeners to indicate this is a replay

  /** the repository we use to find out if we already have seen a state */
  protected StateSet stateSet;

  /** this was the last stateId - note this is also used for stateless model checking */
  protected int newStateId;

  /** the structure responsible for storing and restoring backtrack info */
  protected Backtracker backtracker;

  /** optional serializer/restorer to support backtracker */
  protected StateRestorer<?> restorer;

  /** optional serializer to support stateSet */
  protected StateSerializer serializer;

  /** potential execution listeners. We keep them in a simple array to avoid
   creating objects on each notification */
  protected VMListener[] listeners = new VMListener[0];

  /** did we get a new transition */
  protected boolean transitionOccurred;

  /** how we model execution time */
  protected TimeModel timeModel;
  
  protected Config config; // that's for the options we use only once

  // JVM options we use frequently
  protected boolean runGc;
  protected boolean treeOutput;
  protected boolean pathOutput;
  protected boolean indentOutput;
  
  // <2do> there are probably many places where this should be used
  protected boolean isBigEndian;

  // a list of actions to be run post GC. This is a bit redundant to VMListener,
  // but in addition to avoid the per-instruction execution overhead of a VMListener
  // we want a (internal) mechanism that is on-demand only, i.e. processed
  // actions are removed from the list
  protected ArrayList<Runnable> postGcActions = new ArrayList<Runnable>();
  
  /**
   * be prepared this might throw JPFConfigExceptions
   */
  public JVM (JPF jpf, Config conf) {
    this.jpf = jpf; // so that we know who instantiated us

    // <2do> that's really a bad hack and should be removed once we
    // have cleaned up the reference chains
    jvm = this;

    config = conf;

    runGc = config.getBoolean("vm.gc", true);

    treeOutput = config.getBoolean("vm.tree_output", true);
    // we have to defer setting pathOutput until we have a reporter registered
    indentOutput = config.getBoolean("vm.indent_output",false);

    isBigEndian = getPlatformEndianness(config);
    
    initTimeModel(config);

    initSubsystems(config);
    initFields(config);
  }

  /**
   * just here for unit test mockups, don't use as implicit base ctor in
   * JVM derived classes
   */
  protected JVM (){}

  public JPF getJPF() {
    return jpf;
  }

  public void initFields (Config config) {
    mainClassName = config.getTarget(); // we don't get here if it wasn't set
    args = config.getTargetArgs();

    path = new Path(mainClassName);
    out = null;

    ss = new SystemState(config, this);

    stateSet = config.getInstance("vm.storage.class", StateSet.class);
    if (stateSet != null) stateSet.attach(this);
    backtracker = config.getEssentialInstance("vm.backtracker.class", Backtracker.class);
    backtracker.attach(this);

    newStateId = -1;
  }

  protected void initSubsystems (Config config) {
    ClassInfo.init(config);
    ThreadInfo.init(config);
    ElementInfo.init(config);
    MethodInfo.init(config);
    NativePeer.init(config);
    FieldInstruction.init(config);
    ChoiceGeneratorBase.init(config);

    // peer classes get initialized upon NativePeer creation
  }

  protected void initTimeModel (Config config){
    Class<?>[] argTypes = { JVM.class, Config.class };
    Object[] args = { this, config };
    timeModel = config.getEssentialInstance("vm.time.class", TimeModel.class, argTypes, args);
  }
  
  /**
   * called after the JPF run is finished. Shouldn't be public, but is called by JPF
   */
  public void cleanUp(){
    // nothing yet
  }
  
  protected boolean getPlatformEndianness (Config config){
    String endianness = config.getString("vm.endian");
    if (endianness == null) {
      return ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    } else if (endianness.equalsIgnoreCase("big")) {
      return true;
    } else if (endianness.equalsIgnoreCase("little")) {
      return false;
    } else {
      config.exception("illegal vm.endian value: " + endianness);
      return false; // doesn't matter
    }
  }
  
  public boolean isBigEndianPlatform(){
    return isBigEndian;
  }

  /**
   * do we see our model classes? Some of them cannot be used from the standard CLASSPATH, because they
   * are tightly coupled with the JPF core (e.g. java.lang.Class, java.lang.Thread,
   * java.lang.StackTraceElement etc.)
   * Our strategy here is kind of lame - we just look into java.lang.Class, if we find the 'int cref' field
   * (that's a true '42')
   */
  static boolean checkModelClassAccess () {
    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Class");
    return (ci.getDeclaredInstanceField("cref") != null);
  }

  static boolean checkClassName (String clsName) {
    if ( !clsName.matches("[a-zA-Z_$][a-zA-Z_$0-9.]*")) {
      return false;
    }

    // well, those two could be part of valid class names, but
    // in all likeliness somebody specified a filename instead of
    // a classname
    if (clsName.endsWith(".java")) {
      return false;
    }
    if (clsName.endsWith(".class")) {
      return false;
    }

    return true;
  }

  /**
   * load and pushClinit startup classes, return 'true' if successful.
   *
   * This loads a bunch of core library classes, initializes the tiMain thread,
   * and then all the required startup classes, but excludes the static init of
   * the tiMain class. Note that whatever gets executed in here should NOT contain
   * any non-determinism, since we are not backtrackable yet, i.e.
   * non-determinism in clinits should be constrained to the app class (and
   * classes used by it)
   */
  public boolean initialize () {

    if (!checkClassName(mainClassName)) {
      log.severe("Not a valid main class: " + mainClassName);
      return false;
    }

    // from here, we get into some bootstrapping process
    //  - first, we have to load class structures (fields, supers, interfaces..)
    //  - second, we have to create a thread (so that we have a stack)
    //  - third, with that thread we have to create class objects
    //  - forth, we have to push the clinit methods on this stack
    List<ClassInfo> clinitQueue = registerStartupClasses();

    if (clinitQueue== null) {
      log.severe("error initializing startup classes (check 'classpath' and 'target')");
      return false;
    }

    if (!checkModelClassAccess()) {
      log.severe( "error during VM runtime initialization: wrong model classes (check 'classpath')");
      return false;
    }

    // create the thread for the tiMain class
    // note this is incomplete for Java 1.3 where Thread ctors rely on tiMain's
    // 'inheritableThreadLocals' being set to 'Collections.EMPTY_SET', which
    // pulls in the whole Collections/Random smash, but we can't execute the
    // Collections.<clinit> yet because there's no stack before we have a tiMain
    // thread. Let's hope none of the init classes creates threads in their <clinit>.
    ThreadInfo tiMain = createMainThread();

    // now that we have a tiMain thread, we can finish the startup class init
    createStartupClassObjects(clinitQueue, tiMain);

    // pushClinit the call stack with the clinits we've picked up, followed by tiMain()
    pushMainEntry(tiMain);
    pushClinits(clinitQueue, tiMain);

    initSystemState(tiMain);
    registerThreadListCleanup();
    
    return true;
  }

  protected void initSystemState (ThreadInfo mainThread){
    ss.setStartThread(mainThread);

    // the first transition probably doesn't have much choice (unless there were
    // threads started in the static init), but we want to keep it uniformly anyways
    ChoiceGenerator<?> cg = new ThreadChoiceFromSet("<root>", getThreadList().getRunnableThreads(), true);
    ss.setMandatoryNextChoiceGenerator(cg, "no root CG");

    ss.recordSteps(hasToRecordSteps());

    if (!pathOutput) { // don't override if explicitly requested
      pathOutput = hasToRecordPathOutput();
    }

    transitionOccurred = true;
  }

  /**
   * be careful - everything that's executed from within here is not allowed
   * to depend on static class init having been done yet
   *
   * we have to do the initialization excplicitly here since we can't execute
   * bytecode yet (which would need a ThreadInfo context)
   */
  protected ThreadInfo createMainThread () {
    Heap heap = getHeap();

    ClassInfo ciThread = ClassInfo.getResolvedClassInfo("java.lang.Thread");
    int objRef = heap.newObject( ciThread, null);
    int groupRef = createSystemThreadGroup(objRef);
    int nameRef = heap.newString("main", null);
    
    //--- initialize the main Thread object
    ElementInfo ei = heap.get(objRef);
    ei.setReferenceField("group", groupRef);
    ei.setReferenceField("name", nameRef);
    ei.setIntField("priority", Thread.NORM_PRIORITY);

    int permitRef = heap.newObject(ClassInfo.getResolvedClassInfo("java.lang.Thread$Permit"),null);
    ElementInfo eiPermitRef = heap.get(permitRef);
    eiPermitRef.setBooleanField("blockPark", true);
    ei.setReferenceField("permit", permitRef);

    //--- create the ThreadInfo
    ThreadInfo ti = ThreadInfo.createThreadInfo(this, objRef, groupRef, MJIEnv.NULL, nameRef, 0L);
    
    //--- set it running
    ti.setState(ThreadInfo.State.RUNNING);

    return ti;
  }

  protected int createSystemThreadGroup (int mainThreadRef) {
    Heap heap = getHeap();

    int ref = heap.newObject(ClassInfo.getResolvedClassInfo("java.lang.ThreadGroup"), null);
    ElementInfo ei = heap.get(ref);

    // since we can't call methods yet, we have to init explicitly (BAD)
    // <2do> - this isn't complete yet

    int grpName = heap.newString("main", null);
    ei.setReferenceField("name", grpName);

    ei.setIntField("maxPriority", java.lang.Thread.MAX_PRIORITY);

    int threadsRef = heap.newArray("Ljava/lang/Thread;", 4, null);
    ElementInfo eiThreads = heap.get(threadsRef);
    eiThreads.setReferenceElement(0, mainThreadRef);

    ei.setReferenceField("threads", threadsRef);

    ei.setIntField("nthreads", 1);

    return ref;
  }

  protected void registerThreadListCleanup(){
    ClassInfo ciThread = ClassInfo.tryGetResolvedClassInfo("java.lang.Thread");
    assert ciThread != null : "java.lang.Thread not loaded yet";
    
    ciThread.addReleaseAction( new ReleaseAction(){
      public void release(ElementInfo ei) {
        ThreadList tl = getThreadList();
        int objRef = ei.getObjectRef();
        ThreadInfo ti = tl.getThreadInfoForObjRef(objRef);
        if (tl.remove(ti)){        
          getKernelState().changed();    
        }
      }
    });    
  }

  public void addPostGcAction (Runnable r){
    postGcActions.add(r);
  }
  
  /**
   * to be called from the Heap after GC is completed (i.e. only live objects remain)
   */
  public void processPostGcActions(){
    if (!postGcActions.isEmpty()){
      for (Runnable r : postGcActions){
        r.run();
      }
      
      postGcActions.clear();
    }
  }
  
  protected List<ClassInfo> registerStartupClasses () {
    ArrayList<String> list = new ArrayList<String>(128);
    ArrayList<ClassInfo> queue = new ArrayList<ClassInfo>(32);

    // bare essentials
    list.add("java.lang.Object");
    list.add("java.lang.Class");
    list.add("java.lang.ClassLoader");

    // the builtin types (and their arrays)
    list.add("boolean");
    list.add("[Z");
    list.add("byte");
    list.add("[B");
    list.add("char");
    list.add("[C");
    list.add("short");
    list.add("[S");
    list.add("int");
    list.add("[I");
    list.add("long");
    list.add("[J");
    list.add("float");
    list.add("[F");
    list.add("double");
    list.add("[D");
    list.add("void");

    // the box types
    list.add("java.lang.Boolean");
    list.add("java.lang.Character");
    list.add("java.lang.Short");
    list.add("java.lang.Integer");
    list.add("java.lang.Long");
    list.add("java.lang.Float");
    list.add("java.lang.Double");

    // the cache for box types
    list.add("gov.nasa.jpf.BoxObjectCaches");

    // standard system classes
    list.add("java.lang.String");
    list.add("java.lang.ThreadGroup");
    list.add("java.lang.Thread");
    list.add("java.lang.Thread$State");
    list.add("java.io.PrintStream");
    list.add("java.io.InputStream");
    list.add("java.lang.System");

    // we could be more fancy and use wildcard patterns and the current classpath
    // to specify extra classes, but this could be VERY expensive. Projected use
    // is mostly to avoid static init of single classes during the search
    String[] extraStartupClasses = config.getStringArray("vm.extra_startup_classes");
    if (extraStartupClasses != null) {      
      for (String extraCls : extraStartupClasses) {
        list.add(extraCls);
      }
    }

    // last not least the application main class
    list.add(mainClassName);

    // now resolve all the entries in the list and queue the corresponding ClassInfos
    for (String clsName : list) {
      ClassInfo ci = ClassInfo.tryGetResolvedClassInfo(clsName);
      if (ci != null) {
        registerStartupClass(ci, queue);
      } else {
        log.severe("can't find startup class ", clsName);
        return null;
      }
    }

    return queue;
  }
  

  // note this has to be in order - we don't want to init a derived class before
  // it's parent is initialized
  // This code must be kept in sync with ClassInfo.registerClass()
  void registerStartupClass (ClassInfo ci, List<ClassInfo> queue) {
        
    if (!queue.contains(ci)) {
      if (ci.getSuperClass() != null) {
        registerStartupClass( ci.getSuperClass(), queue);
      }
      
      for (String ifcName : ci.getAllInterfaces()) {
        ClassInfo ici = ClassInfo.getResolvedClassInfo(ifcName);
        registerStartupClass(ici, queue);
      }

      ClassInfo.logger.finer("registering class: ", ci.getName());
      queue.add(ci);

      StaticArea sa = getStaticArea();
      if (!sa.containsClass(ci.getName())){
        sa.addClass(ci, null);
      }
    }
  }
  
  protected void createStartupClassObjects (List<ClassInfo> queue, ThreadInfo ti){
    for (ClassInfo ci : queue) {
      ci.createClassObject(ti);
    }
  }

  protected void pushClinits (List<ClassInfo> queue, ThreadInfo ti) {
    // we have to traverse backwards, since what gets pushed last is executed first
    for (ListIterator<ClassInfo> it=queue.listIterator(queue.size()); it.hasPrevious(); ) {
      ClassInfo ci = it.previous();

      MethodInfo mi = ci.getMethod("<clinit>()V", false);
      if (mi != null) {
        MethodInfo stub = mi.createDirectCallStub("[clinit]");
        StackFrame frame = new DirectCallStackFrame(stub);
        ti.pushFrame(frame);
      } else {
        ci.setInitialized();
      }
    }
  }

  /**
   * override this method if you want your tiMain class entry to be anything else
   * than "public static void tiMain(String[] args)"
   * 
   * Note that we do a directcall here so that we always have a first frame that
   * can't execute SUT code. That way, we can handle synchronized entry points
   * via normal InvokeInstructions, and thread termination processing via
   * DIRECTCALLRETURN
   */
  protected void pushMainEntry (ThreadInfo tiMain) {
    Heap heap = getHeap();
    
    ClassInfo ciMain = ClassInfo.getResolvedClassInfo(mainClassName);
    MethodInfo miMain = ciMain.getMethod("main([Ljava/lang/String;)V", false);

    // do some sanity checks if this is a valid tiMain()
    if (miMain == null || !miMain.isStatic()) {
      throw new JPFException("no main() method in " + ciMain.getName());
    }

    // create the args array object
    int argsRef = heap.newArray("Ljava/lang/String;", args.length, null);
    ElementInfo argsElement = heap.get(argsRef);
    for (int i = 0; i < args.length; i++) {
      int aRef = heap.newString(args[i], tiMain);
      argsElement.setReferenceElement(i, aRef);
    }
    
    // create the direct call stub
    MethodInfo mainStub = miMain.createDirectCallStub("[main]");
    DirectCallStackFrame frame = new DirectCallStackFrame(mainStub);
    frame.pushRef(argsRef);
    // <2do> set RUNSTART pc if we want to catch synchronized tiMain() defects 
    
    tiMain.pushFrame(frame);
  }

  
  public void addListener (VMListener newListener) {
    log.info("VMListener added: ", newListener);
    listeners = Misc.appendElement(listeners, newListener);
  }

  public boolean hasListenerOfType (Class<?> listenerCls) {
    return Misc.hasElementOfType(listeners, listenerCls);
  }

  public <T> T getNextListenerOfType(Class<T> type, T prev){
    return Misc.getNextElementOfType(listeners, type, prev);
  }
  
  public void removeListener (VMListener removeListener) {
    listeners = Misc.removeElement(listeners, removeListener);
  }

  public void setTraceReplay (boolean isReplay) {
    isTraceReplay = isReplay;
  }

  public boolean isTraceReplay() {
    return isTraceReplay;
  }

  public boolean hasToRecordSteps() {
    // we have to record if there either is a reporter that has
    // a 'trace' topic, or there is an explicit request
    return jpf.getReporter().hasToReportTrace()
             || config.getBoolean("vm.store_steps");
  }

  public void recordSteps( boolean cond) {
    // <2do> not ideal - it might be already too late when this is called

    config.setProperty("vm.store_steps", cond ? "true" : "false");

    if (ss != null){
      ss.recordSteps(cond);
    }
  }

  public boolean hasToRecordPathOutput() {
    if (config.getBoolean("vm.path_output")){ // explicitly requested
      return true;
    } else {
      return jpf.getReporter().hasToReportOutput(); // implicilty required
    }
  }

  protected void notifyChoiceGeneratorRegistered (ChoiceGenerator<?>cg, ThreadInfo ti) {
    try {
      lastThreadInfo = ti;
      lastInstruction = ti.getPC();
      lastChoiceGenerator = cg;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].choiceGeneratorRegistered(this);
      }
      lastChoiceGenerator = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during choiceGeneratorRegistered() notification", t);
    }
  }

  protected void notifyChoiceGeneratorSet (ChoiceGenerator<?>cg) {
    try {
      lastChoiceGenerator = cg;
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].choiceGeneratorSet(this);
      }
      lastChoiceGenerator = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during choiceGeneratorSet() notification", t);
    }
  }

  protected void notifyChoiceGeneratorAdvanced (ChoiceGenerator<?>cg) {
    try {
      lastChoiceGenerator = cg;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].choiceGeneratorAdvanced(this);
      }
      lastChoiceGenerator = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during choiceGeneratorAdvanced() notification", t);
    }
  }

  protected void notifyChoiceGeneratorProcessed (ChoiceGenerator<?>cg) {
    try {
      lastChoiceGenerator = cg;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].choiceGeneratorProcessed(this);
      }
      lastChoiceGenerator = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during choiceGeneratorProcessed() notification", t);
    }
  }

  protected void notifyExecuteInstruction (ThreadInfo ti, Instruction insn) {
    try {
      lastThreadInfo = ti;
      nextInstruction = insn;
      lastInstruction = insn; // <2do> debatable - we need to revisit the whole last... business (see header)

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].executeInstruction(this);
      }

      //nextInstruction = null;
      //lastInstruction = null;
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during executeInstruction() notification", t);
    }
  }

  protected void notifyInstructionExecuted (ThreadInfo ti, Instruction insn, Instruction nextInsn) {
    try {
      lastThreadInfo = ti;
      lastInstruction = insn;
      nextInstruction = nextInsn;

      //listener.instructionExecuted(this);
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].instructionExecuted(this);
      }

      //nextInstruction = null;
      //lastInstruction = null;
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during instructionExecuted() notification", t);
    }
  }

  protected void notifyThreadStarted (ThreadInfo ti) {
    try {
      lastThreadInfo = ti;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadStarted(this);
      }
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadStarted() notification", t);
    }
  }

  // NOTE: the supplied ThreadInfo does NOT have to be the running thread, as this
  // notification can occur as a result of a lock operation in the current thread
  protected void notifyThreadBlocked (ThreadInfo ti) {
    try {
      lastThreadInfo = ti;
      lastElementInfo = ti.getLockObject();

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadBlocked(this);
      }
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadBlocked() notification", t);
    }
  }

  protected void notifyThreadWaiting (ThreadInfo ti) {
    try {
      lastThreadInfo = ti;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadWaiting(this);
      }
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadWaiting() notification", t);
    }
  }

  protected void notifyThreadNotified (ThreadInfo ti) {
    try {
      lastThreadInfo = ti;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadNotified(this);
      }
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadNotified() notification", t);
    }
  }

  protected void notifyThreadInterrupted (ThreadInfo ti) {
    try {
      lastThreadInfo = ti;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadInterrupted(this);
      }
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadInterrupted() notification", t);
    }
  }

  protected void notifyThreadTerminated (ThreadInfo ti) {
    try {
      lastThreadInfo = ti;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadTerminated(this);
      }
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadTerminated() notification", t);
    }
  }

  protected void notifyThreadScheduled (ThreadInfo ti) {
    try {
      lastThreadInfo = ti;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].threadScheduled(this);
      }
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during threadScheduled() notification", t);
    }
  }
  
  protected void notifyLoadClass (ClassFile cf){
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].loadClass(this, cf);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during classLoaded() notification", t);
    }    
  }

  protected void notifyClassLoaded(ClassInfo ci) {
    try {
      lastClassInfo = ci;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].classLoaded(this);
      }
      //lastClassInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during classLoaded() notification", t);
    }
  }

  protected void notifyObjectCreated(ThreadInfo ti, ElementInfo ei) {
    try {
      lastThreadInfo = ti;
      lastElementInfo = ei;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectCreated(this);
      }

      //lastElementInfo = null;
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectCreated() notification", t);
    }
  }

  protected void notifyObjectReleased(ElementInfo ei) {
    try {
      lastElementInfo = ei;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectReleased(this);
      }
      //lastElementInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectReleased() notification", t);
    }
  }

  protected void notifyObjectLocked(ThreadInfo ti, ElementInfo ei) {
    try {
      lastThreadInfo = ti;
      lastElementInfo = ei;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectLocked(this);
      }

      //lastElementInfo = null;
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectLocked() notification", t);
    }
  }

  protected void notifyObjectUnlocked(ThreadInfo ti, ElementInfo ei) {
    try {
      lastThreadInfo = ti;
      lastElementInfo = ei;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectUnlocked(this);
      }

      //lastElementInfo = null;
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectUnlocked() notification", t);
    }
  }

  protected void notifyObjectWait(ThreadInfo ti, ElementInfo ei) {
    try {
      lastThreadInfo = ti;
      lastElementInfo = ei;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectWait(this);
      }

      //lastElementInfo = null;
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectWait() notification", t);
    }
  }

  protected void notifyObjectNotifies(ThreadInfo ti, ElementInfo ei) {
    try {
      lastThreadInfo = ti;
      lastElementInfo = ei;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectNotify(this);
      }

      //lastElementInfo = null;
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectNotifies() notification", t);
    }
  }

  protected void notifyObjectNotifiesAll(ThreadInfo ti, ElementInfo ei) {
    try {
      lastThreadInfo = ti;
      lastElementInfo = ei;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].objectNotifyAll(this);
      }

      //lastElementInfo = null;
      //lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during objectNotifiesAll() notification", t);
    }
  }

  protected void notifyGCBegin() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].gcBegin(this);
      }

    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during gcBegin() notification", t);
    }
  }

  protected void notifyGCEnd() {
    try {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].gcEnd(this);
      }
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during gcEnd() notification", t);
    }
  }

  protected void notifyExceptionThrown(ThreadInfo ti, ElementInfo ei) {
  	  
	  try {
      lastThreadInfo = ti;
      lastElementInfo = ei;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].exceptionThrown(this);
      }

      lastElementInfo = null;
      lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during exceptionThrown() notification", t);
    }
  }

  protected void notifyExceptionBailout(ThreadInfo ti) {
    try {
      lastThreadInfo = ti;
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].exceptionBailout(this);
      }
      lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during exceptionBailout() notification", t);
    }
  }

  protected void notifyExceptionHandled(ThreadInfo ti) {
    try {
      lastThreadInfo = ti;
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].exceptionHandled(this);
      }
      lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during exceptionHandled() notification", t);
    }
  }

  protected void notifyMethodEntered(ThreadInfo ti, MethodInfo mi) {
    try {
      lastThreadInfo = ti;
      lastMethodInfo = mi;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].methodEntered(this);
      }
      lastMethodInfo = null;
      lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during methodEntered() notification", t);
    }
  }

  protected void notifyMethodExited(ThreadInfo ti, MethodInfo mi) {
    try {
      lastThreadInfo = ti;
      lastMethodInfo = mi;

      for (int i = 0; i < listeners.length; i++) {
        listeners[i].methodExited(this);
      }
      lastMethodInfo = null;
      lastThreadInfo = null;
    } catch (UncaughtException x) {
      throw x;
    } catch (JPF.ExitException x) {
      throw x;
    } catch (Throwable t) {
      throw new JPFListenerException("exception during methodExited() notification", t);
    }
  }


  // VMListener acquisition
  public int getThreadNumber () {
    if (lastThreadInfo != null) {
      return lastThreadInfo.getId();
    } else {
      return -1;
    }
  }

  // VMListener acquisition
  public String getThreadName () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();

    return ti.getName();
  }

  // VMListener acquisition
  Instruction getInstruction () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    return ti.getPC();
  }

  public int getAliveThreadCount () {
    return getThreadList().getLiveThreadCount();
  }

  public ExceptionInfo getPendingException () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();

    if (ti != null){
      return ti.getPendingException();
    } else {
      return null;
    }
  }


  public Step getLastStep () {
    Transition trail = ss.getTrail();
    if (trail != null) {
      return trail.getLastStep();
    }

    return null;
  }

  public Transition getLastTransition () {
    if (path.size() == 0) {
      return null;
    }
    return path.get(path.size() - 1);
  }

  /**
   * answer the ClassInfo that was loaded most recently
   * part of the VMListener state acqusition (only valid from inside of
   * notification)
   */
  public ClassInfo getLastClassInfo () {
    return lastClassInfo;
  }

  /**
   * answer the ThreadInfo that was most recently started or finished
   * part of the VMListener state acqusition (only valid from inside of
   * notification)
   */
  public ThreadInfo getLastThreadInfo () {
    return lastThreadInfo;
  }

  /**
   * answer the MethodInfo that was most recently entered or exited (only
   * valid from inside notification)
   */
  public MethodInfo getLastMethodInfo () {
    return lastMethodInfo;
  }

  /**
   * answer the last executed Instruction
   * part of the VMListener state acqusition (only valid from inside of
   * notification)
   */
  public Instruction getLastInstruction () {
    return lastInstruction;
  }

  /**
   * answer the next Instruction to execute in the current thread
   * part of the VMListener state acqusition (only valid from inside of
   * notification)
   */
  public Instruction getNextInstruction () {
    return nextInstruction;
  }

  /**
   * answer the Object that was most recently created or collected
   * part of the VMListener state acqusition (only valid from inside of
   * object related notification)
   *
   * NOTE - this is currently not set for instructionExecuted notifications
   */
  public ElementInfo getLastElementInfo () {
    return lastElementInfo;
  }

  /**
   * return the most recently used CoiceGenerator
   */
  public ChoiceGenerator<?>getLastChoiceGenerator () {
    return lastChoiceGenerator;
  }

  /**
   * answer the ClassInfo that was loaded most recently
   * part of the VMListener state acquisition
   */
  public ClassInfo getClassInfo () {
    return lastClassInfo;
  }

  public ClassInfo getClassInfo (int objref) {
    if (objref != MJIEnv.NULL) {
      return getElementInfo(objref).getClassInfo();
    } else {
      return null;
    }
  }

  public String getMainClassName () {
    return mainClassName;
  }

  public ClassInfo getMainClassInfo () {
    return ClassInfo.getResolvedClassInfo(mainClassName);
  }

  public String[] getArgs () {
    return args;
  }

  /**
   * NOTE: only use this locally, since the path is getting modified by the VM
   *
   * The path only contains all states when queried from a stateAdvanced() notification.
   * If this is called from an instructionExecuted() (or other VMListener), and you need
   * the ongoing transition in it, you have to call updatePath() first
   */
  public Path getPath () {
    return path;
  }

  /**
   * this is the ongoing transition. Note that it is not yet stored in the path
   * if this is called from a VMListener notification
   */
  public Transition getCurrentTransition() {
    return ss.getTrail();
  }

  /**
   * use that one if you have to store the path for subsequent use
   *
   * NOTE: without a prior call to updatePath(), this does NOT contain the
   * ongoing transition. See getPath() for usage from a VMListener
   */
  public Path getClonedPath () {
    return path.clone();
  }

  public int getPathLength () {
    return path.size();
  }


  public int getRunnableThreadCount () {
    return ss.getRunnableThreadCount();
  }

  public ThreadList getThreadList () {
    return getKernelState().getThreadList();
  }
  
  /**
   * Bundles up the state of the system for export
   */
  public RestorableVMState getRestorableState () {
    return new RestorableVMState(this);
  }

  /**
   * Gets the system state.
   */
  public SystemState getSystemState () {
    return ss;
  }

  public KernelState getKernelState () {
    return ss.getKernelState();
  }

  public void kernelStateChanged(){
    ss.getKernelState().changed();
  }
  
  public Config getConfig() {
    return config;
  }

  public SchedulerFactory getSchedulerFactory(){
    return ss.getSchedulerFactory();
  }

  public Backtracker getBacktracker() {
    return backtracker;
  }

  @SuppressWarnings("unchecked")
  public <T> StateRestorer<T> getRestorer() {
    if (restorer == null) {
      if (serializer instanceof StateRestorer) {
        restorer = (StateRestorer<?>) serializer;
      } else if (stateSet instanceof StateRestorer) {
        restorer = (StateRestorer<?>) stateSet;
      } else {
        // config read only if serializer is not also a restorer
        restorer = config.getInstance("vm.restorer.class", StateRestorer.class);
      }
      restorer.attach(this);
    }

    return (StateRestorer<T>) restorer;
  }

  public StateSerializer getSerializer() {
    if (serializer == null) {
      serializer = config.getEssentialInstance("vm.serializer.class",
                                      StateSerializer.class);
      serializer.attach(this);
    }
    return serializer;
  }

  public void setSerializer (StateSerializer newSerializer){
    serializer = newSerializer;
    serializer.attach(this);
  }
  
  /**
   * Returns the stateSet if states are being matched.
   */
  public StateSet getStateSet() {
    return stateSet;
  }

  /**
   * return the last registered SystemState's ChoiceGenerator object
   * NOTE: there might be more than one ChoiceGenerator associated with the
   * current transition (ChoiceGenerators can be cascaded)
   */
  public ChoiceGenerator<?> getChoiceGenerator () {
    return ss.getChoiceGenerator();
  }

  public ChoiceGenerator<?> getNextChoiceGenerator() {
    return ss.getNextChoiceGenerator();
  }

  public boolean setNextChoiceGenerator (ChoiceGenerator<?> cg){
    return ss.setNextChoiceGenerator(cg);
  }
  
  
  /**
   * return the latest registered ChoiceGenerator used in this transition
   * that matches the provided 'id' and is of 'cgType'.
   * 
   * This should be the tiMain getter for clients that are cascade aware
   */
  public <T extends ChoiceGenerator<?>> T getCurrentChoiceGenerator (String id, Class<T> cgType) {
    return ss.getCurrentChoiceGenerator(id,cgType);
  }

  /**
   * returns all ChoiceGenerators in current path
   */
  public ChoiceGenerator<?>[] getChoiceGenerators() {
    return ss.getChoiceGenerators();
  }

  public <T extends ChoiceGenerator<?>> T[] getChoiceGeneratorsOfType (Class<T> cgType) {
    return ss.getChoiceGeneratorsOfType(cgType);
  }

  public <T extends ChoiceGenerator<?>> T getLastChoiceGeneratorOfType (Class<T> cgType){
    return ss.getLastChoiceGeneratorOfType(cgType);
  }
  
  public StaticElementInfo getClassReference (String name) {
    return ss.ks.statics.get(name);
  }

  public void print (String s) {
    if (treeOutput) {
      System.out.print(s);
    }

    if (pathOutput) {
      appendOutput(s);
    }
  }

  public void println (String s) {
    if (treeOutput) {
      if (indentOutput){
        StringBuilder indent = new StringBuilder();
        int i;
        for (i = 0;i<=path.size();i++) {
          indent.append('|').append(i);
        }
        indent.append("|").append(s);
        System.out.println(indent);
      }
      else {
        System.out.println(s);
      }
    }

    if (pathOutput) {
      appendOutput(s);
      appendOutput('\n');
    }
  }

  public void print (boolean b) {
    if (treeOutput) {
      System.out.print(b);
    }

    if (pathOutput) {
      appendOutput(Boolean.toString(b));
    }
  }

  public void print (char c) {
    if (treeOutput) {
      System.out.print(c);
    }

    if (pathOutput) {
      appendOutput(c);
    }
  }

  public void print (int i) {
    if (treeOutput) {
      System.out.print(i);
    }

    if (pathOutput) {
      appendOutput(Integer.toString(i));
    }
  }

  public void print (long l) {
    if (treeOutput) {
      System.out.print(l);
    }

    if (pathOutput) {
      appendOutput(Long.toString(l));
    }
  }

  public void print (double d) {
    if (treeOutput) {
      System.out.print(d);
    }

    if (pathOutput) {
      appendOutput(Double.toString(d));
    }
  }

  public void print (float f) {
    if (treeOutput) {
      System.out.print(f);
    }

    if (pathOutput) {
      appendOutput(Float.toString(f));
    }
  }

  public void println () {
    if (treeOutput) {
      System.out.println();
    }

    if (pathOutput) {
      appendOutput('\n');
    }
  }


  void appendOutput (String s) {
    if (out == null) {
      out = new StringBuilder();
    }
    out.append(s);
  }

  void appendOutput (char c) {
    if (out == null) {
      out = new StringBuilder();
    }
    out.append(c);
  }

  /**
   * get the pending output (not yet stored in the path)
   */
  public String getPendingOutput() {
    if (out != null && out.length() > 0){
      return out.toString();
    } else {
      return null;
    }
  }
  
  /**
   * this is here so that we can intercept it in subclassed VMs
   */
  public Instruction handleException (ThreadInfo ti, int xObjRef){
    ti = null;        // Get rid of IDE warning
    xObjRef = 0;
    return null;
  }

  public void storeTrace (String fileName, String comment, boolean verbose) {
    ChoicePoint.storeTrace(fileName, mainClassName, args, comment,
                           ss.getChoiceGenerators(), verbose);
  }

  public void storePathOutput () {
    pathOutput = true;
  }

  public ThreadInfo[] getLiveThreads () {
    return getThreadList().getThreads();
  }

  /**
   * print call stacks of all live threads
   * this is also used for debugging purposes, so we can't move it to the Reporter system
   * (it's also using a bit too many internals for that)
   */
  public void printLiveThreadStatus (PrintWriter pw) {
    int nThreads = ss.getThreadCount();
    ThreadInfo[] threads = getThreadList().getThreads();
    int n=0;

    for (int i = 0; i < nThreads; i++) {
      ThreadInfo ti = threads[i];

      if (ti.getStackDepth() > 0){
        n++;
        //pw.print("Thread: ");
        //pw.print(tiMain.getName());
        pw.println(ti.getStateDescription());

        List<ElementInfo> locks = ti.getLockedObjects();
        if (!locks.isEmpty()) {
          pw.print("  owned locks:");
          boolean first = true;
          for (ElementInfo e : locks) {
            if (first) {
              first = false;
            } else {
              pw.print(",");
            }
            pw.print(e);
          }
          pw.println();
        }

        ElementInfo ei = ti.getLockObject();
        if (ei != null) {
          if (ti.getState() == ThreadInfo.State.WAITING) {
            pw.print( "  waiting on: ");
          } else {
            pw.print( "  blocked on: ");
          }
          pw.println(ei);
        }

        pw.println("  call stack:");
        for (StackFrame frame : ti){
          if (!frame.isDirectCallFrame()) {
            pw.print("\tat ");
            pw.println(frame.getStackTraceInfo());
          }
        }

        pw.println();
      }
    }

    if (n==0) {
      pw.println("no live threads");
    }
  }

  // just a debugging aid
  public void dumpThreadStates () {
    java.io.PrintWriter pw = new java.io.PrintWriter(System.out, true);
    printLiveThreadStatus(pw);
    pw.flush();
  }

  /**
   * Moves one step backward. This method and forward() are the main methods
   * used by the search object.
   * Note this is called with the state that caused the backtrack still being on
   * the stack, so we have to remove that one first (i.e. popping two states
   * and restoring the second one)
   */
  public boolean backtrack () {
    transitionOccurred = false;

    boolean success = backtracker.backtrack();
    if (success) {
      if (CHECK_CONSISTENCY) checkConsistency(false);
      
      // restore the path
      path.removeLast();
      lastTrailInfo = path.getLast();

      return true;
      
    } else {
      return false;
    }
  }

  /**
   * store the current SystemState's Trail in our path, after updating it
   * with whatever annotations the JVM wants to add.
   * This is supposed to be called after each transition we want to keep
   */
  public void updatePath () {
    Transition t = ss.getTrail();
    Transition tLast = path.getLast();

    // NOTE: don't add the transition twice, this is public and might get called
    // from listeners, so the transition object might get changed

    if (tLast != t) {
      // <2do> we should probably store the output directly in the TrailInfo,
      // but this might not be our only annotation in the future

      // did we have output during the last transition? If yes, add it
      if ((out != null) && (out.length() > 0)) {
        t.setOutput( out.toString());
        out.setLength(0);
      }

      path.add(t);
    }
  }

  /**
   * advance the program state
   *
   * forward() and backtrack() are the two primary interfaces towards the Search
   * driver. note that the caller still has to check if there is a next state,
   * and if the executed instruction sequence led into a new or already visited state
   *
   * @return 'true' if there was an un-executed sequence out of the current state,
   * 'false' if it was completely explored
   *
   */
  public boolean forward () {

    // the reason we split up CG initialization and transition execution
    // is that program state storage is not required if the CG initialization
    // does not produce a new choice since we have to backtrack in that case
    // anyways. This can be caused by complete enumeration of CGs and/or by
    // CG listener intervention (i.e. not just after backtracking). For a large
    // number of matched or end states and ignored transitions this can be a
    // huge saving.
    // The downside is that CG notifications are NOT allowed anymore to change the
    // KernelState (modify fields or thread states) since those changes would
    // happen before storing the KernelState, and hence would make backtracking
    // inconsistent. This is advisable anyways since all program state changes
    // should take place during transitions, but the real snag is that this
    // cannot be easily enforced.

    // actually, it hasn't occurred yet, but will
    transitionOccurred = ss.initializeNextTransition(this);
    
    if (transitionOccurred){
      if (CHECK_CONSISTENCY) {
        checkConsistency(true); // don't push an inconsistent state
      }

      backtracker.pushKernelState();

      // cache this before we execute (and increment) the next insn(s)
      lastTrailInfo = path.getLast();

      try {
        ss.executeNextTransition(jvm);

      } catch (UncaughtException e) {
        // we don't pass this up since it means there were insns executed and we are
        // in a consistent state
      } // every other exception goes upwards

      backtracker.pushSystemState();
      updatePath();

      if (!isIgnoredState()) {
        // if this is ignored we are going to backtrack anyways
        // matching states out of ignored transitions is also not a good idea
        // because this transition is usually incomplete

        if (runGc && !hasPendingException()) {
          ss.gcIfNeeded();
        }

        if (stateSet != null) {
          newStateId = stateSet.size();
          int id = stateSet.addCurrent();
          ss.setId(id);

        } else { // this is 'state-less' model checking, i.e. we don't match states
          ss.setId(++newStateId); // but we still should have states numbered in case listeners use the id
        }
      }
      
      return true;

    } else {
    	
      return false;  // no transition occurred
    }
  }

  /**
   * Prints the current stack trace. Just for debugging purposes
   */
  public void printCurrentStackTrace () {
    ThreadInfo th = ThreadInfo.getCurrentThread();

    if (th != null) {
      th.printStackTrace();
    }
  }


  public void restoreState (RestorableVMState state) {
    if (state.path == null) {
      throw new JPFException("tried to restore partial VMState: " + state);
    }
    backtracker.restoreState(state.getBkState());
    path = state.path.clone();
  }

  public void activateGC () {
    ss.activateGC();
  }


  //--- various state attribute getters and setters (mostly forwarding to SystemState)

  public void retainStateAttributes (boolean isRetained){
    ss.retainAttributes(isRetained);
  }

  public void forceState () {
    ss.setForced(true);
  }

  /**
   * override the state matching - ignore this state, no matter if we changed
   * the heap or stacks.
   * use this with care, since it prunes whole search subtrees
   */
  public void ignoreState (boolean cond) {
    ss.setIgnored(cond);
  }

  public void ignoreState(){
    ignoreState(true);
  }

  /**
   * imperatively break the transition to enable state matching
   */
  public void breakTransition () {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    ti.breakTransition();
  }

  public boolean transitionOccurred(){
    return transitionOccurred;
  }

  /**
   * answers if the current state already has been visited. This is mainly
   * used by the searches (to control backtracking), but could also be useful
   * for observers to build up search graphs (based on the state ids)
   *
   * this returns true if no state has been produced yet, and false if
   * no transition occurred after a forward call
   */
  public boolean isNewState() {

    if (!transitionOccurred){
      return false;
    }

    if (stateSet != null) {
      if (ss.isForced()){
        return true;
      } else if (ss.isIgnored()){
        return false;
      } else {
        return (newStateId == ss.getId());
      }

    } else { // stateless model checking - each transition leads to a new state
      return true;
    }
  }

  public boolean isEndState () {
    // note this uses 'alive', not 'runnable', hence isEndStateProperty won't
    // catch deadlocks - but that would be NoDeadlockProperty anyway
    return ss.isEndState();
  }

  public boolean isVisitedState(){
    return !isNewState();
  }

  public boolean isIgnoredState(){
    return ss.isIgnored();
  }

  public boolean isInterestingState () {
    return ss.isInteresting();
  }

  public boolean isBoringState () {
    return ss.isBoring();
  }

  public boolean hasPendingException () {
    return (getPendingException() != null);
  }

  public boolean isDeadlocked () {
    return ss.isDeadlocked();
  }

  public boolean isTerminated () {
    return ss.ks.isTerminated();
  }

  public Exception getException () {
    return ss.getUncaughtException();
  }



  /**
   * get the numeric id for the current state
   * Note: this can be called several times (by the search and observers) for
   * every forward()/backtrack(), so we want to cache things a bit
   */
  public int getStateId() {
    return ss.getId();
  }

  public int getStateCount() {
    return newStateId;
  }


  /**
   * <2do> this is a band aid to bundle all these legacy reference chains
   * from JPFs past. The goal is to replace them with proper accessors (normally
   * through ThreadInfo, MJIEnv or JVM, which all act as facades) wherever possible,
   * and use JVM.getVM() where there is no access to such a facade. Once this
   * has been completed, we can start refactoring the users of JVM.getVM() to
   * get access to a suitable facade. 
   */
  public static JVM getVM () {
    return jvm;
  }

  /**
   * pushClinit all our static fields. Called from <clinit> and reset
   */
  static void initStaticFields () {
    error_id = 0;
  }

  public Heap getHeap() {
    return ss.getHeap();
  }

  public ElementInfo getElementInfo(int objref){
    return ss.getHeap().get(objref);
  }

  public ThreadInfo getCurrentThread () {
    return ThreadInfo.currentThread;
  }

  ThreadInfo[] getRunnableThreads(){
    return getThreadList().getRunnableThreads();
  }
  
  public boolean hasOtherRunnablesThan (ThreadInfo ti){
    return getThreadList().hasOtherRunnablesThan(ti);
  }

  public boolean hasOtherNonDaemonRunnablesThan (ThreadInfo ti){
    return getThreadList().hasOtherNonDaemonRunnablesThan(ti);
  }

  public boolean hasOnlyDaemonRunnablesOtherThan (ThreadInfo ti){
    return getThreadList().hasOnlyDaemonRunnablesOtherThan(ti);
  }
  
  public int registerThread (ThreadInfo ti){
    getKernelState().changed();
    return getThreadList().add(ti);    
  }
  
  
  public boolean isAtomic() {
    return ss.isAtomic();
  }

  /**
   * same for "loaded classes", but be advised it will probably go away at some point
   */
  public StaticArea getStaticArea () {
    return ss.ks.statics;
  }

    
  /**
   * <2do> this is where we will hook in a better time model
   */
  public long currentTimeMillis () {
    return timeModel.currentTimeMillis();
  }

  /**
   * <2do> this is where we will hook in a better time model
   */
  public long nanoTime() {
    return timeModel.nanoTime();
  }

  public void resetNextCG() {
    if (ss.nextCg != null) {
      ss.nextCg.reset();
    }
  }
  
  /**
   * only for debugging, this is expensive
   *
   * If this is a store (forward) this is called before the state is stored.
   *
   * If this is a restore (visited forward or backtrack), this is called after
   * the state got restored
   */
  public void checkConsistency(boolean isStateStore) {
    getThreadList().checkConsistency( isStateStore);
    getHeap().checkConsistency( isStateStore);
  }
}
