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

import gov.nasa.jpf.concurrent.version.Version;
import gov.nasa.jpf.concurrent.version.ReentrantLockVersion;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.ThreadInfo;

/*
 * This class is the heart of the ReentrantLock model. It was implemented to be similar
 * to the Sync class that resides inside the original Java JDK.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class ReentrantLock extends FairnessModel {

  /*
   * For this method tricky part is the beginning. If we are waiting for a lock
   * and we will be interrupted this method will be reexecuted so firstly we
   * need to clear our interrupt flag and secondly we need to remove ourself
   * from waiting queue, if we wont do that and we will not acquire lock we will
   * end up with two the same objects in queuedThreads list.
   */
  
  public boolean lock() {
    if(thread.isFirstStepInsn()) {
      if (checkIfUnblock()) {
        return false;
      }
    } else {
      thread.isInterrupted(true);
      if(alreadyOwned()) return true;
      return acquireUninterruptibly(1);
    }
    return false;
  }

  public boolean acquireInterruptibly() {
    if (checkNotInterrupted()) return acquireUninterruptibly(1);
    return false;
  }

  /*
   * Releases a lock
   */
  public void unlock() {
    if (isOwner()) {
      if(getCurrentVersion().getState() == 1) getCurrentVersion().setOwner(null);
      releasePermit(-1);
      saveVersion();
    } else {
      env.throwException("java.lang.IllegalMonitorStateException");
    }
  }

  public boolean tryLock() {
    if(alreadyOwned()) return true;
    if (isLocked()) return false;

    tryAcquire(1);
    return true;
  }

  public boolean tryLock(long timeout, int unit) {
    if (!checkNotInterrupted()) return false;
    if(alreadyOwned()) return true;

    return tryAcquire(1, timeout);
  }

  public boolean acquireRequirement(int permits) {
    return getState() == 0 && getCurrentVersion().getOwner() == null;
  }
  
  public boolean releaseRequirement(ThreadInfo t) {
    return acquireRequirement(0);
  }


  protected boolean nonFairAcquire(int permits) {
    getCurrentVersion().removeThreadFromQueue(thread);
    getCurrentVersion().setState(1);
    stateChange();
    getCurrentVersion().setOwner(thread);
    return true;
  }

  public int getHoldCount() {
    if (isOwner()) {
      return getState();
    } else {
      return 0;
    }
  }

  public int getOwner() {
    if (getCurrentVersion().getOwner() != null) {
      return getCurrentVersion().getOwner().getThreadObjectRef();
    } else {
      return MJIEnv.NULL;
    }
  }

  public boolean isHeldByCurrentThread() {
    return isOwner();
  }

  public boolean hasQueuedThread(int thread) {
    return isQueued(thread);
  }

  public boolean isLocked() {
    return getState() > 0;
  }

  public ReentrantLock doClone() {
    return (ReentrantLock)doClone(new ReentrantLock());
  }

  private boolean alreadyOwned() {
    if (isOwner()) {
      setState(getState() + 1);
      stateChange();
      saveVersion();
      return true;
    } else {
      return false;
    }
  }

  private boolean isOwner() {
    if (getCurrentVersion().getOwner() != null && getCurrentVersion().getOwner().equals(thread)) {
      return true;
    } else {
      return false;
    }
  }

  private void stateChange() {
    int aqsRef = env.getReferenceField(objRef, "s");
    int aqsVer = env.getIntField(aqsRef, "version");
    AQS aqs = AQS.getAQS(env, aqsRef, aqsVer);
    aqs.setState(getCurrentVersion().getState());
  }

  public Version newVersionInstance() {
    return new ReentrantLockVersion();
  }

  public Version newVersionInstance(Version v) {
    return new ReentrantLockVersion(v);
  }

  public static ReentrantLock getReentrantLock (MJIEnv env, int objRef, int version) {
    ReentrantLock s = (ReentrantLock)getModel(env,objRef);
    if (s == null) {
      s = new ReentrantLock();
      addModel(objRef, s);
    }
    s = (ReentrantLock)initModel(env, objRef, version, s);
    return s;
  }

  protected ReentrantLockVersion getCurrentVersion() {
    return (ReentrantLockVersion) currentVersion;
  }

  protected void addAndPark(int permits, long timeout) {
    if (!getCurrentVersion().getQueuedThreads().contains(thread)) {
      getCurrentVersion().addThreadToQueue(thread);
    }
    park(timeout);
  }

}
