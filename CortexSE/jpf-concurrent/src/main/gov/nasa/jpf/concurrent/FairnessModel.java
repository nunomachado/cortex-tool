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

import gov.nasa.jpf.concurrent.version.FairnessVersion;
import gov.nasa.jpf.jvm.ThreadInfo;


/*
 * This class is the heart of the AQS,ReentrantLock,Semaphore,... model.
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public abstract class FairnessModel extends ThreadModel {

  public boolean acquireUninterruptibly(int permits) {
    if (checkIsNegative(permits)) return false;
    if(thread.isFirstStepInsn()) {
      if (checkIfUnblock()) return false;
    } else {
      if (getCurrentVersion().isFair()) {
        if(acquireRequirement(permits) && fairAcquire(permits)) {
          saveVersion();
          return true;
        } else {
          thread.isInterrupted(true);
          addAndPark(permits,0);
          saveVersion();
          return false;
        }
      } else {
        if(acquireRequirement(permits) && nonFairAcquire(permits)) {
          saveVersion();
          return true;
        } else {
          thread.isInterrupted(true);
          addAndPark(permits,0);
          saveVersion();
          return false;
        }
      }
    }
    return false;
  }

  public boolean tryAcquire(int permits) {
    if(checkIsNegative(permits)) return false;
    if(acquireRequirement(permits)) {
      boolean r = nonFairAcquire(permits);
      saveVersion();
      return r;
    }
    return false;
  }

  public boolean tryAcquire(int permits, long timeout) {
    if (!checkNotInterrupted()) return false;
    if(checkIsNegative(permits)) return false;
    if(timeout == 0 && getCurrentVersion().isFair()) {
      if(acquireRequirement(permits) && fairAcquire(permits)) {
        saveVersion();
        return true;
      } else {
        addAndPark(permits, timeout);
        saveVersion();
        return false;
      }
    } else {
      if(acquireRequirement(permits) && nonFairAcquire(permits)) {
        saveVersion();
        return true;
      } else {
        addAndPark(permits, timeout);
        saveVersion();
        return false;
      }
    }
  }

  protected boolean releasePermit(int permits) {
    int current = getCurrentVersion().getState();
    current += permits;
    getCurrentVersion().setState(current);
    if(getCurrentVersion().getQueuedThreads().size() > 0) {
      ThreadInfo t = getCurrentVersion().getQueuedThreads().get(0);
      if(releaseRequirement(t)) {
        dequeueLongestWaitingThread();
        return true;
      }
    }
    getCurrentVersion().setLastRemoved(null);
    return false;
  }

  public abstract boolean acquireRequirement(int permits);

  public abstract boolean releaseRequirement(ThreadInfo t);

  /*
   * It's almost the same as Nonfair version the only difference is that we
   * check if we were on the head of the queue before acquiring a lock.
   * Explanation goes as follows: Let's assume that we have 3 threads. First
   * thread acquires lock, second one waits in queue to acquire the third one is
   * not interested in acquiring lock(for now). First thread decides to stop
   * holding lock which results in waking up second thread which will be able to
   * acquire. But before second thread will acquire lock, third thread decides
   * that he want to acquire a lock and in the end second thread goes again to
   * waiting queue. This can cause a starvation of second thread and for sure
   * it's not fair. As we can see by checking that thread were at the head of
   * the queue, third thread will not be able to acquire lock before second
   * thread.
   * 
   */

  protected boolean fairAcquire(int permits) {
    if (getCurrentVersion().getLastRemoved() != null) {
      if (getCurrentVersion().getLastRemoved() == thread) {
        return nonFairAcquire(permits);
      } else {
        return false;
      }
    } else {
      return nonFairAcquire(permits);
    }
  }


  protected abstract boolean nonFairAcquire(int permits);

  public boolean isFair() {
    return getCurrentVersion().isFair();
  }

  public void setFair(boolean f) {
    getCurrentVersion().setFair(f);
    saveVersion();
  }

  protected abstract FairnessVersion getCurrentVersion();

}
