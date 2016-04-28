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

/**
 * interface of factory object that creates all ChoiceGenerators required
 * to implement scheduling strategies
 */
public interface SchedulerFactory {

  /** used by InvokeInstructions of sync methods */
  ChoiceGenerator<ThreadInfo> createSyncMethodEnterCG(ElementInfo ei, ThreadInfo ti);
  
  /** user by Returns of sync methods */
  ChoiceGenerator<ThreadInfo> createSyncMethodExitCG(ElementInfo ei, ThreadInfo ti);
  
  /** used by MonitorEnter insns */
  ChoiceGenerator<ThreadInfo> createMonitorEnterCG(ElementInfo ei, ThreadInfo ti);
  
  /** used by MonitorExit insns */
  ChoiceGenerator<ThreadInfo> createMonitorExitCG(ElementInfo ei, ThreadInfo ti);
  
  /** used by Object.wait() */
  ChoiceGenerator<ThreadInfo> createWaitCG (ElementInfo ei, ThreadInfo ti, long timeOut);
  
  /** used by Object.notify() */
  ChoiceGenerator<ThreadInfo> createNotifyCG(ElementInfo ei, ThreadInfo ti);

  /** used by sun.misc.Unsafe.park() */
  ChoiceGenerator<ThreadInfo> createParkCG (ElementInfo ei, ThreadInfo tiPark, boolean isAbsoluteTime, long timeOut);
    
  /** used by sun.misc.Unsafe.unpark() */
  ChoiceGenerator<ThreadInfo> createUnparkCG (ThreadInfo tiUnparked);
  
  /** used by Object.notifyAll() */
  ChoiceGenerator<ThreadInfo> createNotifyAllCG(ElementInfo ei, ThreadInfo ti);

  /** used by GetField,PutField,GetStatic,PutStatic insns of shared objects */
  ChoiceGenerator<ThreadInfo> createSharedFieldAccessCG(ElementInfo ei, ThreadInfo ti);
  
  /** used from ArrayInstruction (various array element access insns) */
  ChoiceGenerator<ThreadInfo> createSharedArrayAccessCG (ElementInfo eiArray, ThreadInfo ti);
  
  /** used by Thread.start() */
  ChoiceGenerator<ThreadInfo> createThreadStartCG (ThreadInfo newThread);
  
  /** used by Thread.yield() */
  ChoiceGenerator<ThreadInfo> createThreadYieldCG (ThreadInfo yieldThread);
  
  /** used by Thread.sleep() */
  ChoiceGenerator<ThreadInfo> createThreadSleepCG (ThreadInfo sleepThread, long millis, int nanos);

  /** used by Thread.interrupt() */
  public ChoiceGenerator<ThreadInfo> createInterruptCG (ThreadInfo interruptedThread);

  /** used by Return from run() */
  ChoiceGenerator<ThreadInfo> createThreadTerminateCG (ThreadInfo terminatedThread);

  /** used by Thread.suspend() */
  ChoiceGenerator<ThreadInfo> createThreadSuspendCG ();

  /** used by Thread.resume() */
  ChoiceGenerator<ThreadInfo> createThreadResumeCG ();

  /** used by Thread.stop() */
  ChoiceGenerator<ThreadInfo> createThreadStopCG ();

  ChoiceGenerator<ThreadInfo> createBeginAtomicCG (ThreadInfo ti);

  ChoiceGenerator<ThreadInfo> createEndAtomicCG (ThreadInfo ti);
}
