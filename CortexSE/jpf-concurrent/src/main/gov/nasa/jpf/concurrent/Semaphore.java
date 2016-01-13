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

import gov.nasa.jpf.concurrent.version.SemaphoreVersion;
import gov.nasa.jpf.concurrent.version.Version;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.ThreadInfo;


/*
 * This class is the heart of the Semaphore model.
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class Semaphore extends FairnessModel {

  public void newSemaphore(int permits, boolean fair) {
    getCurrentVersion().setFair(fair);
    getCurrentVersion().setState(permits);
    saveVersion();
  }

  public boolean acquire(int permits) {
    if (checkIsNegative(permits)) return false;
    if (!checkNotInterrupted()) {
      return false;
    }
    if(thread.isFirstStepInsn()) {
      if (checkIfUnblock()) return false;  
    } else {
      return acquireUninterruptibly(permits);
    }
    return false;
  }

  public void release(int permits) {
    releasePermit(permits);
    saveVersion();
  }

  public boolean acquireRequirement(int permits) {
    return permits <= getCurrentVersion().getState();
  }

  public boolean releaseRequirement(ThreadInfo t) {
    int permits = getCurrentVersion().getPermitByThread(t);
    return permits <= getCurrentVersion().getState();
  }

  protected boolean nonFairAcquire(int permits) {
    int current = getCurrentVersion().getState();
    int newPermits = current - permits;
    getCurrentVersion().setState(newPermits);
    return true;
  }

  protected void addAndPark(int permits,long timeout) {
    if (!getCurrentVersion().getQueuedThreads().contains(thread)) {
      getCurrentVersion().addThreadToQueue(thread);
    }
    getCurrentVersion().addThreadToQueue(thread, permits);
    park(timeout);
  }

  public int availablePermits() {
    return getCurrentVersion().getState();
  }

  public void reducePermits(int reduction) {
    if(checkIsNegative(reduction)) return;
    int current = getCurrentVersion().getState();
    getCurrentVersion().setState(current - reduction);
    saveVersion();
  }

  public Version newVersionInstance() {
    return new SemaphoreVersion();
  }

  public Version newVersionInstance(Version v) {
    return new SemaphoreVersion(v);
  }

  public Semaphore doClone() {
    return (Semaphore)doClone(new Semaphore());
  }

  public static Semaphore getSemaphore(MJIEnv env, int objRef, int version) {
    Semaphore s = (Semaphore) getModel(env, objRef);
    if (s == null) {
      s = new Semaphore();
      addModel(objRef, s);
    }
    s = (Semaphore) initModel(env, objRef, version, s);
    return s;
  }

  protected SemaphoreVersion getCurrentVersion() {
    return (SemaphoreVersion) currentVersion;
  }
}
