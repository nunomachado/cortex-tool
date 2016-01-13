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
import gov.nasa.jpf.concurrent.version.ConditionVersion;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.ThreadInfo;
import java.util.List;


/*
 * Implements logic for ConditionObject.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class Condition extends ThreadModel {

  /*
   * Methods works the same as their counterparts from SDK so please go there
   * for documentation.
   * 
   * The only different thing it's a way how we acquire lock after signaling.
   * After signaling acquiring lock is made at non-MJI level, not at MJI level.
   */
  
  public boolean await(long timeout) {
    getCurrentVersion().removeRecentlySignalled(thread);
    if(!checkNotInterrupted()) return false;
    if(!thread.isFirstStepInsn()) {
      addAndPark(1, timeout);
      saveVersion();
      return false;
    } else {
      if(thread.isTimedOut()) {
        park(0);
        return false;
      } else {
        return true;
      }
    }
  }

  public boolean awaitUninterruptibly() {
    if(thread.isFirstStepInsn()) {
      if(getCurrentVersion().isRecentlySignalled(thread)) {
        getCurrentVersion().removeRecentlySignalled(thread);
        checkIfUnblock();
        saveVersion();
        return true;
      }
    } else {
      thread.isInterrupted(true);
      addAndPark(1, 0);
      saveVersion();
      return false;
    }
    return false;
  }

  public boolean signal() {
    if (getCurrentVersion().getQueuedThreads().size() > 0) {
      removeAndUnpark(getCurrentVersion().getQueuedThreads().get(0));
      saveVersion();
      return true;
    }
    return false;
  }

  public boolean signalAll() {
    if (getCurrentVersion().getQueuedThreads().size() > 0) {
      List<ThreadInfo> queuedThreads = getCurrentVersion().getQueuedThreads();
      for (int i = 0; i < queuedThreads.size(); i++) {
        removeAndUnpark(queuedThreads.get(i));
      }
      saveVersion();
      return true;
    }
    return false;
  }

  public int getWaitQueueLength() {
    return getQueueLength();
  }

  public int getWaitingThreads() {
    return getQueuedThreads();
  }

  public boolean hasWaiters() {
    return getWaitQueueLength() > 0;
  }

  protected void addAndPark(int permits, long timeout) {
    getCurrentVersion().addThreadToQueue(thread);
    park(timeout);
  }

  private void removeAndUnpark(ThreadInfo t) {
    getCurrentVersion().removeThreadFromQueue(t);
    getCurrentVersion().addRecentlySignalled(t);
    unpark(t.getThreadObjectRef());
  }

  public Condition doClone() {
    return (Condition)doClone(new Condition());
  }

  public Version newVersionInstance() {
    return new ConditionVersion();
  }

  public Version newVersionInstance(Version v) {
    return new ConditionVersion(v);
  }

  public static Condition getCondition (MJIEnv env, int objRef, int version) {
    Condition c = (Condition)getModel(env,objRef);
    if (c == null) {
      c = new Condition();
      addModel(objRef, c);
    }
    c = (Condition)initModel(env, objRef, version, c);
    return c;
  }

  protected ConditionVersion getCurrentVersion() {
    return (ConditionVersion)currentVersion;
  }

}
