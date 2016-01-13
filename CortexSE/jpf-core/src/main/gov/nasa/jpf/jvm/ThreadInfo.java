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
import gov.nasa.jpf.jvm.bytecode.EXECUTENATIVE;
import gov.nasa.jpf.jvm.bytecode.INVOKESTATIC;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.jvm.choice.BreakGenerator;
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.ObjectList;
import gov.nasa.jpf.util.StringSetMatcher;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Represents a thread. It contains the state of the thread, static
 * information about the thread, and the stack frames.
 * Race detection and lock order also store some information
 * in this data structure.
 *
 * Note that we preserve identities according to their associated java.lang.Thread object
 * (objRef). This esp. means along the same path, a ThreadInfo reference
 * is kept invariant
 *
 * <2do> remove EXECUTENATIVE,INVOKESTATIC .bytecode dependencies
 */
public class ThreadInfo
     implements Iterable<StackFrame>, Comparable<ThreadInfo>, Cloneable, Restorable<ThreadInfo> {

  static JPFLogger log = JPF.getLogger("gov.nasa.jpf.jvm.ThreadInfo");

  //--- our internal thread states
  public enum State {
    NEW,  // means created but not yet started
    RUNNING,
    BLOCKED,  // waiting to acquire a lock
    UNBLOCKED,  // was BLOCKED but can acquire the lock now
    WAITING,  // waiting to be notified
    TIMEOUT_WAITING,
    NOTIFIED,  // was WAITING and got notified, but is still blocked
    INTERRUPTED,  // was WAITING and got interrupted
    TIMEDOUT,  // was TIMEOUT_WAITING and timed out
    TERMINATED,
    SLEEPING
  };

  static int threadInfoCount; // the number of ThreadInfos created

  static final int[] emptyRefArray = new int[0];

  static ThreadInfo currentThread;
  static ThreadInfo mainThread;

  static GlobalIdManager gidManager;

  protected class StackIterator implements Iterator<StackFrame> {
    StackFrame frame = top;

    public boolean hasNext() {
      return frame != null;
    }

    public StackFrame next() {
      if (frame != null){
        StackFrame ret = frame;
        frame = frame.getPrevious();
        return ret;

      } else {
        throw new NoSuchElementException();
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("can't remove StackFrames");
    }
  }

  protected class InvokedStackIterator extends StackIterator implements Iterator<StackFrame> {
    InvokedStackIterator() {
      frame = getLastInvokedStackFrame();
    }

    public StackFrame next() {
      if (frame != null){
        StackFrame ret = frame;
        frame = null;
        for (StackFrame f=ret.getPrevious(); f != null; f = f.getPrevious()){
          if (!f.isDirectCallFrame()){
            frame = f;
            break;
          }
        }
        return ret;

      } else {
        throw new NoSuchElementException();
      }
    }
  }

  protected static class UncaughtHandlerFrame extends DirectCallStackFrame {

    ExceptionInfo exceptionInfo;
    
    
    UncaughtHandlerFrame (ExceptionInfo xi, MethodInfo miHandler){
      // we need three operands: handlerObject, thread and exception object
      super(miHandler, 3, 0);
      
      exceptionInfo = xi;
    }
    
    ExceptionInfo getExceptionInfo(){
      return exceptionInfo;
    }
  }
    
  // transient, not state stored
  protected ExceptionInfo pendingException;

  // state managed data that is copy-on-first-write
  protected ThreadData threadData;

  
  //<2do> Hmm, why are these not in ThreadData?
  // the top stack frame
  protected StackFrame top = null;

  // the current stack depth (number of frames)
  protected int stackDepth;

  
  //--- the invariants
  
  // search global id, which is the basis for canonical order of threads
  protected int gid;
  
  protected int objRef; // the java.lang.Thread object reference
  protected ClassInfo ci; // the classinfo associated with the thread object
  protected int targetRef; // the associated java.lang.Runnable
  

  // which attributes are stored/restored
  static final int   ATTR_STORE_MASK = 0x0000ffff;

  //--- the transient (un(re)stored) attributes
  static final int ATTR_DATA_CHANGED       = 0x10000;
  static final int ATTR_STACK_CHANGED      = 0x20000;
  static final int ATTR_ATTRIBUTE_CHANGED  = 0x80000;


  //--- state stored/restored part
  
  // this is a typical "orthogonal" thread state we have to remember, but
  // that should not affect any locking, blocking, notifying or such
  static final int ATTR_STOPPED = 0x0001;

  //--- change sets
  static final int ATTR_ANY_CHANGED = (ATTR_DATA_CHANGED | ATTR_STACK_CHANGED | ATTR_ATTRIBUTE_CHANGED);

  static final int ATTR_SET_STOPPED = (ATTR_STOPPED | ATTR_ATTRIBUTE_CHANGED);

  protected int attributes;

  /** counter for executed instructions along current transition */
  protected int executedInstructions;

  /** shall we skip the next insn */
  protected boolean skipInstruction;

  /** store the last executed insn in the path */
  protected boolean logInstruction;


  /** the last returned direct call frame */
  protected StackFrame returnedDirectCall;

  /** the next insn to execute (null prior to execution) */
  protected Instruction nextPc;

  /**
   * not so nice we cross-couple the NativePeers with ThreadInfo,
   * but to carry on with the JNI analogy, a MJIEnv is clearly
   * owned by a thread (even though we don't have much ThreadInfo
   * state dependency in here (yet), hence the simplistic init)
   */
  MJIEnv env;

  /**
   * the VM we are running on. Bad backlink, but then again, we can't really
   * test a ThreadInfo outside a VM context anyways.
   * <2do> If we keep 'list' as a field, 'vm' might be moved over there
   * (all threads in the list share the same VM)
   */
  JVM vm;

  /**
   * !! this is a volatile object, i.e. it has to be re-computed explicitly
   * !! after each backtrack (we don't want to duplicate state storage)
   * list of lock objects currently held by this thread.
   * unfortunately, we cannot organize this as a stack, since it might get
   * restored (from the heap) in random order
   */
  LinkedList<ElementInfo> lockedObjects;

  /**
   * !! this is also volatile -> has to be reset after backtrack
   * the reference of the object if this thread is blocked or waiting for
   */
  int lockRef = -1;


  Memento<ThreadInfo> cachedMemento; // cache for unchanged ThreadInfos


  static class TiMemento implements Memento<ThreadInfo> {
    // note that we don't have to store the invariants (id, objRef, runnableRef, ci)
    ThreadInfo ti;

    ThreadData threadData;
    StackFrame top;
    int stackDepth;
    int attributes;

    TiMemento (ThreadInfo ti){
      this.ti = ti;
      threadData = ti.threadData;  // no need to clone - it's copy on first write
      top = ti.top; // likewise
      stackDepth = ti.stackDepth; // we just copy this for efficiency reasons
      attributes = (ti.attributes & ATTR_STORE_MASK);

      for (StackFrame frame = top; frame != null && frame.hasChanged(); frame = frame.getPrevious()){
        frame.setChanged(false);
      }
      ti.markUnchanged();
    }

    public ThreadInfo restore(ThreadInfo ignored) {
      ti.resetVolatiles();

      ti.threadData = threadData;
      ti.top = top;
      ti.stackDepth = stackDepth;
      ti.attributes = attributes;

      ti.markUnchanged();

      return ti;
    }
  }


  // the following parameters are configurable. Would be nice if we could keep
  // them on a per-instance basis, but there are a few locations
  // (e.g. ThreadList) where we loose the init context, and it's questionable
  // if we want to change this at runtime, or POR might make sense on a per-thread
  // basis

  /** do we halt on each throw, i.e. don't look for an exception handler?
   * Useful to find empty handler blocks, or misusd exceptionHandlers
   */
  static StringSetMatcher haltOnThrow;

  /**
   * do we delegate to Thread.UncaughtExceptionHandlers (in case there is any
   * other than the standard ThreadGroup)
   */
  static boolean ignoreUncaughtHandlers;
  
  /**
   * do we go on if we return from an UncaughtExceptionHandler, or do we still
   * regard this as a NoUncaughtExceptionProperty violation
   */
  static boolean passUncaughtHandler;
  
  /** is on-the-fly partial order in effect? */
  static boolean porInEffect;

  /** do we treat access of fields referring to objects that are reachable
   * from different threads as boundary steps (i.e. starting a new Transition)?
   */
  static boolean porFieldBoundaries;

  /** detect field synchronization (find locks which are used to synchronize
   * field access - if we have viable candidates, and we find the locks taken,
   * we don't treat access of the corresponding field as a boundary step
   */
  static boolean porSyncDetection;

  /**
   * break the current transition after this number of instructions.
   * This is a safeguard against paths that won't break because potentially
   * shared fields are not yet accessed by a second thread (existence of such
   * paths is the downside of our access tracking). Note that we only break on
   * backjumps once this count gets exceeded, to give state matching a better
   * chance and avoid interference with the IdleLoop listener
   */
  static int maxTransitionLength;
  
  
  static boolean init (Config config) {
    currentThread = null;
    mainThread = null;
    gidManager = new GlobalIdManager();
    
    threadInfoCount = 0;

    String[] haltOnThrowSpecs = config.getStringArray("vm.halt_on_throw");
    if (haltOnThrowSpecs != null){
      haltOnThrow = new StringSetMatcher(haltOnThrowSpecs);
    }
    
    ignoreUncaughtHandlers = config.getBoolean( "vm.ignore_uncaught_handler", true);
    passUncaughtHandler = config.getBoolean( "vm.pass_uncaught_handler", true);
    porInEffect = config.getBoolean("vm.por");
    porFieldBoundaries = porInEffect && config.getBoolean("vm.por.field_boundaries");
    porSyncDetection = porInEffect && config.getBoolean("vm.por.sync_detection");
    
    maxTransitionLength = config.getInt("vm.max_transition_length", 5000);
    
    return true;
  }

  /**
   * <2do> this is going to be a configurable factory method
   */
  static ThreadInfo createThreadInfo (JVM vm, int objRef, int groupRef, int runnableRef, int nameRef, long stackSize) {
    return new ThreadInfo(vm, objRef, groupRef, runnableRef, nameRef, stackSize);
  }
  
  /**
   * Creates a new thread info. It is associated with the object
   * passed and sets the target object as well.
   */
  public ThreadInfo (JVM vm, int objRef, int groupRef, int runnableRef, int nameRef, long stackSize) {
    threadInfoCount++;

    gid = computeGlobalId(vm.getSystemState());
    
    this.objRef = objRef;
    targetRef = runnableRef;
    
    ElementInfo ei = vm.getElementInfo(objRef);
    ci = ei.getClassInfo();

    this.vm = vm;

    threadData = new ThreadData();
    threadData.state = State.NEW;
    threadData.priority = Thread.NORM_PRIORITY;
    threadData.isDaemon = false;
    threadData.lockCount = 0;
    threadData.suspendCount = 0;
    threadData.name = vm.getElementInfo(nameRef).asString();
    
    // this is nasty - 'priority', 'name', 'target' and 'group' are not taken
    // from the object, but set within the java.lang.Thread ctors

    top = null;
    stackDepth = 0;

    lockedObjects = new LinkedList<ElementInfo>();

    markUnchanged();
    attributes |= ATTR_DATA_CHANGED; 
    env = new MJIEnv(this);
    //threadInfos.set(objRef, this); // our ThreadInfo repository

    // there can only be one
    if (mainThread == null) {
      mainThread = this;
      currentThread = this;
    }
        
    // note that we have to register here so that subsequent native peer calls can use the objRef
    // to lookup the ThreadInfo. This is a bit premature since the thread is not runnable yet,
    // but chances are it will be started soon, so we don't waste another data structure to do the mapping
    vm.registerThread(this);    
    ei.setIntField("id", gid);
  }

  
  public Memento<ThreadInfo> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<ThreadInfo> getMemento(){
    return new TiMemento(this);
  }

  //--- cached mementos are only supposed to be accessed from the Restorer

  public Memento<ThreadInfo> getCachedMemento(){
    return cachedMemento;
  }

  public void setCachedMemento(Memento<ThreadInfo> memento){
    cachedMemento = memento;
  }

  public static ThreadInfo getMainThread () {
    return mainThread;
  }

  public static ThreadInfo getCurrentThread() {
    return currentThread;
  }


  public boolean isExecutingAtomically () {
    return vm.getSystemState().isAtomic();
  }

  public boolean holdsLock (ElementInfo ei) {
    return lockedObjects.contains(ei);
  }

  public JVM getVM () {
    return vm;
  }

  /**
   * answers if is this the first instruction within the current transition.
   * This is mostly used to tell the top- from the bottom-half of a native method
   * or Instruction.execute(), so that only one (the top half) registers the CG
   * 
   * This can be used in both pre- and post-exec notification listeners, although
   * the executedInstructions number is incremented before notifying
   * instructionExecuted()
   */
  public boolean isFirstStepInsn() {
    int nInsn = executedInstructions;
    return ( nInsn == 0 || (nextPc != null && nInsn == 1));
  }

  /**
   * get the number of instructions executed in the current transition. This
   * gets incremented after calling Instruction.execute(), i.e. before
   * notifying instructionExecuted() listeners
   */
  public int getExecutedInstructions(){
    return executedInstructions;
  }
  
  /**
   * to be used from methods called from listeners, to find out if we are in a
   * pre- or post-exec notification
   */
  public boolean isPreExec() {
    return (nextPc == null);
  }

  public boolean usePor () {
    return porInEffect;
  }

  public boolean usePorFieldBoundaries () {
    return porFieldBoundaries;
  }

  public boolean usePorSyncDetection () {
    return porSyncDetection;
  }


  //--- various thread state related methods

  /**
   * Updates the status of the thread.
   */
  public void setState (State newStatus) {
    State oldStatus = threadData.state;

    if (oldStatus != newStatus) {

      assert (oldStatus != State.TERMINATED) : "can't resurrect thread " + this + " to " + newStatus.name();

      threadDataClone().state = newStatus;

      switch (newStatus) {
      case NEW:
        break; // Hmm, shall we report a thread object creation?
      case RUNNING:
        // nothing. the notifyThreadStarted has to happen from
        // Thread.start(), since the thread could have been blocked
        // at the time with a sync run() method
       // assert lockRef == -1;
        break;
      case TERMINATED:
        vm.notifyThreadTerminated(this);
        break;
      case BLOCKED:
        assert lockRef != -1;
        vm.notifyThreadBlocked(this);
        break;
      case UNBLOCKED:
        assert lockRef == -1;
        break; // nothing to notify
      case WAITING:
        assert lockRef != -1;
        vm.notifyThreadWaiting(this);
        break;
      case INTERRUPTED:
        vm.notifyThreadInterrupted(this);
        break;
      case NOTIFIED:
        assert lockRef != -1;
        vm.notifyThreadNotified(this);
        break;
      }

      if (log.isLoggable(Level.FINE)){
        log.fine("setStatus of " + getName() + " from "
                 + oldStatus.name() + " to " + newStatus.name());
      }
    }
  }

  void setBlockedState (int objref) {
    
    State currentState = threadData.state;
    switch (currentState){
      case NEW:
      case RUNNING:
      case UNBLOCKED:
        lockRef = objref;
        setState(State.BLOCKED);
        break;

      default:
        assert false : "thread " + this + "can't be blocked in state: " + currentState.name();
    }
  }

  void setNotifiedState() {
    State currentState = threadData.state;
    switch (currentState){
      case BLOCKED:
      case INTERRUPTED: // too late, we are already interrupted
      case NOTIFIED:
        // can happen in a Thread.join()
        break;
      case WAITING:
      case TIMEOUT_WAITING:
        setState(State.NOTIFIED);
        break;

      default:
        assert false : "thread " + this + "can't be notified in state: " + currentState.name();
    }
  }

  /**
   * Returns the current status of the thread.
   */
  public State getState () {
    return threadData.state;
  }


  /**
   * Returns true if this thread is either RUNNING or UNBLOCKED
   */
  public boolean isRunnable () {
    if (threadData.suspendCount != 0)
      return false;

    switch (threadData.state) {
    case RUNNING:
    case UNBLOCKED:
      return true;
    case SLEEPING:
      return true;    // that's arguable, but since we don't model time we treat it like runnable
    case TIMEDOUT:
      return true;    // would have been set to blocked if it couldn't reacquire the lock
    default:
      return false;
    }
  }

  public boolean willBeRunnable () {
    if (threadData.suspendCount != 0)
      return false;

    switch (threadData.state) {
    case RUNNING:
    case UNBLOCKED:
      return true;
    case TIMEOUT_WAITING: // it's not yet, but it will be at the time it gets scheduled
    case SLEEPING:
      return true;
    default:
      return false;
    }
  }

  public boolean isNew () {
    return (threadData.state == State.NEW);
  }

  public boolean isTimeoutRunnable () {
    if (threadData.suspendCount != 0)
      return false;

    switch (threadData.state) {

    case RUNNING:
    case UNBLOCKED:
    case SLEEPING:
      return true;

    case TIMEOUT_WAITING:
      // depends on if we can re-acquire the lock
      //assert lockRef != -1 : "timeout waiting but no blocked object";
      if (lockRef != -1){
        ElementInfo ei = vm.getElementInfo(lockRef);
        return ei.canLock(this);
      } else {
        return true;
      }

    default:
      return false;
    }
  }

  public boolean isTimedOut() {
    return (threadData.state == State.TIMEDOUT);
  }

  public boolean isTimeoutWaiting() {
    return (threadData.state == State.TIMEOUT_WAITING);
  }

  public void setTimedOut() {
    setState(State.TIMEDOUT);
  }

  public void setTerminated() {
    setState(State.TERMINATED);
  }

  public void resetTimedOut() {
    // should probably check for TIMEDOUT
    setState(State.TIMEOUT_WAITING);
  }

  public void setSleeping() {
    setState(State.SLEEPING);
  }

  public boolean isSleeping(){
    return (threadData.state == State.SLEEPING);
  }

  public void setRunning() {
    setState(State.RUNNING);
  }

  public void setStopped(int throwableRef){
    if (isTerminated()){
      // no need to kill twice
      return;
    }

    attributes |= ATTR_SET_STOPPED;

    if (!hasBeenStarted()){
      // that one is easy - just remember the state so that a subsequent start()
      // does nothing
      return;
    }

    // for all other cases, we need to have a proper stopping Throwable that does not
    // fall victim to GC, and that does not cause NoUncaughtExcceptionsProperty violations
    if (throwableRef == MJIEnv.NULL){
      // if no throwable was provided (the normal case), throw a java.lang.ThreadDeath Error
      ClassInfo cix = ClassInfo.getInitializedClassInfo("java.lang.ThreadDeath", this);
      throwableRef = createException(cix, null, MJIEnv.NULL);
    }

    // now the tricky part - this thread is alive but might be blocked, notified
    // or waiting. In any case, exception action should not take place before
    // the thread becomes scheduled again, which
    // means we are not allowed to fiddle with its state in any way that changes
    // scheduling/locking behavior. On the other hand, if this is the currently
    // executing thread, take immediate action

    if (isCurrentThread()){ // we are suicidal
      if (isInNativeMethod()){
        // remember the exception to be thrown when we return from the native method
        env.throwException(throwableRef);
      } else {
        Instruction nextPc = throwException(throwableRef);
        setNextPC(nextPc);
      }

    } else { // this thread is not currently running, this is an external kill

      // remember there was a pending exception that has to be thrown the next
      // time this gets scheduled, and make sure the exception object does
      // not get GCed prematurely
      ElementInfo eit = getElementInfo(objRef);
      eit.setReferenceField("stopException", throwableRef);
    }
  }

  public boolean isCurrentThread(){
    return this == currentThread;
  }

  /**
   * An alive thread is anything but TERMINATED or NEW
   */
  public boolean isAlive () {
    State state = threadData.state;
    return (state != State.TERMINATED && state != State.NEW);
  }

  public boolean isWaiting () {
    State state = threadData.state;
    return (state == State.WAITING) || (state == State.TIMEOUT_WAITING);
  }

  public boolean isNotified () {
    return (threadData.state == State.NOTIFIED);
  }

  public boolean isUnblocked () {
    State state = threadData.state;
    return (state == State.UNBLOCKED) || (state == State.TIMEDOUT);
  }

  public boolean isBlocked () {
    return (threadData.state == State.BLOCKED);
  }

  public boolean isTerminated () {
    return (threadData.state == State.TERMINATED);
  }

  MethodInfo getExitMethod() {
    MethodInfo mi = getClassInfo().getMethod("exit()V", true);
    return mi;
  }

  public boolean isBlockedOrNotified() {
    State state = threadData.state;
    return (state == State.BLOCKED) || (state == State.NOTIFIED);
  }

  // this is just a state attribute
  public boolean isStopped() {
    return (attributes & ATTR_STOPPED) != 0;
  }

  public boolean isInNativeMethod(){
    return top != null && top.isNative();
  }

  public boolean hasBeenStarted(){
    return (threadData.state != State.NEW);
  }

  public String getStateName () {
    return threadData.getState().name();
  }


  public boolean getBooleanLocal (String lname) {
    return Types.intToBoolean(getLocalVariable(lname));
  }

  public boolean getBooleanLocal (int lindex) {
    return Types.intToBoolean(getLocalVariable(lindex));
  }

  public boolean getBooleanReturnValue () {
    return Types.intToBoolean(peek());
  }

  public byte getByteLocal (String lname) {
    return (byte) getLocalVariable(lname);
  }

  public byte getByteLocal (int lindex) {
    return (byte) getLocalVariable(lindex);
  }


  public byte getByteReturnValue () {
    return (byte) peek();
  }

  public Iterator<StackFrame> iterator () {
    return new StackIterator();
  }

  public Iterable<StackFrame> invokedStackFrames () {
    return new Iterable<StackFrame>() {
      public Iterator<StackFrame> iterator() {
        return new InvokedStackIterator();
      }
    };
  }

  /**
   * this returns a copy of the StackFrames in reverse order. Note this is
   * redundant because the frames are linked explicitly
   * @deprecated - use Iterable<StackFrame>
   */
  @Deprecated
  public List<StackFrame> getStack() {
    ArrayList<StackFrame> list = new ArrayList<StackFrame>(stackDepth);

    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      list.add(frame);
    }

    Collections.reverse(list);

    return list;
  }

  /**
   * returns StackFrames which have been entered through a corresponding
   * invoke instruction (in top first order)
   */
  public List<StackFrame> getInvokedStackFrames() {
    ArrayList<StackFrame> list = new ArrayList<StackFrame>(stackDepth);

    int i = stackDepth-1;
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.isDirectCallFrame()){
        list.add( frame);
      }
    }
    Collections.reverse(list);

    return list;
  }

  public List<StackFrame> getChangedStackFrames() {
    ArrayList<StackFrame> list = new ArrayList<StackFrame>();

    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.hasChanged()){
        list.add(frame);
      }
    }

    return list;
  }

  public int getStackDepth() {
    return stackDepth;
  }

  public StackFrame getCallerStackFrame (int offset){
    int n = offset;
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (n < 0){
        break;
      } else if (n == 0){
        return frame;
      }
      n--;
    }
    return null;
  }

  public StackFrame getLastInvokedStackFrame() {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.isDirectCallFrame()){
        return frame;
      }
    }

    return null;
  }

  public StackFrame getLastNonSyntheticStackFrame (){
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      if (!frame.isSynthetic()){
        return frame;
      }
    }

    return null;
  }

  /**
   * Returns the this pointer of the callee from the stack.
   */
  public int getCalleeThis (MethodInfo mi) {
    return top.getCalleeThis(mi);
  }

  /**
   * Returns the this pointer of the callee from the stack.
   */
  public int getCalleeThis (int size) {
    return top.getCalleeThis(size);
  }

  public ClassInfo getClassInfo (int objref) {
    return env.getClassInfo(objref);
  }

  public boolean isCalleeThis (ElementInfo r) {
    if (top == null || r == null) {
      return false;
    }

    Instruction pc = getPC();

    if (pc == null ||
        !(pc instanceof InvokeInstruction) ||
        pc instanceof INVOKESTATIC) {
      return false;
    }

    InvokeInstruction call = (InvokeInstruction) pc;

    return getCalleeThis(Types.getArgumentsSize(call.getInvokedMethodSignature()) + 1) == r.getObjectRef();
  }

  public char getCharLocal (String lname) {
    return (char) getLocalVariable(lname);
  }

  public char getCharLocal (int lindex) {
    return (char) getLocalVariable(lindex);
  }

  public char getCharReturnValue () {
    return (char) peek();
  }

  /**
   * Returns the class information.
   */
  public ClassInfo getClassInfo () {
    return ci;
  }

  public double getDoubleLocal (String lname) {
    return Types.longToDouble(getLongLocalVariable(lname));
  }

  public double getDoubleLocal (int lindex) {
    return Types.longToDouble(getLongLocalVariable(lindex));
  }

  public double getDoubleReturnValue () {
    return Types.longToDouble(longPeek());
  }

  public MJIEnv getEnv() {
    return env;
  }

  public float getFloatLocal (String lname) {
    return Types.intToFloat(getLocalVariable(lname));
  }

  public float getFloatLocal (int lindex) {
    return Types.intToFloat(getLocalVariable(lindex));
  }

  public float getFloatReturnValue () {
    return Types.intToFloat(peek());
  }

  public int getIntLocal (String lname) {
    return getLocalVariable(lname);
  }

  public int getIntLocal (int lindex) {
    return getLocalVariable(lindex);
  }

  public int getIntReturnValue () {
    return peek();
  }

  public boolean isInterrupted (boolean resetStatus) {
    ElementInfo ei = getElementInfo(getThreadObjectRef());
    boolean status =  ei.getBooleanField("interrupted");

    if (resetStatus && status) {
      ei.setBooleanField("interrupted", false);
    }

    return status;
  }

  /**
   * path local unique id for live threads. This is what we use for the
   * public java.lang.Thread.getId() that can be called from SUT code. It is
   * NOT what we use for our canonical root set
   */
  public int getId () {
    return gid;
  }

  /**
   * this is our internal, search global id that is used for the
   * canonical root set
   */
  public int getGlobalId(){
    return gid;
  }
  
  protected int computeGlobalId (SystemState ss){
    ThreadInfo tiExec = currentThread;
    Instruction insn = null;
    
    if (tiExec != null){
      insn = tiExec.getTopFrame().getPC();  
    }
        
    return gidManager.getNewId(ss, currentThread, insn);
  }
  
  /**
   * record what this thread is being blocked on.
   */
  void setLockRef (int objref) {
/**
    assert ((lockRef == -1) || (lockRef == objref)) :
      "attempt to overwrite lockRef: " + vm.getHeap().get(lockRef) +
      " with: " + vm.getHeap().get(objref);
**/
    lockRef = objref;
  }

  /**
   * thread is not blocked anymore
   * needs to be public since we have to use it from INVOKECLINIT (during call skipping)
   */
  public void resetLockRef () {
    lockRef = -1;
  }

  public int getLockRef() {
    return lockRef;
  }

  public ElementInfo getLockObject () {
    if (lockRef == -1) {
      return null;
    } else {
      return vm.getElementInfo(lockRef);
    }
  }

  /**
   * Returns the line number of the program counter of the top stack frame.
   */
  public int getLine () {
    if (top == null) {
      return -1;
    } else {
      return top.getLine();
    }
  }
  
  public LocalVarInfo[] getLocalVars() {
    return top.getLocalVars();
  }
  

  /**
   * Sets the value of a local variable.
   */
  public void setLocalVariable (int idx, int v, boolean ref) {
    topClone().setLocalVariable(idx, v, ref);
  }

  /**
   * Returns the value of a local variable.
   */
  public int getLocalVariable (int idx) {
    return top.getLocalVariable(idx);
  }

  /**
   * Gets the value of a local variable from its name.
   */
  public int getLocalVariable (String name) {
    return top.getLocalVariable(name);
  }

  /**
   * Checks if a local variable is a reference.
   */
  public boolean isLocalVariableRef (int idx) {
    return top.isLocalVariableRef(idx);
  }


  /**
   * Gets the type associated with a local variable.
   */
  public String getLocalVariableType (String name) {
    return top.getLocalVariableType(name);
  }

  
  //--- suspend/resume modeling
  // modeling this with a count is an approximation. In reality it behaves
  // rather like a race that /sometimes/ causes the resume to fail, but its
  // Ok if we overapproximate on the safe side, since suspend/resume is such
  // an inherently unsafe thing. What we *do* want to preserve faithfully is 
  // that locks held by the suspended thread are not released
  
  /**
   * set suspension status
   * @return true if thread was not suspended
   */
  public boolean suspend() {
    return threadDataClone().suspendCount++ == 0;
  }

  /**
   * unset suspension status
   * @return true if thread was suspended
   */
  public boolean resume() {
    return (threadData.suspendCount > 0) && (--threadDataClone().suspendCount == 0);
  }
  
  public boolean isSuspended() {
    return threadData.suspendCount > 0;
  }


  //--- locks
  
  /**
   * Sets the number of locks held at the time of a wait.
   */
  public void setLockCount (int l) {
    if (threadData.lockCount != l) {
      threadDataClone().lockCount = l;
    }
  }

  /**
   * Returns the number of locks in the last wait.
   */
  public int getLockCount () {
    return threadData.lockCount;
  }
  
  public List<ElementInfo> getLockedObjects () {
    return lockedObjects;
  }

  public int[] getLockedObjectReferences () {
    int nLocks = lockedObjects.size();
    if (nLocks > 0) {
      int[] a = new int[lockedObjects.size()];
      int i = 0;
      for (ElementInfo e : lockedObjects) {
        a[i++] = e.getObjectRef();
      }
      return a;

    } else {
      return emptyRefArray;
    }
  }

  public long getLongLocal (String lname) {
    return getLongLocalVariable(lname);
  }

  public long getLongLocal (int lindex) {
    return getLongLocalVariable(lindex);
  }

  /**
   * Sets the value of a long local variable.
   */
  public void setLongLocalVariable (int idx, long v) {
    topClone().setLongLocalVariable(idx, v);
  }

  /**
   * Returns the value of a long local variable.
   */
  public long getLongLocalVariable (int idx) {
    return top.getLongLocalVariable(idx);
  }

  /**
   * Gets the value of a long local variable from its name.
   */
  public long getLongLocalVariable (String name) {
    return top.getLongLocalVariable(name);
  }

  public long getLongReturnValue () {
    return longPeek();
  }

  /**
   * returns the current method in the top stack frame, which is always a
   * bytecode method (executed by JPF)
   */
  public MethodInfo getTopMethod () {
    if (top != null) {
      return top.getMethodInfo();
    } else {
      return null;
    }
  }

  /**
   * returns the currently executing MethodInfo, which can be a native/MJI method
   */
  public MethodInfo getMethod() {
    MethodInfo mi = vm.getLastMethodInfo();
    if (mi != null){
      return mi;
    } else {
      return getTopMethod();
    }
  }

  public boolean isInCtor () {
    // <2do> - hmm, if we don't do this the whole stack, we miss factored
    // init funcs
    MethodInfo mi = getMethod();
    if (mi != null) {
      return mi.isCtor();
    } else {
      return false;
    }
  }

  public boolean isCtorOnStack (int objRef){
    for (StackFrame f = top; f != null; f = f.getPrevious()){
      if (f.getThis() == objRef && f.getMethodInfo().isCtor()){
        return true;
      }
    }

    return false;
  }

  public boolean isClinitOnStack (ClassInfo ci){
    for (StackFrame f = top; f != null; f = f.getPrevious()){
      MethodInfo mi = f.getMethodInfo();
      if (mi.isClinit(ci)){
        return true;
      }
    }

    return false;
  }


  public String getName () {
    return threadData.name;
  }


  public ElementInfo getObjectLocal (String lname) {
    return vm.getElementInfo(getLocalVariable(lname));
  }

  public ElementInfo getObjectLocal (int lindex) {
    return vm.getElementInfo(getLocalVariable(lindex));
  }

  /**
   * Returns the object reference.
   */
  public int getThreadObjectRef () {
    return objRef;
  }

  public ElementInfo getThreadObject(){
    return getElementInfo(objRef);
  }

  public ElementInfo getObjectReturnValue () {
    return vm.getElementInfo(peek());
  }

  //------------------- attribute accessors
  
  //--- top single-slot operand attr accessors
  
  public boolean hasOperandAttr(){
    return top.hasOperandAttr();
  }  
  public boolean hasOperandAttr(Class<?> type){
    return top.hasOperandAttr(type);
  }
  
  /**
   * this returns all of them - use either if you know there will be only
   * one attribute at a time, or check/process result with ObjectList
   */
  public Object getOperandAttr () {
    return top.getOperandAttr();
  }
  
  /**
   * this replaces all of them - use only if you know 
   *  - there will be only one attribute at a time
   *  - you obtained the value you set by a previous getXAttr()
   *  - you constructed a multi value list with ObjectList.createList()
   */
  public void setOperandAttr (Object attr) {
    topClone().setOperandAttr(attr);
  }
  public void setOperandAttrNoClone (Object attr) {
    top.setOperandAttr(attr);
  }

  /**
   * this only returns the first attr of this type, there can be more
   * if you don't use client private types or the provided type is too general
   */
  public <T> T getOperandAttr (Class<T> attrType){
    return top.getOperandAttr(attrType);
  }
  public <T> T getNextOperandAttr (Class<T> attrType, Object prev){
    return top.getNextOperandAttr(attrType, prev);
  }
  public Iterator operandAttrIterator (){
    return top.operandAttrIterator();
  }
  public <T> Iterator<T> operandAttrIterator (Class<T> attrType){
    return top.operandAttrIterator(attrType);
  }
    
  public void addOperandAttr (Object attr) {
    topClone().addOperandAttr(attr);
  }
  public void addOperandAttrNoClone (Object attr) {
    top.addOperandAttr(attr);
  }

  public void removeOperandAttr (Object attr) {
    topClone().removeOperandAttr(attr);
  }
  public void removeOperandAttrNoClone (Object attr) {
    top.removeOperandAttr(attr);
  }
  
  public void replaceOperandAttr (Object oldAttr, Object newAttr) {
    topClone().replaceOperandAttr(oldAttr, newAttr);
  }
  public void replaceOperandAttrNoClone (Object oldAttr, Object newAttr) {
    top.replaceOperandAttr(oldAttr, newAttr);
  }
  

  //--- offset operand attr accessors
  
  public boolean hasOperandAttr(int opStackOffset){
    return top.hasOperandAttr(opStackOffset);
  }  
  public boolean hasOperandAttr(int opStackOffset, Class<?> type){
    return top.hasOperandAttr(opStackOffset, type);
  }
  
  /**
   * this returns all of them - use either if you know there will be only
   * one attribute at a time, or check/process result with ObjectList
   */
  public Object getOperandAttr (int opStackOffset) {
    return top.getOperandAttr(opStackOffset);
  }
  
  /**
   * this replaces all of them - use only if you know 
   *  - there will be only one attribute at a time
   *  - you obtained the value you set by a previous getXAttr()
   *  - you constructed a multi value list with ObjectList.createList()
   */
  public void setOperandAttr (int opStackOffset, Object attr) {
    topClone().setOperandAttr(opStackOffset, attr);
  }
  public void setOperandAttrNoClone (int opStackOffset, Object attr) {
    top.setOperandAttr(opStackOffset, attr);
  }

  /**
   * this only returns the first attr of this type, there can be more
   * if you don't use client private types or the provided type is too general
   */
  public <T> T getOperandAttr( int opStackOffset, Class<T> attrType){
    return top.getOperandAttr( opStackOffset, attrType);
  }
  public <T> T getNextOperandAttr( int opStackOffset, Class<T> attrType, Object prev){
    return top.getNextOperandAttr( opStackOffset, attrType, prev);
  }
  
  public ObjectList.Iterator operandAttrIterator (int opStackOffset){
    return top.operandAttrIterator(opStackOffset);
  }
  public <T> ObjectList.TypedIterator<T> operandAttrIterator (int opStackOffset, Class<T> attrType){
    return top.operandAttrIterator(opStackOffset, attrType);
  }
    
  public void addOperandAttr (int opStackOffset, Object attr) {
    topClone().addOperandAttr(opStackOffset,attr);
  }
  public void addOperandAttrNoClone (int opStackOffset, Object attr) {
    top.addOperandAttr(opStackOffset,attr);
  }

  public void removeOperandAttr (int opStackOffset, Object attr) {
    topClone().removeOperandAttr(opStackOffset,attr);
  }
  public void removeOperandAttrNoClone (int opStackOffset, Object attr) {
    top.removeOperandAttr(opStackOffset,attr);
  }
  
  public void replaceOperandAttr (int opStackOffset, Object oldAttr, Object newAttr) {
    topClone().replaceOperandAttr(opStackOffset,oldAttr, newAttr);
  }
  public void replaceOperandAttrNoClone (int opStackOffset, Object oldAttr, Object newAttr) {
    top.replaceOperandAttr(opStackOffset,oldAttr, newAttr);
  }
  
  
  //--- top double-slot operand attr accessors

  public boolean hasLongOperandAttr(){
    return top.hasLongOperandAttr();
  }  
  public boolean hasLongOperandAttr(Class<?> type){
    return top.hasLongOperandAttr(type);
  }
  
  /**
   * this returns all of them - use either if you know there will be only
   * one attribute at a time, or check/process result with ObjectList
   */  
  public Object getLongOperandAttr () {
    return top.getLongOperandAttr();
  }
  
  /**
   * this replaces all of them - use only if you know 
   *  - there will be only one attribute at a time
   *  - you obtained the value you set by a previous getXAttr()
   *  - you constructed a multi value list with ObjectList.createList()
   */
  public void setLongOperandAttr (Object attr) {
    topClone().setLongOperandAttr(attr);
  }
  public void setLongOperandAttrNoClone (Object attr) {
    top.setLongOperandAttr(attr);
  }
  
  /**
   * this only returns the first attr of this type, there can be more
   * if you don't use client private types or the provided type is too general
   */
  public <T> T getLongOperandAttr (Class<T> attrType){
    return top.getLongOperandAttr(attrType);
  }
  public <T> T getNextLongOperandAttr (Class<T> attrType, Object prev){
    return top.getNextLongOperandAttr(attrType, prev);
  }

  public ObjectList.Iterator longOperandAttrIterator (){
    return top.longOperandAttrIterator();
  }
  public <T> ObjectList.TypedIterator<T> longOperandAttrIterator (Class<T> attrType){
    return top.longOperandAttrIterator(attrType);
  }
    
  public void addLongOperandAttr (Object attr) {
    topClone().addLongOperandAttr(attr);
  }
  public void addLongOperandAttrNoClone (Object attr) {
    top.addLongOperandAttr(attr);
  }

  public void removeLongOperandAttr (Object attr) {
    topClone().removeLongOperandAttr(attr);
  }
  public void removeLongOperandAttrNoClone (Object attr) {
    top.removeLongOperandAttr(attr);
  }
  
  public void replaceLongOperandAttr (Object oldAttr, Object newAttr) {
    topClone().replaceLongOperandAttr(oldAttr, newAttr);
  }
  public void replaceLongOperandAttrNoClone (Object oldAttr, Object newAttr) {
    top.replaceLongOperandAttr(oldAttr, newAttr);
  }
  
  
  //--- local var attribute accessors

  public boolean hasLocalAttr(int localIndex){
    return top.hasLocalAttr(localIndex);
  }
  public boolean hasLocalAttr(int localIndex, Class<?> type){
    return top.hasLocalAttr(localIndex, type);
  }
  
  /**
   * this returns all of them - use either if you know there will be only
   * one attribute at a time, or check/process result with ObjectList
   */  
  public Object getLocalAttr (int localIndex) {
    return top.getLocalAttr(localIndex);
  }
  
  /**
   * this replaces all of them - use only if you know 
   *  - there will be only one attribute at a time
   *  - you obtained the value you set by a previous getXAttr()
   *  - you constructed a multi value list with ObjectList.createList()
   */
  public void setLocalAttr (int localIndex, Object a){
    topClone().setLocalAttr(localIndex, a);
  }
  public void setLocalAttrNoClone (int localIndex, Object a){
    top.setLocalAttr(localIndex, a);
  }
  
  /**
   * this only returns the first attr of this type, there can be more
   * if you don't use client private types or the provided type is too general
   */
  public <T> T getLocalAttr( int localIndex, Class<T> attrType){
    return top.getLocalAttr( localIndex, attrType);
  }
  public <T> T getNextLocalAttr( int localIndex, Class<T> attrType, Object prev){
    return top.getNextLocalAttr( localIndex, attrType, prev);
  }
  
  public ObjectList.Iterator localAttrIterator (int localIndex){
    return top.localAttrIterator(localIndex);
  }
  public <T> ObjectList.TypedIterator<T> localAttrIterator (int localIndex, Class<T> attrType){
    return top.localAttrIterator(localIndex, attrType);
  }
    
  public void addLocalAttr (int localIndex, Object attr) {
    topClone().addLocalAttr(localIndex,attr);
  }
  public void addLocalAttrNoClone (int localIndex, Object attr) {
    top.addLocalAttr(localIndex,attr);
  }

  public void removeLocalAttr (int localIndex, Object attr) {
    topClone().removeLocalAttr(localIndex,attr);
  }
  public void removeLocalAttrNoClone (int localIndex, Object attr) {
    top.removeLocalAttr(localIndex,attr);
  }
  
  public void replaceLocalAttr (int localIndex, Object oldAttr, Object newAttr) {
    topClone().replaceLocalAttr(localIndex,oldAttr, newAttr);
  }
  public void replaceLocalAttrNoClone (int localIndex, Object oldAttr, Object newAttr) {
    top.replaceLocalAttr(localIndex,oldAttr, newAttr);
  }
  

  // -- end attribute accessors --
  
  
  /**
   * Checks if the top operand is a reference.
   */
  public boolean isOperandRef () {
    return top.isOperandRef();
  }

  /**
   * Checks if an operand is a reference.
   */
  public boolean isOperandRef (int idx) {
    return top.isOperandRef(idx);
  }

  /**
   * Sets the program counter of the top stack frame.
   */
  public void setPC (Instruction pc) {
    topClone().setPC(pc);
  }

  public void advancePC () {
    topClone().advancePC();
  }

  /**
   * Returns the program counter of the top stack frame.
   */
  public Instruction getPC () {
    if (top != null) {
      return top.getPC();
    } else {
      return null;
    }
  }

  public Instruction getNextPC () {
    return nextPc;
  }

  public short getShortLocal (String lname) {
    return (short) getLocalVariable(lname);
  }

  public short getShortLocal (int lindex) {
    return (short) getLocalVariable(lindex);
  }

  public short getShortReturnValue () {
    return (short) peek();
  }

  /**
   * get the current stack trace of this thread
   * this is called during creation of a Throwable, hence we should skip
   * all throwable ctors in here
   * <2do> this is only a partial solution,since we don't catch exceptionHandlers
   * in Throwable ctors yet
   */
  public String getStackTrace () {
    StringBuilder sb = new StringBuilder(256);

    for (StackFrame sf = top; sf != null; sf = sf.getPrevious()){
      MethodInfo mi = sf.getMethodInfo();

      if (mi.isCtor()){
        ClassInfo ci = mi.getClassInfo();
        if (ci.isInstanceOf("java.lang.Throwable")) {
          continue;
        }
      }

      sb.append("\tat ");
      sb.append(sf.getStackTraceInfo());
      sb.append('\n');
    }

    return sb.toString();
  }


  /**
   * Returns the information necessary to store.
   *
   * <2do> pcm - not clear to me how lower stack frames can contribute to
   * a different threadinfo state hash - only the current one can be changed
   * by the executing method
   */
  public void dumpStoringData (IntVector v) {
    v = null;  // Get rid of IDE warnings
  }

  public String getStringLocal (String lname) {
    return vm.getElementInfo(getLocalVariable(lname)).asString();
  }

  public String getStringLocal (int lindex) {
    return vm.getElementInfo(getLocalVariable(lindex)).asString();
  }

  public String getStringReturnValue () {
    return vm.getElementInfo(peek()).asString();
  }

  /**
   * Returns the object reference of the target.
   */
  public int getRunnableRef () {
    return targetRef;
  }

  /**
   * Returns the pointer to the object reference of the executing method
   */
  public int getThis () {
    return top.getThis();
  }

  public ElementInfo getThisElementInfo(){
    return getElementInfo(getThis());
  }

  public boolean isThis (ElementInfo r) {
    if (r == null) {
      return false;
    }

    if (top == null) {
      return false;
    }

    return getMethod().isStatic()
      ? false : r.getObjectRef() == getLocalVariable(0);
  }

  public boolean atMethod (String mname) {
    return top != null && getMethod().getFullName().equals(mname);
  }

  public boolean atPosition (int position) {
    if (top == null) {
      return false;
    } else {
      Instruction pc = getPC();
      return pc != null && pc.getPosition() == position;
    }
  }

  public boolean atReturn () {
    if (top == null) {
      return false;
    } else {
      Instruction pc = getPC();
      return pc instanceof ReturnInstruction;
    }
  }


  /**
   * reset any information that has to be re-computed in a backtrack
   * (i.e. hasn't been stored explicitly)
   */
  void resetVolatiles () {
    // resetting lock sets goes here
    lockedObjects = new LinkedList<ElementInfo>();

    // the ref of the object we are blocked on or waiting for
    lockRef = -1;
  }

  /**
   * this is used when restoring states
   */
  void updateLockedObject (ElementInfo ei) {
    lockedObjects.add(ei);
    // don't notify here, it's just a restore
  }

  void addLockedObject (ElementInfo ei) {
    lockedObjects.add(ei);
    vm.notifyObjectLocked(this, ei);
  }

  void removeLockedObject (ElementInfo ei) {
    lockedObjects.remove(ei);
    vm.notifyObjectUnlocked(this, ei);
  }

  /**
   * Clears the operand stack of all value.
   */
  public void clearOperandStack () {
    topClone().clearOperandStack();
  }

  public Object clone() {
    try {
      // threadData and top StackFrame are copy-on-write, so we should not have to clone them
      // lockedObjects are state-volatile and restored explicitly after a backtrack
      return super.clone();

    } catch (CloneNotSupportedException cnsx) {
      return null;
    }
  }

  LinkedList<ElementInfo> cloneLockedObjects() {
    LinkedList<ElementInfo> lo = new LinkedList<ElementInfo>();

    for (ElementInfo ei : lockedObjects) {
      lo.add((ElementInfo)ei.clone());
    }

    return lo;
  }


  /**
   * Returns the number of stack frames.
   */
  public int countStackFrames () {
    return stackDepth;
  }

  /**
   * get a stack snapshot that consists of an array of {mthId,pc} pairs.
   * strip stackframes that execute instance methods of the exception object
   */
  public int[] getSnapshot (int xObjRef) {
    StackFrame frame = top;
    int n = stackDepth;
    
    if (xObjRef != MJIEnv.NULL){ // filter out exception method frames
      for (;frame != null; frame = frame.getPrevious()){
        if (frame.getThis() != xObjRef){
          break;
        }
        n--;
      }
    }

    int j=0;
    int[] snap = new int[n*2];

    for (; frame != null; frame = frame.getPrevious()){
      snap[j++] = frame.getMethodInfo().getGlobalId();
      snap[j++] = frame.getPC().getInstructionIndex();
    }

    return snap;
  }

  /**
   * turn a snapshot into an JPF array of StackTraceElements, which means
   * a lot of objects. Do this only on demand
   */
  public int createStackTraceElements (int[] snapshot) {
    int n = snapshot.length/2;
    int nVisible=0;
    StackTraceElement[] list = new StackTraceElement[n];
    for (int i=0, j=0; i<n; i++){
      int methodId = snapshot[j++];
      int pcOffset = snapshot[j++];
      StackTraceElement ste = new StackTraceElement( methodId, pcOffset);
      if (!ste.ignore){
        list[nVisible++] = ste;
      }
    }

    Heap heap = vm.getHeap();
    int aref = heap.newArray("Ljava/lang/StackTraceElement;", nVisible, this);
    ElementInfo aei = heap.get(aref);
    for (int i=0; i<nVisible; i++){
      int eref = list[i].createJPFStackTraceElement();
      aei.setReferenceElement( i, eref);
    }

    return aref;
  }

  void print (PrintWriter pw, String s) {
    if (pw != null){
      pw.print(s);
    } else {
      vm.print(s);
    }
  }

  public void printStackTrace (int objRef) {
    printStackTrace(null, objRef);
  }

  public void printPendingExceptionOn (PrintWriter pw) {
    if (pendingException != null) {
      printStackTrace( pw, pendingException.getExceptionReference());
    }
  }

  /**
   * the reason why this is kind of duplicated (there is also a StackFrame.getPositionInfo)
   * is that this might be working off a StackTraceElement[] that is created when the exception
   * is created. At the time printStackTrace() is called, the StackFrames in question
   * are most likely already be unwinded
   */
  public void printStackTrace (PrintWriter pw, int objRef) {
    // 'env' usage is not ideal, since we don't know from what context we are called, and
    // hence the MJIEnv calling context might not be set (no Method or ClassInfo)
    // on the other hand, we don't want to re-implement all the MJIEnv accessor methods

    print(pw, env.getClassInfo(objRef).getName());
    int msgRef = env.getReferenceField(objRef,"detailMessage");
    if (msgRef != MJIEnv.NULL) {
      print(pw, ": ");
      print(pw, env.getStringObject(msgRef));
    }
    print(pw, "\n");

    // try the 'stackTrace' field first, it might have been set explicitly
    int aRef = env.getReferenceField(objRef, "stackTrace"); // StackTrace[]
    if (aRef != MJIEnv.NULL) {
      int len = env.getArrayLength(aRef);
      for (int i=0; i<len; i++) {
        int steRef = env.getReferenceArrayElement(aRef, i);
        if (steRef != MJIEnv.NULL){  // might be ignored (e.g. direct call)
          StackTraceElement ste = new StackTraceElement(steRef);
          ste.printOn( pw);
        }
      }

    } else { // fall back to use the snapshot stored in the exception object
      aRef = env.getReferenceField(objRef, "snapshot");
      int[] snapshot = env.getIntArrayObject(aRef);
      int len = snapshot.length/2;

      for (int i=0, j=0; i<len; i++){
        int methodId = snapshot[j++];
        int pcOffset = snapshot[j++];
        StackTraceElement ste = new StackTraceElement( methodId, pcOffset);
        ste.printOn( pw);
      }
    }

    int causeRef = env.getReferenceField(objRef, "cause");
    if ((causeRef != objRef) && (causeRef != MJIEnv.NULL)){
      print(pw, "Caused by: ");
      printStackTrace(pw, causeRef);
    }
  }

  class StackTraceElement {
    String clsName, mthName, fileName;
    int line;
    boolean ignore;


    StackTraceElement (int methodId, int pcOffset) {
      if (methodId == MethodInfo.REFLECTION_CALL) {
        clsName = "java.lang.reflect.Method";
        mthName = "invoke";
        fileName = "Native Method";
        line = -1;

      } else if (methodId == MethodInfo.DIRECT_CALL) {
        ignore = true;

      } else {
        MethodInfo mi = MethodInfo.getMethodInfo(methodId);
        if (mi != null) {
          clsName = mi.getClassName();
          mthName = mi.getName();

          fileName = mi.getStackTraceSource();
          if (pcOffset < 0){
            // See ThreadStopTest.threadDeathWhileRunstart
            // <2do> remove when RUNSTART is gone
            pcOffset = 0;
          }
          line = mi.getLineNumber(mi.getInstruction(pcOffset));

        } else { // this sounds like a bug
          clsName = "?";
          mthName = "?";
          fileName = "?";
          line = -1;
        }
      }
    }

    StackTraceElement (int sRef){
      clsName = env.getStringObject(env.getReferenceField(sRef, "clsName"));
      mthName = env.getStringObject(env.getReferenceField(sRef, "mthName"));
      fileName = env.getStringObject(env.getReferenceField(sRef, "fileName"));
      line = env.getIntField(sRef, "line");
    }

    int createJPFStackTraceElement() {
      if (ignore) {
        return MJIEnv.NULL;
      } else {
        Heap heap = vm.getHeap();

        ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.StackTraceElement");
        int sRef = heap.newObject(ci, ThreadInfo.this);

        ElementInfo  sei = heap.get(sRef);
        sei.setReferenceField("clsName", heap.newString(clsName, ThreadInfo.this));
        sei.setReferenceField("mthName", heap.newString(mthName, ThreadInfo.this));
        sei.setReferenceField("fileName", heap.newString(fileName, ThreadInfo.this));
        sei.setIntField("line", line);

        return sRef;
      }
    }

    void printOn (PrintWriter pw){
      if (!ignore){
        // the usual behavior is to print only the filename, strip the path
        if (fileName != null){
          int idx = fileName.lastIndexOf(File.separatorChar);
          if (idx >=0) {
            fileName = fileName.substring(idx+1);
          }
        }

        print(pw, "\tat ");
        if (clsName != null){
          print(pw, clsName);
          print(pw, ".");
        } else { // some synthetic methods don't belong to classes
          print(pw, "[no class] ");
        }
        print(pw, mthName);

        if (fileName != null){
          print(pw, "(");
          print(pw, fileName);
          if (line >= 0){
            print(pw, ":");
            print(pw, Integer.toString(line));
          }
          print(pw, ")");
        } else {
          //print(pw, "<no source>");
        }

        print(pw, "\n");
      }
    }
  }

  /**
   * <2do> pcm - this is not correct! We have to call a proper ctor
   * for the Throwable (for now, we just explicitly set the details)
   * but since this is not used with user defined exceptionHandlers (it's only
   * called from within the VM, i.e. with standard exceptionHandlers), we for
   * now skip the hassle of doing direct calls that would change the
   * call stack
   */
  int createException (ClassInfo ci, String details, int causeRef){
    Heap heap = vm.getHeap();
    int objref = heap.newObject(ci, this);
    int msgref = -1;

    ElementInfo ei = heap.get(objref);
	
    if (details != null) {
      msgref = heap.newString(details, this);
      ei.setReferenceField("detailMessage", msgref);
    }

    // store the stack snapshot
    int[] snap = getSnapshot(MJIEnv.NULL);
    int aref = env.newIntArray(snap);
    ei.setReferenceField("snapshot", aref);

    ei.setReferenceField("cause", (causeRef != MJIEnv.NULL)? causeRef : objref);

    return objref;
  }

  /**
   * Creates and throws an exception. This is what is used if the exception is
   * thrown by the VM (or a listener)
   */
  public Instruction createAndThrowException (ClassInfo ci, String details) {
    if (!ci.isRegistered()) {
      ci.registerClass(this);
    }

    if (!ci.isInitialized()){
      if (ci.initializeClass(this)) {
        return getPC();
      }
    }

    int objref = createException(ci,details, MJIEnv.NULL);
    return throwException(objref);
  }

  /**
   * Creates an exception and throws it.
   */
  public Instruction createAndThrowException (String cname) {
    return createAndThrowException(cname, null);
  }

  public Instruction createAndThrowException (String cname, String details) {
    try {
      ClassInfo ci = ClassInfo.getResolvedClassInfo(cname);
      return createAndThrowException(ci, details);

    } catch (NoClassInfoException cx){
      try {
        ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.NoClassDefFoundError");
        return createAndThrowException(ci, cx.getMessage());

      } catch (NoClassInfoException cxx){
        throw new JPFException("no java.lang.NoClassDefFoundError class");
      }
    }
  }

  /**
   * Duplicates a value on the top stack frame.
   */
  public void dup () {
    topClone().dup();
  }

  /**
   * Duplicates a long value on the top stack frame.
   */
  public void dup2 () {
    topClone().dup2();
  }

  /**
   * Duplicates a long value on the top stack frame.
   */
  public void dup2_x1 () {
    topClone().dup2_x1();
  }

  /**
   * Duplicates a long value on the top stack frame.
   */
  public void dup2_x2 () {
    topClone().dup2_x2();
  }

  /**
   * Duplicates a value on the top stack frame.
   */
  public void dup_x1 () {
    topClone().dup_x1();
  }

  /**
   * Duplicates a value on the top stack frame.
   */
  public void dup_x2 () {
    topClone().dup_x2();
  }

  /**
   * execute instructions until there is none left or somebody breaks
   * the transition (e.g. by registering a CG)
   */
  protected void executeTransition (SystemState ss) throws JPFException {
    Instruction pc = getPC();
    Instruction nextPc = null;

    currentThread = this;
    executedInstructions = 0;

    if (isStopped()){
      pc = throwStopException();
      setPC(pc);
    }
    
    // this constitutes the main transition loop. It gobbles up
    // insns until someone registered a ChoiceGenerator, there are no insns left,
    // the transition was explicitly marked as ignored, or we have reached a
    // max insn count and preempt the thread upon the next available backjump
    while (pc != null) {
      nextPc = executeInstruction();
      
      if (ss.breakTransition()) {
        break;
        
      } else {
        if (executedInstructions >= maxTransitionLength){ // try to preempt the current thread
          //if (vm.getStateId() > 0){
            if (pc.isBackJump() && (pc != nextPc) && (top != null && !top.isNative())) {
              if (yield()) {
                log.info("max transition length exceeded, breaking transition on ", nextPc);
                break;
              }
            }
          //}
        }
        
        pc = nextPc;
      }
    }
  }


  /**
   * Execute next instruction.
   */
  public Instruction executeInstruction () {
    Instruction pc = getPC();
    SystemState ss = vm.getSystemState();
    KernelState ks = vm.getKernelState();

    // the default, might be changed by the insn depending on if it's the first
    // time we exec the insn, and whether it does its magic in the top (before break)
    // or bottom half (re-exec after break) of the exec
    logInstruction = true;
    skipInstruction = false;
    nextPc = null;

    if (log.isLoggable(Level.FINER)) {
      log.fine( pc.getMethodInfo().getFullName() + " " + pc.getPosition() + " : " + pc);
    }

    // this is the pre-execution notification, during which a listener can perform
    // on-the-fly instrumentation or even replace the instruction altogether
    vm.notifyExecuteInstruction(this, pc);

    if (!skipInstruction) {
      // execute the next bytecode
    	nextPc = pc.execute(ss, ks, this);
    }

    // we also count the skipped ones
    executedInstructions++;
    
    if (logInstruction) {
      ss.recordExecutionStep(pc);
    }

    // here we have our post exec bytecode exec observation point
    vm.notifyInstructionExecuted(this, pc, nextPc);
    
    // clean up whatever might have been stored by execute
    pc.cleanupTransients();

    // set+return the next insn to execute if we did not return from the last stack frame.
    // Note that 'nextPc' might have been set by a listener, and/or 'top' might have
    // been changed by executing an invoke, return or throw (handler), or by
    // pushing overlay calls on the stack
    if (top != null) {
      // <2do> this is where we would have to handle general insn repeat
      setPC(nextPc);
      return nextPc;
    } else {
      return null;
    }
  }

  /**
   * execute instruction hidden from any listeners, and do not
   * record it in the path
   */
  public Instruction executeInstructionHidden () {
    Instruction pc = getPC();
    SystemState ss = vm.getSystemState();
    KernelState ks = vm.getKernelState();

    nextPc = null; // reset in case pc.execute blows (this could be behind an exception firewall)

    if (log.isLoggable(Level.FINE)) {
      log.fine( pc.getMethodInfo().getFullName() + " " + pc.getPosition() + " : " + pc);
    }

    nextPc = pc.execute(ss, ks, this);

    // we did not return from the last frame stack
    if (top != null) { // <2do> should probably bomb otherwise
      setPC(nextPc);
    }

    return nextPc;
  }

  /**
   * is this after calling Instruction.execute()
   * used by instructions and listeners
   */
  public boolean isPostExec() {
    return (nextPc != null);
  }

  public void reExecuteInstruction() {
    nextPc = getPC();
  }

  public boolean willReExecuteInstruction() {
    return (getPC() == nextPc);
  }

  /**
   * skip the next bytecode. To be used by listeners to on-the-fly replace
   * instructions
   */
  public void skipInstruction (Instruction nextInsn) {
    skipInstruction = true;
    
    assert nextInsn != null;
    nextPc = nextInsn;
  }

  /**
   * skip and continue with the following instruction. This is deprecated because
   * callers should explicitly provide the next instruction (depending on the
   * skipped insn)
   */
  @Deprecated
  public void skipInstruction(){
    skipInstruction(getPC().getNext());
  }

  public boolean isInstructionSkipped() {
    return skipInstruction;
  }

  public void skipInstructionLogging () {
    logInstruction = false;
  }

  /**
   * explicitly set the next insn to execute. To be used by listeners that
   * replace bytecode exec (during 'executeInstruction' notification
   *
   * Note this is dangerous because you have to make sure the operand stack is
   * in a consistent state. This also will fail if someone already ordered
   * reexecution of the current instruction
   */
  public void setNextPC (Instruction insn) {
    nextPc = insn;
  }

  /**
   * Executes a method call. Be aware that it executes the whole method as one atomic
   * step. Arguments have to be already on the provided stack
   *
   * This only works for non-native methods, and does not allow any choice points,
   * so you have to know very well what you are doing.
   *
   * Instructions executed by this method are still fully observable and stored in
   * the path
   */
  public void executeMethodAtomic (StackFrame frame) {

    pushFrame(frame);
    int    depth = countStackFrames();
    Instruction pc = frame.getPC();
    SystemState ss = vm.getSystemState();

    ss.incAtomic(); // to shut off avoidable context switches (MONITOR_ENTER and wait() can still block)

    while (depth <= countStackFrames()) {
      Instruction nextPC = executeInstruction();

      if (ss.getNextChoiceGenerator() != null) {
        // BANG - we can't have CG's here
        // should be rather an ordinary exception
        // createAndThrowException("java.lang.AssertionError", "choice point in sync executed method: " + frame);
        throw new JPFException("choice point in atomic method execution: " + frame);
      } else {
        pc = nextPC;
      }
    }

    vm.getSystemState().decAtomic();

    nextPc = null;

    // the frame was already removed by the RETURN insn of the frame's method
  }

  /**
   * execute method atomically, but also hide it from listeners and do NOT add
   * executed instructions to the path.
   *
   * this can be even more confusing than executeMethodAtomic(), since
   * nothing prevents such a method from changing the program state, and we
   * wouldn't know for what reason by looking at the trace
   *
   * this method should only be used if we have to execute test application code
   * like hashCode() or equals() from native code, e.g. to silently check property
   * violations
   *
   * executeMethodHidden also acts as an exception firewall, since we don't want
   * any silently executed code fall back into the visible path (for
   * no observable reason)
   */
  public void executeMethodHidden (StackFrame frame) {

    pushFrame(frame);
    
    int depth = countStackFrames(); // this includes the DirectCallStackFrame
    Instruction pc = frame.getPC();

    vm.getSystemState().incAtomic(); // to shut off avoidable context switches (MONITOR_ENTER and wait() can still block)

    while (depth <= countStackFrames()) {
      Instruction nextPC = executeInstructionHidden();

      if (pendingException != null) {

      } else {
        if (nextPC == pc) {
          // BANG - we can't have CG's here
          // should be rather an ordinary exception
          // createAndThrowException("java.lang.AssertionError", "choice point in sync executed method: " + frame);
          throw new JPFException("choice point in hidden method execution: " + frame);
        } else {
          pc = nextPC;
        }
      }
    }

    vm.getSystemState().decAtomic();

    nextPc = null;

    // the frame was already removed by the RETURN insn of the frame's method
  }

  public Heap getHeap () {
    return vm.getHeap();
  }

  public ElementInfo getElementInfo (int ref) {
    Heap heap = vm.getHeap();
    return heap.get(ref);
  }


  /**
   * this should only be called from the top half of the last DIRECTCALLRETURN of
   * a thread.
   * @return true - if the thread is done, false - if instruction has to be re-executed
   */
  public boolean exit(){
    int objref = getThreadObjectRef();
    ElementInfo ei = getElementInfo(objref);
    SystemState ss = vm.getSystemState();
    ThreadList tl = vm.getThreadList();
    
    // beware - this notifies all waiters for this thread (e.g. in a join())
    // hence it has to be able to acquire the lock
    if (!ei.canLock(this)) {
      // block first, so that we don't get this thread in the list of CGs
      ei.block(this);

      // if we can't acquire the lock, it means there needs to be another thread alive,
      // but it might not be runnable (deadlock) and we don't want to mask that error
      ChoiceGenerator<ThreadInfo> cg = ss.getSchedulerFactory().createMonitorEnterCG(ei, this);
      ss.setMandatoryNextChoiceGenerator(cg, "blocking thread termination without CG: ");

      return false; // come back once we can obtain the lock to notify our waiters

    } else { // Ok, we can get the lock, time to die
      
      // if this is the last non-daemon and there are only daemons left (which
      // would be killed by our termination) we have to give them a chance to
      // run BEFORE we terminate, to catch errors in those daemons we might have
      // triggered in our last transition. In a way, this simulates preemption on
      // non-CG insns within our last transition
      if (tl.hasOnlyDaemonRunnablesOtherThan(this)){
        if (yield()){
          return false;
        }
      }
      
      //--- now we get into the termination spin
      
      // notify waiters on thread termination
      if (!holdsLock(ei)) {
        // we only need to increase the lockcount if we don't own the lock yet,
        // as is the case for synchronized run() in anonymous threads (otherwise
        // we have a lockcount > 1 and hence do not unlock upon return)
        ei.lock(this);
      }

      ei.notifiesAll(); // watch out, this might change the runnable count
      ei.unlock(this);

      setState(State.TERMINATED);
      
      // we don't unregister this thread yet, this happens when the corresponding
      // thread object is collected

      ss.clearAtomic();
      cleanupThreadObject(ei);
      vm.activateGC();  // stack is gone, so reachability might change

      if (tl.hasOtherNonDaemonRunnablesThan(this)){
        ChoiceGenerator<ThreadInfo> cg = ss.getSchedulerFactory().createThreadTerminateCG(this);
        ss.setMandatoryNextChoiceGenerator(cg, "thread terminated without CG: ");
      }

      popFrame(); // we need to do this *after* setting the CG (so that we still have a CG insn)
      
      return true;
    }
  }


  /**
   * this is called upon ThreadInfo.exit(), i.e. the Thread object can be still
   * around for a while
   */
  void cleanupThreadObject (ElementInfo ei) {
    // ideally, this should be done by calling Thread.exit(), but this
    // does a ThreadGroup.remove(), which does a lot of sync stuff on the shared
    // ThreadGroup object, which might create lots of states. So we just nullify
    // the Thread fields and remove it from the ThreadGroup from here
    int grpRef = ei.getReferenceField("group");
    cleanupThreadGroup(grpRef, ei.getObjectRef());

    ei.setReferenceField("group", MJIEnv.NULL);
    ei.setReferenceField("threadLocals", MJIEnv.NULL);
    ei.setReferenceField("inheritableThreadLocals", MJIEnv.NULL);
    ei.setReferenceField("uncaughtExceptionHandler", MJIEnv.NULL);
  }

  
  void cleanupThreadGroup (int grpRef, int threadRef) {
    if (grpRef != MJIEnv.NULL) {
      ElementInfo eiGrp = getElementInfo(grpRef);
      int threadsRef = eiGrp.getReferenceField("threads");
      if (threadsRef != MJIEnv.NULL) {
        ElementInfo eiThreads = getElementInfo(threadsRef);
        if (eiThreads.isArray()) {
          int nthreads = eiGrp.getIntField("nthreads");

          for (int i=0; i<nthreads; i++) {
            int tref = eiThreads.getReferenceElement(i);

            if (tref == threadRef) { // compact the threads array
              int n1 = nthreads-1;
              for (int j=i; j<n1; j++) {
                eiThreads.setReferenceElement(j, eiThreads.getReferenceElement(j+1));
              }
              eiThreads.setReferenceElement(n1, MJIEnv.NULL);

              eiGrp.setIntField("nthreads", n1);
              if (n1 == 0) {
                eiGrp.lock(this);
                eiGrp.notifiesAll();
                eiGrp.unlock(this);
              }

              // <2do> we should probably also check if we have to set it destroyed

              return;
            }
          }
        }
      }
    }
  }

  public void hash (HashData hd) {
    threadData.hash(hd);

    for (StackFrame f = top; f != null; f = f.getPrevious()){
      f.hash(hd);
    }
  }

  public void interrupt () {
    ElementInfo eiThread = getElementInfo(getThreadObjectRef());

    State status = getState();
    switch (status) {
    case RUNNING:
    case BLOCKED:
    case UNBLOCKED:
    case NOTIFIED:
    case TIMEDOUT:
      // just set interrupt flag
      eiThread.setBooleanField("interrupted", true);
      break;

    case WAITING:
    case TIMEOUT_WAITING:
      eiThread.setBooleanField("interrupted", true);
      setState(State.INTERRUPTED);

      // since this is potentially called w/o owning the wait lock, we
      // have to check if the waiter goes directly to UNBLOCKED
      ElementInfo eiLock = getElementInfo(lockRef);
      if (eiLock.canLock(this)) {
        resetLockRef();
        setState(State.UNBLOCKED);
        
        // we cannot yet remove this thread from the Monitor lock contender list
        // since it still has to re-acquire the lock before becoming runnable again
        
        // NOTE: this can lead to attempts to reenter the same thread to the 
        // lock contender list if the interrupt handler of the interrupted thread
        // tries to wait/block/park again
        //eiLock.setMonitorWithoutLocked(this);
      }
      
      break;

    case NEW:
    case TERMINATED:
      // ignore
      break;

    default:
    }
  }

  /**
   * Peeks the top long value from the top stack frame.
   */
  public long longPeek () {
    return top.longPeek();
  }

  /**
   * Peeks a long value from the top stack frame.
   */
  public long longPeek (int n) {
    return top.longPeek(n);
  }

  /**
   * Pops the top long value from the top stack frame.
   */
  public long longPop () {
    return topClone().longPop();
  }

  /**
   * Pushes a long value of the top stack frame.
   */
  public void longPush (long v) {
    topClone().longPush(v);
  }


  /**
   * mark all objects during gc phase1 which are reachable from this threads
   * root set (Thread object, Runnable, stack)
   * @aspects: gc
   */
  void markRoots (Heap heap) {

    // 1. mark the Thread object itself
    heap.markThreadRoot(objRef, gid);

    // 2. and its runnable
    if (targetRef != MJIEnv.NULL) {
      heap.markThreadRoot(targetRef,gid);
    }

    // 3. now all references on the stack
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      frame.markThreadRoots(heap, gid);
    }
  }


  /**
   * replace the top frame - this is a dangerous method that should only
   * be used from Restoreres and to restore operators and locals in post-execution notifications
   * to their pre-execution contents
   */
  public void setTopFrame (StackFrame frame) {
    top = frame;

    // since we have swapped the top frame, the stackDepth might have changed
    int n = 0;
    for (StackFrame f = frame; f != null; f = f.getPrevious()){
      n++;
    }
    stackDepth = n;
  }

  /**
   * Peeks the top value from the top stack frame.
   */
  public int peek () {
    if (top != null) {
      return top.peek();
    } else {
      // <?> not really sure what to do here, but if the stack is gone, so is the thread
      return -1;
    }
  }

  /**
   * Peeks a int value from the top stack frame.
   */
  public int peek (int n) {
    if (top != null) {
      return top.peek(n);
    } else {
      // <?> see peek()
      return -1;
    }
  }


  /**
   * Pops the top value from the top stack frame.
   */
  public int pop () {
    if (top != null) {
      return topClone().pop();
    } else {
      // <?> see peek()
      return -1;
    }
  }

  /**
   * Pops a set of values from the top stack frame.
   */
  public void pop (int n) {
    if (top != null) {
      topClone().pop(n);
    }
  }


  /**
   * Adds a new stack frame for a new called method.
   */
  public void pushFrame (StackFrame frame) {

    frame.setPrevious(top);

    top = frame;
    stackDepth++;

    // a new frame cannot have been stored yet, so we don't need to clone on the next mod
    // note this depends on not pushing a frame in the top half of a CG method
    markTfChanged(top);

    returnedDirectCall = null;
  }

  /**
   * Removes a stack frame
   */
  public StackFrame popFrame() {
    StackFrame frame = top;

    //--- do our housekeeping
    if (frame.hasAnyRef()) {
      vm.getSystemState().activateGC();
    }

    // there always is one since we start all threads through directcalls
    top = frame.getPrevious();
    stackDepth--;
    
    return top;
  }

  /**
   * removing DirectCallStackFrames is a bit different (only happens from
   * DIRECTCALLRETURN insns)
   */
  public StackFrame popDirectCallFrame() {
    assert top instanceof DirectCallStackFrame;

    returnedDirectCall = top;

    if (top instanceof UncaughtHandlerFrame){
      return popUncaughtHandlerFrame();
    }
    
    top = top.getPrevious();
    stackDepth--;
    
    return top;
  }

  
  public boolean hasReturnedFromDirectCall () {
    // this is reset each time we push a new frame
    return (returnedDirectCall != null);
  }

  public boolean hasReturnedFromDirectCall(String directCallId){
    return (returnedDirectCall != null &&
            returnedDirectCall.getMethodName().equals(directCallId));
  }

  public StackFrame getReturnedDirectCall () {
    return returnedDirectCall;
  }


  public String getStateDescription () {
    StringBuilder sb = new StringBuilder("thread ");
    sb.append(getThreadObjectClassInfo().getName());
    sb.append(":{id:");
    sb.append(gid);
    sb.append(',');
    sb.append(threadData.getFieldValues());
    sb.append('}');
    
    return sb.toString();
  }

  public ClassInfo getThreadObjectClassInfo() {
    return getThreadObject().getClassInfo();
  }
  
  /**
   * Prints the content of the stack
   */
  public void printStackContent () {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      frame.printStackContent();
    }
  }

  /**
   * Prints current stacktrace information
   */
  public void printStackTrace () {
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()){
      frame.printStackTrace();
    }
  }


  //--- those are the transfer operations between operand stack and locals
  public void push (int v, boolean ref) {
    topClone().push(v, ref);
  }

  public void pushRef (int ref) {
    topClone().pushRef(ref);
  }

  public void push (int v) {
    topClone().push(v);
  }

  public void pushLocal (int localIndex){
    topClone().pushLocal(localIndex);
  }

  public void pushLongLocal (int localIndex){
    topClone().pushLongLocal(localIndex);
  }

  public void storeOperand (int localIndex){
    topClone().storeOperand(localIndex);
  }

  public void storeLongOperand (int localIndex){
    topClone().storeLongOperand(localIndex);
  }

  /**
   * Removes the arguments of a method call.
   */
  public void removeArguments (MethodInfo mi) {
    int i = mi.getArgumentsSize();

    if (i != 0) {
      pop(i);
    }
  }

  /**
   * Swaps two entry on the stack.
   */
  public void swap () {
    topClone().swap();
  }

  boolean haltOnThrow (String exceptionClassName){
    if ((haltOnThrow != null) && (haltOnThrow.matchesAny(exceptionClassName))){
      return true;
    }

    return false;
  }

  Instruction throwStopException (){

    // <2do> maybe we should do a little sanity check first
    ElementInfo ei = getThreadObject();

    int xRef = ei.getReferenceField("stopException");
    ei.setReferenceField("stopException", MJIEnv.NULL);

    Instruction insn = getPC();
    if (insn instanceof EXECUTENATIVE){
      // we only get here if there was a CG in a native method and we might
      // have to reacquire a lock to go on

      // <2do> it would be better if we could avoid to execute the native method
      // since it might have side effects like overwriting the exception or
      // doing roundtrips in its bottom half, but we don't know which lock that
      // is (lockRef might be already reset)

      env.throwException(xRef);
      return insn;
    }

    return throwException(xRef);
  }

  /**
   * unwind stack frames until we find a matching handler for the exception object
   */
  public Instruction throwException (int exceptionObjRef) {
    Heap heap = vm.getHeap();
    ElementInfo ei = heap.get(exceptionObjRef);
    ClassInfo ci = ei.getClassInfo();
    String cname = ci.getName();
    StackFrame handlerFrame = null; // the stackframe that has a matching handler (if any)
    ExceptionHandler matchingHandler = null; // the matching handler we found (if any)

    // first, give the VM a chance to intercept (we do this before changing anything)
    Instruction insn = vm.handleException(this, exceptionObjRef);
    if (insn != null){
      return insn;
    }

    // we don't have to store the stacktrace explicitly anymore, since that is now
    // done in the Throwable ctor (more specifically the native fillInStackTrace)
    pendingException = new ExceptionInfo(this, ei);

    vm.notifyExceptionThrown(this, ei);

    if (haltOnThrow(cname)) {
      // shortcut - we don't try to find a handler for this one but bail immediately
      NoUncaughtExceptionsProperty.setExceptionInfo(pendingException);
      throw new UncaughtException(this, exceptionObjRef);
    }

    // check if we find a matching handler, and if we do store it. Leave the
    // stack untouched so that listeners can still inspect it
    for (StackFrame frame = top; (frame != null) && (handlerFrame == null); frame = frame.getPrevious()) {
      MethodInfo mi = frame.getMethodInfo();

      // that means we have to turn the exception into an InvocationTargetException
      if (mi.isReflectionCallStub()) {
        ci               = ClassInfo.getInitializedClassInfo("java.lang.reflect.InvocationTargetException", this);
        exceptionObjRef  = createException(ci, cname, exceptionObjRef);
        cname            = ci.getName();
        ei               = heap.get(exceptionObjRef);
        pendingException = new ExceptionInfo(this, ei);
      }

      insn = frame.getPC();
      int position = insn.getPosition();

      ExceptionHandler[] exceptions = mi.getExceptions();
      if (exceptions != null) {
        // checks the exceptionHandlers caught (in order of handler definitions)
        for (ExceptionHandler handler : exceptions){
          // checks if it falls in the right range
          if ((position >= handler.getBegin()) && (position < handler.getEnd())) {
            // checks if this type of exception is caught here (null means 'any')
            String en = handler.getName();
            if ((en == null) || ci.isInstanceOf(en)) {
              handlerFrame = frame;
              matchingHandler = handler;
              break;
            }
          }
        }
      }

      if ((handlerFrame == null) && mi.isFirewall()) {
        // this method should not let exceptionHandlers pass into lower level stack frames
        // (e.g. for <clinit>, or hidden direct calls)
        // <2do> if this is a <clinit>, we should probably turn into an
        // ExceptionInInitializerError first
        unwindTo(frame);
        NoUncaughtExceptionsProperty.setExceptionInfo(pendingException);
        throw new UncaughtException(this, exceptionObjRef);
      }
    }

    if (handlerFrame == null) {
      // we still have to check if there is a Thread.UncaughtExceptionHandler in effect,
      // and if we already execute within one, in which case we don't reenter it
      if (!ignoreUncaughtHandlers && !isUncaughtHandlerOnStack()) {
        // we use a direct call instead of exception handlers within the run()/main()
        // direct call methods because we want to preserve the whole stack in case
        // we treat returned (report-only) handlers as NoUncaughtExceptionProperty
        // violations (passUncaughtHandler=false)
        insn = callUncaughtHandler(pendingException);
        if (insn != null) {
          // we only do this if there is a UncaughtHandler other than the standard
          // ThreadGroup, hence we have to check for the return value. If there is
          // only ThreadGroup.uncaughtException(), we put the system out of its misery
          return insn;
        }
      }

      // there was no overridden uncaughtHandler, or we already executed it
      if ("java.lang.ThreadDeath".equals(cname)) { // gracefully shut down
        unwindToFirstFrame();
        pendingException = null;
        return top.getPC().getNext(); // the final DIRECTCALLRETURN

      } else { // we have a NoUncaughtPropertyViolation
        NoUncaughtExceptionsProperty.setExceptionInfo(pendingException);
        throw new UncaughtException(this, exceptionObjRef);
      }

    } else { // we found a matching handler

      unwindTo(handlerFrame);

      // according to the VM spec, before transferring control to the handler we have
      // to reset the operand stack to contain only the exception reference
      // (4.9.2 - "4. merge the state of the operand stack..")
      clearOperandStack();
      push(exceptionObjRef, true);

      // jump to the exception handler and set pc so that listeners can see it
      int handlerOffset = matchingHandler.getHandler();
      insn = handlerFrame.getMethodInfo().getInstructionAt(handlerOffset);
      setPC(insn);

      // notify before we reset the pendingException
      vm.notifyExceptionHandled(this);

      pendingException = null; // handled, no need to keep it

      return insn;
    }
  }

  
  /**
   * this explicitly models the standard ThreadGroup.uncaughtException(), but we want
   * to save us a roundtrip if that's the only handler we got. If we would use
   * a handler block in the run()/main() direct call stubs that just delegate to
   * the standard ThreadGroup.uncaughtException(), we would have trouble mapping
   * this to NoUncaughtExceptionProperty violations (which is just a normal
   * printStackTrace() in there).
   */
  protected Instruction callUncaughtHandler (ExceptionInfo xi){
    Instruction insn = null;
    
    // 1. check if this thread has its own uncaughtExceptionHandler set. If not,
    // hand it over to ThreadGroup.uncaughtException()
    int  hRef = getInstanceUncaughtHandler();
    if (hRef != MJIEnv.NULL){
      insn = callUncaughtHandler(xi, hRef, "[threadUncaughtHandler]");
      
    } else {
      // 2. check if any of the ThreadGroup chain has an overridden uncaughtException
      int grpRef = getThreadGroupRef();
      hRef = getThreadGroupUncaughtHandler(grpRef);
      
      if (hRef != MJIEnv.NULL){
        insn = callUncaughtHandler(xi, hRef, "[threadGroupUncaughtHandler]");
      
      } else {
        // 3. as a last measure, check if there is a global handler 
        hRef = getGlobalUncaughtHandler();
        if (hRef != MJIEnv.NULL){
          insn = callUncaughtHandler(xi, hRef, "[globalUncaughtHandler]");
        }    
      }
    }
    
    return insn;
  }

  public boolean isUncaughtHandler() {
    return (top instanceof UncaughtHandlerFrame);
  }

  
  protected boolean isUncaughtHandlerOnStack(){
    for (StackFrame frame = top; frame != null; frame = frame.getPrevious()) {
      if (frame instanceof UncaughtHandlerFrame){
        return true;
      }
    }
    
    return false;
  }
  
  protected int getInstanceUncaughtHandler (){
    ElementInfo ei = getElementInfo(objRef);
    int handlerRef = ei.getReferenceField("uncaughtExceptionHandler");
    return handlerRef;
  }
  
  protected int getThreadGroupRef() {
    ElementInfo ei = getElementInfo(objRef);
    int groupRef = ei.getReferenceField("group");
    return groupRef;
  }
  
  protected int getThreadGroupUncaughtHandler (int grpRef){

    // get the first overridden uncaughtException() implementation in the group chain
    while (grpRef != MJIEnv.NULL){
      ElementInfo eiGrp = getElementInfo(grpRef);
      ClassInfo ciGrp = eiGrp.getClassInfo();
      MethodInfo miHandler = ciGrp.getMethod("uncaughtException(Ljava/lang/Thread;Ljava/lang/Throwable;)V", true);
      ClassInfo ciHandler = miHandler.getClassInfo();
      if (!ciHandler.getName().equals("java.lang.ThreadGroup")) {
        return eiGrp.getObjectRef();
      }

      grpRef = eiGrp.getReferenceField("parent");
    }
    
    // no overridden uncaughtHandler found
    return MJIEnv.NULL;
  }
  
  protected int getGlobalUncaughtHandler(){
    ElementInfo ei = getElementInfo(objRef);
    ClassInfo ci = ei.getClassInfo();
    
    return ci.getStaticElementInfo().getReferenceField("defaultUncaughtExceptionHandler");
  }
  
  protected Instruction callUncaughtHandler(ExceptionInfo xi, int handlerRef, String id){
    ElementInfo eiHandler = getElementInfo(handlerRef);
    ClassInfo ciHandler = eiHandler.getClassInfo();
    MethodInfo miHandler = ciHandler.getMethod("uncaughtException(Ljava/lang/Thread;Ljava/lang/Throwable;)V", true);

    MethodInfo stub = miHandler.createDirectCallStub(id);
    StackFrame frame = new UncaughtHandlerFrame(xi, stub);

    frame.pushRef(handlerRef);
    frame.pushRef(objRef);
    frame.pushRef(xi.getExceptionReference());

    pushFrame(frame);
    
    return frame.getPC();
  }
  
  protected StackFrame popUncaughtHandlerFrame(){    
    // we return from a overridden uncaughtException() direct call, but
    // its debatable if this counts as 'handled'. For handlers that just do
    // reporting this is probably false and we want JPF to report the defect.
    // If this is a fail-safe handler that tries to clean up so that other threads can
    // take over, we want to be able to go on and properly shut down the 
    // thread without property violation
    
    if (passUncaughtHandler) {
      // gracefully shutdown this thread
      unwindToFirstFrame(); // this will take care of notifying
      pendingException = null;
      
      topClone().advancePC();
      assert top.getPC() instanceof ReturnInstruction : "topframe PC not a ReturnInstruction: " + top.getPC();
      return top;

    } else {
      // treat this still as an NoUncaughtExceptionProperty violation
      pendingException = ((UncaughtHandlerFrame) returnedDirectCall).getExceptionInfo();
      NoUncaughtExceptionsProperty.setExceptionInfo(pendingException);
      throw new UncaughtException(this, pendingException.getExceptionReference());
    }
  }

  
  protected void unwindTo (StackFrame newTopFrame){
    for (StackFrame frame = top; (frame != null) && (frame != newTopFrame); frame = frame.getPrevious()) {
      MethodInfo mi = frame.getMethodInfo();
      mi.leave(this); // that takes care of releasing locks
      vm.notifyExceptionBailout(this); // notify before we pop the frame
      popFrame();
    }
  }

  protected StackFrame unwindToFirstFrame(){
    StackFrame frame;

    for (frame = top; frame.getPrevious() != null; frame = frame.getPrevious()) {
      MethodInfo mi = frame.getMethodInfo();
      mi.leave(this); // that takes care of releasing locks
      vm.notifyExceptionBailout(this); // notify before we pop the frame
      popFrame();
    }

    return frame;
  }

  public ExceptionInfo getPendingException () {
    return pendingException;
  }

  /**
   * watch out - just clearing it might cause an infinite loop
   * if we don't drop frames and/or advance the pc
   */
  public void clearPendingException () {
    NoUncaughtExceptionsProperty.setExceptionInfo(null);
    pendingException = null;
  }

  /**
   * Returns a clone of the thread data. To be called every time we change some ThreadData field
   * (which unfortunately includes lock counts, hence this should be changed)
   */
  protected ThreadData threadDataClone () {
    if ((attributes & ATTR_DATA_CHANGED) != 0) {
      // already cloned, so we don't have to clone
    } else {
      // reset, so that next storage request would recompute tdIndex
      markTdChanged();
      vm.kernelStateChanged();

      threadData = threadData.clone();
    }

    return threadData;
  }

  public void restoreThreadData(ThreadData td) {
    threadData = td;
  }

  /**
   * request a reschedule no matter what the next insn is
   * Note this unconditionally creates and registers a ThreadCG, even if there is
   * only one runnable thread (ourself). This is intended to be used from
   * within Listeners that need to break transitions / store states in locations
   * only they know about.
   * Note also this differs from Thread.yield() in that yield() is handled by
   * the SchedulerFactory, and it is at its discretion to just ignore it, either
   * because yield in itself is not POR relevant (doesn't change anything), or
   * because there might be only one runnable thread. In both cases, the transition
   * would not be broken.
   * If there is more than one runnable thread, this also differs from
   * breakTransition(), which will continue with the same thread
   */
  public void reschedule (boolean forceBreak) {
    ThreadInfo[] runnables = vm.getRunnableThreads();

    if (forceBreak || (runnables.length > 1)) {
      ThreadChoiceGenerator cg = new ThreadChoiceFromSet("reschedule",runnables,true);
      SystemState ss = vm.getSystemState();
      ss.setNextChoiceGenerator(cg); // this breaks the transition
    }
  }

  /**
   * this is a version that unconditionally breaks the current transition
   * without really adding choices. It only goes on with the same thread
   * (to avoid state explosion).
   *
   * if the current transition is already marked as ignored, this method does nothing
   *
   * NOTE: this neither means that we ignore the current transition, nor that
   * it is an end state
   */
  public void breakTransition() {
    SystemState ss = vm.getSystemState();

    if (!ss.isIgnored()){
      // no need to set a CG if this transition is already marked as ignored
      // (which will also break executeTransition)
      BreakGenerator cg = new BreakGenerator( "breakTransition", this, false);
      ss.setNextChoiceGenerator(cg); // this breaks the transition
    }
  }

  public boolean yield() {
    SystemState ss = vm.getSystemState();

    if (!ss.isIgnored()){
      ThreadList tl = vm.getThreadList();
      ThreadInfo[] choices = tl.getRunnableThreads();
      ThreadChoiceFromSet cg = new ThreadChoiceFromSet( "yield", choices, true);
        
      return ss.setNextChoiceGenerator(cg); // this breaks the transition
    }
    
    return false;
  }  
  
  /**
   * this breaks the current transition with a CG that forces an end state (i.e.
   * has no choices)
   * this only takes effect if the current transition is not already marked
   * as ignored
   */
  public void breakTransition(boolean isTerminator) {
    SystemState ss = vm.getSystemState();

    if (!ss.isIgnored()){
      BreakGenerator cg = new BreakGenerator( "breakTransition", this, isTerminator);
      ss.setNextChoiceGenerator(cg); // this breaks the transition
    }
  }


  public boolean checkPorFieldBoundary () {
    return (executedInstructions == 0) && porFieldBoundaries && vm.hasOtherRunnablesThan(this);
  }

  public boolean hasOtherRunnables () {
    return vm.hasOtherRunnablesThan(this);
  }

  public boolean hasOtherNonDaemonRunnables() {
    return vm.hasOtherNonDaemonRunnablesThan(this);
  }
  
  protected void markUnchanged() {
    attributes &= ~ATTR_ANY_CHANGED;

    for (StackFrame f = top; f != null && f.hasChanged(); f = f.getPrevious()){
      f.setChanged(false);
    }
  }

  protected void markTfChanged(StackFrame frame) {
    frame.setChanged(true);

    attributes |= ATTR_STACK_CHANGED;
    vm.kernelStateChanged();
  }

  protected void markTdChanged() {
    attributes |= ATTR_DATA_CHANGED;
    vm.kernelStateChanged();
  }

  public StackFrame getCallerStackFrame() {
    if (top != null){
      return top.getPrevious();
    } else {
      return null;
    }
  }

  public boolean hasDataChanged() {
    return (attributes & ATTR_DATA_CHANGED) != 0;
  }

  public boolean hasStackChanged() {
    return (attributes & ATTR_STACK_CHANGED) != 0;
  }

  public boolean hasChanged() {
    return (attributes & ATTR_ANY_CHANGED) != 0;
  }

  /**
   * Returns a clone of the top stack frame.
   */
  protected StackFrame topClone () {
    if (!top.hasChanged()) {
      top = top.clone();
      markTfChanged(top);
    }
    return top;
  }

  /**
   * Returns the top stack frame.
   */
  public StackFrame getTopFrame () {
    return top;
  }

  /**
   * this is going to be used to directly manipulate the Stackframes in lieu
   * of the various forwarding operations such as dup(), push() etc.
   */
  public StackFrame getClonedTopFrame(){
    if (!top.hasChanged()) {
      top = top.clone();
      markTfChanged(top);
    }
    return top;    
  }
  
  public StackFrame getStackFrameExecuting (Instruction insn, int offset){
    int n = offset;
    StackFrame frame = top;

    for (; (n > 0) && frame != null; frame = frame.getPrevious()){
      n--;
    }

    for(; frame != null; frame = frame.getPrevious()){
      if (frame.getPC() == insn){
        return frame;
      }
    }

    return null;
  }

  public String toString() {
    return "ThreadInfo [name=" + getName() + ",id=" + gid + ",state=" + getStateName() + ']';
  }

  void setDaemon (boolean isDaemon) {
    threadDataClone().isDaemon = isDaemon;
  }

  public boolean isDaemon () {
    return threadData.isDaemon;
  }

  MJIEnv getMJIEnv () {
    return env;
  }

  void setName (String newName) {
    threadDataClone().name = newName;

    // see 'setPriority()', only that it's more serious here, because the
    // java.lang.Thread name is stored as a char[]
  }

  public void setPriority (int newPrio) {
    if (threadData.priority != newPrio) {
      threadDataClone().priority = newPrio;

      // note that we don't update the java.lang.Thread object, but
      // use our threadData value (which works because the object
      // values are just used directly from the Thread ctors (from where we pull
      // it out in our ThreadInfo ctor), and henceforth only via our intercepted
      // native getters
    }
  }

  public int getPriority () {
    return threadData.priority;
  }




  /**
   * Comparison for sorting based on index.
   */
  public int compareTo (ThreadInfo that) {
    return this.gid - that.gid;
  }
  
  /**
   * only for debugging purposes
   */
  public void checkConsistency(boolean isStore){
    checkAssertion(threadData != null, "no thread data");
    
    // if the thread is runnable, it can't be blocked
    if (isRunnable()){
      checkAssertion(lockRef == -1, "runnable thread with non-null lockRef: " + lockRef) ;
    }
    
    // every thread that has been started and is not terminated has to have a stackframe with a next pc
    if (!isTerminated() && !isNew()){
      checkAssertion( stackDepth > 0, "empty stack " + getState());
      checkAssertion( top != null, "no top frame");
      checkAssertion( top.getPC() != null, "no top PC");
    }
    
    // if we are timedout, the top pc has to be either on a native Object.wait() or Unsafe.park()
    if (isTimedOut()){
      Instruction insn = top.getPC();
      checkAssertion( insn instanceof EXECUTENATIVE, "thread timedout outside of native method");
      
      // this is a bit dangerous in case we introduce new timeout points
      MethodInfo mi = ((EXECUTENATIVE)insn).getExecutedMethod();
      String mname = mi.getUniqueName();
      checkAssertion( mname.equals("wait(J") || mname.equals("park(ZJ"), "timedout thread outside timeout method: " + mi.getFullName());
    }
    
    if (lockRef != -1){
      // object we are blocked on has to exist
      ElementInfo ei = this.getElementInfo(lockRef);
      checkAssertion( ei != null, "thread locked on non-existing object: " + lockRef);
      
      // we have to be in the lockedThreads list of that objects monitor
      checkAssertion( ei.isLocking(this), "thread blocked on non-locking object: " + ei);
      
      // can't be blocked on a lock we own (but could be in waiting before giving it up)
      if (!isWaiting() && lockedObjects != null && !lockedObjects.isEmpty()){
        for (ElementInfo lei : lockedObjects){
            checkAssertion( lei.getObjectRef() != lockRef, "non-waiting thread blocked on owned lock: " + lei);
        }
      }
      
      // the state has to be BLOCKED, NOTIFIED, WAITING or TIMEOUT_WAITING
      checkAssertion( isWaiting() || isBlockedOrNotified(), "locked thread not waiting, blocked or notified");
      
    } else { // no lockRef set
      checkAssertion( !isWaiting() && !isBlockedOrNotified(), "non-locked thread is waiting, blocked or notified");
    }
    
    // if we have locked objects, we have to be the locking thread of each of them
    if (lockedObjects != null && !lockedObjects.isEmpty()){
      for (ElementInfo ei : lockedObjects){
        ThreadInfo lti = ei.getLockingThread();
        if (lti != null){
          checkAssertion(lti == this, "not the locking thread for locked object: " + ei + " owned by " + lti);
        } else {
          // can happen for a nested monitor lockout
        }
      }
    }

    if (!isStore){
      // all stack frames have to be set to unchanged
      for (StackFrame f = top; f != null; f = f.getPrevious()){
        checkAssertion( !f.hasChanged(), "changed stackframe upon restore: " + f);
      }
    }
  }
  
  protected void checkAssertion(boolean cond, String failMsg){
    if (!cond){
      System.out.println("!!!!!! failed thread consistency: "  + this + ": " + failMsg);
      vm.dumpThreadStates();
      assert false;
    }
  }
}
