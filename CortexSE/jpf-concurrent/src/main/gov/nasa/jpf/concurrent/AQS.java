//
// Copyright (C) 2008 United States Government as represented by the
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
package gov.nasa.jpf.concurrent;


import gov.nasa.jpf.concurrent.version.AQSVersion;
import gov.nasa.jpf.concurrent.version.Version;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.MJIEnv;

import java.util.ArrayList;
import java.util.List;

/*
 * This class is the heart of the AQS model.
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class AQS extends ThreadModel {

  public void queueThread(long timeout,boolean isShared) {
    if(thread.isFirstStepInsn()) {
      checkIfUnblock();
    } else {
      //clearing interrupted flag, interruption is handled on class level
      thread.isInterrupted(true);
      addAndPark(1, timeout,isShared);
      saveVersion();
    }
  }

  public boolean queueThreadInteruptibly(long timeout,boolean isShared) {
    if(thread.isFirstStepInsn()) {
      if(thread.isTimedOut()) {
        park(0);
        saveVersion();
        return false;
      }
      if(!checkNotInterrupted()) {
        saveVersion();
        return false;
      }
    } else {
      queueThread(timeout,isShared);
    }
    return true;
  }

  public void dequeueFirstThread() {
    dequeueLongestWaitingThread();
    saveVersion();
  }

  public boolean compareAndSetState(int expect,int update) {
    if(getCurrentVersion().getState() == expect) {
      getCurrentVersion().setState(update);
      saveVersion();
      return true;
    }
    return false;
  }

  public int getFirstQueuedThread() {
    if(getCurrentVersion().getQueuedThreads().size() == 0) return MJIEnv.NULL;
    return getCurrentVersion().getQueuedThreads().get(0).getThreadObjectRef();
  }

  public boolean hasContended() {
    return getCurrentVersion().getEverBlocked();
  }

  public int getExclusiveQueuedThreads() {
    List<ThreadInfo> threadList = new ArrayList<ThreadInfo>();
    for(int i=0;i<getCurrentVersion().getQueuedThreads().size();i++) {
      ThreadInfo t = getCurrentVersion().getQueuedThreads().get(i);
      if(!getCurrentVersion().isShared(t)) {
        threadList.add(t);
      }
    }
    return createQueueWithThreads(env, thread, threadList);
  }

  public int getSharedQueuedThreads() {
    List<ThreadInfo> threadList = new ArrayList<ThreadInfo>();
    for(int i=0;i<getCurrentVersion().getQueuedThreads().size();i++) {
      ThreadInfo t = getCurrentVersion().getQueuedThreads().get(i);
      if(getCurrentVersion().isShared(t)) {
        threadList.add(t);
      }
    }
    return createQueueWithThreads(env, thread, threadList);
  }

  public boolean apparentlyFirstQueuedIsExclusive() {
    if(getCurrentVersion().getQueuedThreads().size() == 0) return false;
    ThreadInfo t = getCurrentVersion().getQueuedThreads().get(0);
    return !getCurrentVersion().isShared(t);
  }

  /*
   * Maybe there is an error in ReentrantReadWriteLock(SDK version) but for now it will
   * be very hard to find it so we will stick to previous version.
  */
  public boolean hasQueuedPredecessors() {
    return false;
  }

  public AQS doClone() {
    return (AQS)doClone(new AQS());
  }

  private void addAndPark(int permits, long timeout,boolean isShared) {
    getCurrentVersion().addThreadToQueue(thread, isShared);
    addAndPark(permits, timeout);
  }

  protected void addAndPark(int permits, long timeout) {
    getCurrentVersion().setEverBlocked(true);
    park(timeout);
  }

  public Version newVersionInstance () {
    return new AQSVersion();
  }

  public Version newVersionInstance (Version v) {
    return new AQSVersion(v);
  }

  public static AQS getAQS (MJIEnv env, int objRef, int version) {
    AQS s = (AQS)getModel(env,objRef);
    if (s == null) {
      s = new AQS();
      addModel(objRef, s);
    }
    s = (AQS)initModel(env, objRef, version, s);
    return s;
  }

  protected AQSVersion getCurrentVersion() {
    return (AQSVersion)currentVersion;
  }
}
