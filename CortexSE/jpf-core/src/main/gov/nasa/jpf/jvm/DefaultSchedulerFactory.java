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
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;


/**
 * the general policy is that we only create Thread CGs here (based on their
 * status), but we don't change any thread or lock status. This has to happen
 * in the instruction before calling the factory
 */

public class DefaultSchedulerFactory implements SchedulerFactory {

  protected JVM vm;
  protected SystemState ss;

  //--- those are configured
  boolean breakStart;
  boolean breakYield;
  boolean breakSleep;
  boolean breakArrayAccess;
  boolean breakSingleChoice;

  public DefaultSchedulerFactory (Config config, JVM vm, SystemState ss) {
    this.vm = vm;
    this.ss = ss;

    breakStart = config.getBoolean("cg.threads.break_start", false);
    breakYield = config.getBoolean("cg.threads.break_yield", false);
    breakSleep = config.getBoolean("cg.threads.break_sleep", false);

    breakArrayAccess = config.getBoolean("cg.threads.break_arrays", false);
    breakSingleChoice = config.getBoolean("cg.break_single_choice");
  }

  /*************************************** internal helpers *****************/

  /**
   * post process a list of choices. This is our primary interface towards
   * subclasses (together with overriding the relevant ainsn APIs
   */
  protected ThreadInfo[] filter ( ThreadInfo[] list) {
    // we have nothing to do, but subclasses can use it to
    // shuffle the order (e.g. to avoid the IdleFilter probblem),
    // or to filter out the top priorities
    return list;
  }

  protected ChoiceGenerator<ThreadInfo> getRunnableCG (String id) {
    ThreadInfo[] choices = getRunnablesIfChoices();
    if (choices != null) {
      return new ThreadChoiceFromSet( id, choices, true);
    } else {
      return null;
    }
  }

  protected ChoiceGenerator<ThreadInfo> getSyncCG (String id, ElementInfo ei, ThreadInfo ti) {
    return getRunnableCG(id);
  }


  /**************************************** our choice acquisition methods ***/

  /**
   * get list of all runnable threads
   */
  protected ThreadInfo[] getRunnables() {
    ThreadList tl = vm.getThreadList();
    return filter(tl.getRunnableThreads());
  }

  /**
   * return a list of runnable choices, or null if there is only one
   */
  protected ThreadInfo[] getRunnablesIfChoices() {
    ThreadList tl = vm.getThreadList();
    int n = tl.getRunnableThreadCount();

    if ((n > 1) || (n == 1 && breakSingleChoice)){
      return filter(tl.getRunnableThreads());
    } else {
      return null;
    }
  }

  protected ThreadInfo[] getRunnablesWith (ThreadInfo ti) {
    ThreadList tl = vm.getThreadList();
    return filter( tl.getRunnableThreadsWith(ti));
  }

  protected ThreadInfo[] getRunnablesWithout (ThreadInfo ti) {
    ThreadList tl = vm.getThreadList();
    return filter( tl.getRunnableThreadsWithout(ti));
  }


  /************************************ the public interface towards the insns ***/

  public ChoiceGenerator<ThreadInfo> createSyncMethodEnterCG (ElementInfo ei, ThreadInfo ti) {
    return createMonitorEnterCG(ei, ti, true);
  }

  public ChoiceGenerator<ThreadInfo> createSyncMethodExitCG (ElementInfo ei, ThreadInfo ti) {
    return null; // nothing, left mover
  }

  public ChoiceGenerator<ThreadInfo> createMonitorEnterCG (ElementInfo ei, ThreadInfo ti) {
    return createMonitorEnterCG(ei, ti, false);
  }

  protected ChoiceGenerator<ThreadInfo> createMonitorEnterCG(ElementInfo ei,
                                                             ThreadInfo ti,
                                                             boolean isMethodCall) {
    
    if (ti.isBlocked()) { // we have to return something
      if (ss.isAtomic()) {
        ss.setBlockedInAtomicSection();
      }

      return new ThreadChoiceFromSet("monitorEnter", getRunnables(), true);

    } else {
      if (ss.isAtomic()) {
        return null;
      }

      return getSyncCG( "monitorEnter", ei, ti);
    }
  }

  public ChoiceGenerator<ThreadInfo> createMonitorExitCG (ElementInfo ei, ThreadInfo ti) {

    /**
    if (!ss.isAtomic()){
      return getSyncCG( "monitorExit", ei, ti);
    }
    **/

    return null; // nothing, left mover
  }


  public ChoiceGenerator<ThreadInfo> createWaitCG (ElementInfo ei, ThreadInfo ti, long timeOut) {
    if (ss.isAtomic()) {
      ss.setBlockedInAtomicSection();
    }

    return new ThreadChoiceFromSet( "wait", getRunnables(), true);
  }

  public ChoiceGenerator<ThreadInfo> createNotifyCG (ElementInfo ei, ThreadInfo ti) {
    if (ss.isAtomic()) {
      return null;
    }

    ThreadInfo[] waiters = ei.getWaitingThreads();
    if (waiters.length < 2) {
      // if there are less than 2 threads waiting, there is no nondeterminism
      return null;
    } else {
      return new ThreadChoiceFromSet( "notify", waiters, false);
    }
  }

  public ChoiceGenerator<ThreadInfo> createNotifyAllCG (ElementInfo ei, ThreadInfo ti) {
    return null; // no choice here
  }

  public ChoiceGenerator<ThreadInfo> createSharedFieldAccessCG (ElementInfo ei, ThreadInfo ti) {
    if (ss.isAtomic()) {
      return null;
    }

    return getSyncCG( "sharedField", ei, ti);
  }

  public ChoiceGenerator<ThreadInfo> createParkCG (ElementInfo ei, ThreadInfo tiPark, boolean isAbsoluteTime, long timeOut){
    // we treat this like a wait, but don't differentiate between absolute and relative timeout. Note it has to be a right mover
    // note that tiPark is already blocked at this point
    if (ss.isAtomic()) {
      ss.setBlockedInAtomicSection();
    }

    return new ThreadChoiceFromSet( "park", getRunnables(), true);
  }
  
  public ChoiceGenerator<ThreadInfo> createUnparkCG (ThreadInfo tiUnparked) {
    // note that tiUnparked is already runnable at this point
    return getRunnableCG("unpark");
  }
  
  public ChoiceGenerator<ThreadInfo> createSharedArrayAccessCG (ElementInfo ei, ThreadInfo ti) {
    // the array object (ei) is shared, otherwise we won't get here

    if (breakArrayAccess) {
      if (ss.isAtomic()) {
        return null;
      }

      /**
      // <2do> CG sequence based POR should be optional
      ArrayInstruction ainsn = (ArrayInstruction)ti.getPC();
      boolean isRead = ainsn.isRead();
      int aref = ei.getThreadInfoForId();

      for (ChoiceGenerator<?> cg = ss.getChoiceGenerator(); cg != null; cg = cg.getPreviousChoiceGenerator()){
      if (cg.getThreadInfoForId() != ti || cg.getChoiceType() != ThreadInfo.class){
      break; // different thread or different choice type -> we need a CG
      }

      Instruction cgInsn = cg.getInsn();
      if (!(cgInsn instanceof ArrayInstruction)){
      break; // not an aload/astore -> we need a CG
      }
      ArrayInstruction cgAinsn = (ArrayInstruction)cgInsn;
      // this is only an approximation since the array ref stored in the insn
      // might have changed. Note this only works if the insn arrayRef is
      // stored AFTER this gets executed
      if (cgAinsn.getArrayRef(ti) != aref){
      break;
      }

      if (cgAinsn.isRead() == isRead){
      return null; // same op on same array in same thread -> no new CG required
      }

      // if we get here, this is a complement op on the same array in the same thread
      // -> skip over all prev. CG insns of the same type
      }
       **/
      ChoiceGenerator<ThreadInfo> cg = getSyncCG( "sharedArray", ei, ti);
      return cg;

    } else {
      return null;
    }
  }

  public ChoiceGenerator<ThreadInfo> createThreadStartCG (ThreadInfo newThread) {
    // NOTE if newThread is sync and blocked, it already will be blocked
    // before this gets called

    // we've been going forth & back a number of times with this. The idea was
    // that it would be more intuitive to see a transition break every time we
    // start a new thread, but this causes significant state bloat in case of
    // pure starter threads, i.e. something that simply does
    //   ...
    //   t.start();
    //   return
    //
    // because we get a state branch at the "t.start()" and the "return".
    // It should be safe to go on, since the important thing is to set the new thread
    // runnable.

    if (breakStart) {
      if (ss.isAtomic()) {
        // this is dangerous - POR now depends on Thread.start() being a right mover
        // If the current thread doesn't have a scheduling point before termination,
        // we might loose paths (Thread.start() will warn about it)
        return null;
      }
      return getRunnableCG("start");

    } else {
      return null;
    }
  }

  public ChoiceGenerator<ThreadInfo> createBeginAtomicCG (ThreadInfo atomicThread) {
    return null;
  }

  public ChoiceGenerator<ThreadInfo> createEndAtomicCG (ThreadInfo atomicThread) {
    return getRunnableCG("end atomic");
  }


  public ChoiceGenerator<ThreadInfo> createThreadYieldCG (ThreadInfo yieldThread) {
    if (breakYield) {
      if (ss.isAtomic()) {
        return null;
      }
      return getRunnableCG("yield");

    } else {
      return null;
    }
  }

  public ChoiceGenerator<ThreadInfo> createInterruptCG (ThreadInfo interruptedThread) {
    if (ss.isAtomic()) {
      return null;
    }

    return getRunnableCG("interrupt");
  }

  public ChoiceGenerator<ThreadInfo> createThreadSleepCG (ThreadInfo sleepThread, long millis, int nanos) {
    if (breakSleep) {
      if (ss.isAtomic()) {
        return null;
      }

      // we treat this as a simple reschedule
      return getRunnableCG("sleep");

    } else {
      return null;
    }
  }

  public ChoiceGenerator<ThreadInfo> createThreadTerminateCG (ThreadInfo terminateThread) {
    // terminateThread is already TERMINATED at this point
    ThreadList tl = vm.getThreadList();

    // NOTE returning null does not directly define an end state - that's up to
    // a subsequent call to vm.isEndState()
    // <2do> FIXME this is redundant and error prone
    if (tl.hasAnyAliveThread()) {
      return new ThreadChoiceFromSet( "terminate", getRunnablesWithout(terminateThread), true);
    } else {
      return null;
    }
  }

  public ChoiceGenerator<ThreadInfo> createThreadSuspendCG () {
    return getRunnableCG("suspend");
  }

  public ChoiceGenerator<ThreadInfo> createThreadResumeCG () {
    return getRunnableCG("resume");
  }

  public ChoiceGenerator<ThreadInfo> createThreadStopCG () {
    return null; // left mover, there will be still a terminateCG
    //return getRunnableCG("stop");
  }
}
