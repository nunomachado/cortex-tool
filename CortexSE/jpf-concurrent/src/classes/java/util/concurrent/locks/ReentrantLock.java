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
package java.util.concurrent.locks;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/*
 * Non MJI level class for modeling ReentrantLock.
 * It's here for two reasons, lock() and tryLock() methods
 * cannot be fully implemented in MJI level
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class ReentrantLock implements Lock, java.io.Serializable {

  private int version = 0;

  private ReentrantLockSync s = new ReentrantLockSync();

  public ReentrantLock () {
    setFair(false);
  }

  public ReentrantLock (boolean fair) {
    setFair(fair);
  }

  public void lock () {
    boolean interrupted = Thread.currentThread().isInterrupted();
    while (!nativeLock()) {
      interrupted = interrupted || Thread.currentThread().isInterrupted();
    }
      ;
    if(interrupted) Thread.currentThread().interrupt();
  }

  public void lockInterruptibly () throws InterruptedException{
    while(!nativeLockInterruptibly());
  }

  public boolean tryLock (long timeout, TimeUnit unit) throws InterruptedException {
    nativeTryLock(timeout, unit);
    nativeRemoveFromQueue();
    return isHeldByCurrentThread();
  }

  public boolean hasWaiters (Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) throw new IllegalArgumentException("not owner");
    if (!((AbstractQueuedSynchronizer.ConditionObject) condition).isOwnedBy(s)) throw new IllegalArgumentException("not owner");
    return ((AbstractQueuedSynchronizer.ConditionObject) condition).hasWaiters();
  }

  public int getWaitQueueLength (Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) throw new IllegalArgumentException("not owner");
    if (!((AbstractQueuedSynchronizer.ConditionObject) condition).isOwnedBy(s)) throw new IllegalArgumentException("not owner");
    return ((AbstractQueuedSynchronizer.ConditionObject) condition).getWaitQueueLength();
  }

  protected Collection<Thread> getWaitingThreads (Condition condition) {
    if (condition == null) throw new NullPointerException();
    if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) throw new IllegalArgumentException("not owner");
    if (!((AbstractQueuedSynchronizer.ConditionObject) condition).isOwnedBy(s)) throw new IllegalArgumentException("not owner");
    return ((AbstractQueuedSynchronizer.ConditionObject) condition).getWaitingThreads();
  }

  public String toString () {
    Thread o = getOwner();
    return super.toString() + ((o == null) ? "[Unlocked]" : "[Locked by thread " + o.getName() + "]");
  }

  private native boolean nativeLock ();

  private native boolean nativeLockInterruptibly();

  private native void nativeTryLock (long timeout, TimeUnit unit);

  private native void nativeRemoveFromQueue ();

  protected native Collection<Thread> getQueuedThreads ();

  protected native Thread getOwner ();

  public native void unlock ();

  public native int getHoldCount ();

  public native boolean isHeldByCurrentThread ();

  public native boolean isLocked ();

  public final native boolean hasQueuedThreads ();

  public final native boolean hasQueuedThread (Thread thread);

  public final native int getQueueLength ();

  public final native boolean isFair ();

  private native void setFair(boolean f);

  public native boolean tryLock ();

  public Condition newCondition () {
    return s.newCondition();
  }

  private class ReentrantLockSync extends AbstractQueuedSynchronizer {

    public Condition newCondition() {
      return new AbstractQueuedSynchronizer.ConditionObject();
    }

    public boolean isHeldExclusively() {
      return isHeldByCurrentThread();
    }

    public boolean tryRelease(int arg) {
      while (isLocked()) {
        unlock();
      }
      return true;
    }

    public boolean tryAcquire(int arg) {
      while(arg > 0) {
        lock();
        arg--;
      }
      return true;
    }

  }
}
