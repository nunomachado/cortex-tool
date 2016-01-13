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
import java.util.Date;
import java.util.concurrent.TimeUnit;

/*
 *  AbstractQueuedSynchronizer model
 *
 *  @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer{

  private int version = 0;

  protected native final int getState();

  protected native final void setState(int newState);

  protected native final boolean compareAndSetState(int expect, int update);

  protected boolean tryAcquire(int arg) {
    throw new UnsupportedOperationException();
  }

  protected boolean tryRelease(int arg) {
    throw new UnsupportedOperationException();
  }

  protected int tryAcquireShared(int arg) {
    throw new UnsupportedOperationException();
  }

  protected boolean tryReleaseShared(int arg) {
    throw new UnsupportedOperationException();
  }

  protected boolean isHeldExclusively() {
    throw new UnsupportedOperationException();
  }

  public final void acquire(int arg) {
    boolean interrupted = Thread.currentThread().isInterrupted();
    while(!tryAcquire(arg)) {
      queueThread(false);
      interrupted = interrupted || Thread.currentThread().isInterrupted();
    }
    if(interrupted) Thread.currentThread().interrupt();
  }

  public final void acquireInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    while(!tryAcquire(arg)) {
      queueThreadInterruptibly(0,false);
    }
  }

  public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    while(!tryAcquire(arg)) {
      if(!queueThreadInterruptibly(nanosTimeout,false)) {
        return false;
      }
    }
    return true;
  }

  public final boolean release(int arg) {
    if (tryRelease(arg)) {
      dequeueFirstThread();
      return true;
    }
    return false;
  }

  public final void acquireShared(int arg) {
    while(tryAcquireShared(arg) < 0) {
      queueThread(true);
    }
  }

  public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    while(tryAcquireShared(arg) < 0) {
      queueThreadInterruptibly(0,true);
    }
  }

  public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    while(tryAcquireShared(arg) < 0) {
      if(!queueThreadInterruptibly(nanosTimeout,true))
        return false;
    }
    return true;
      
  }

  public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
      dequeueFirstThread();
      return true;
    }
    return false;
  }

  public native void queueThread(boolean isShared);

  public native boolean queueThreadInterruptibly(long timeout,boolean isShared);

  public native void dequeueFirstThread();

  public native void doAcquireInterruptibly(int arg);

  public native boolean doAcquireNanos(int arg, long nanosTimeout);

  public native void doAcquireShared(int arg);

  public native void doAcquireSharedInterruptibly(int arg);

  public native boolean doAcquireSharedNanos(int arg, long nanosTimeout);

  public native void doReleaseShared();

  public native final boolean hasQueuedThreads();

  public native final boolean hasContended();

  public native final Thread getFirstQueuedThread();

  public native final boolean isQueued(Thread thread);

  public native final int getQueueLength();

  public native final Collection<Thread> getQueuedThreads();

  public native final Collection<Thread> getExclusiveQueuedThreads();

  public native final Collection<Thread> getSharedQueuedThreads();

  native final boolean apparentlyFirstQueuedIsExclusive();

  native final boolean hasQueuedPredecessors();

  public String toString() {
    int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
  }

  public final boolean owns(ConditionObject condition) {
    if(condition == null) throw new NullPointerException();
    return condition.isOwnedBy(this);
  }

  public final boolean hasWaiters(ConditionObject condition) {
    if (condition == null) {
      throw new NullPointerException();
    }
    if (!((AbstractQueuedSynchronizer.ConditionObject) condition).isOwnedBy(this)) {
      throw new IllegalArgumentException("not owner");
    }
    return ((AbstractQueuedSynchronizer.ConditionObject) condition).hasWaiters();
  }

  public final int getWaitQueueLength(ConditionObject condition) {
    if (condition == null) {
      throw new NullPointerException();
    }
    if (!((AbstractQueuedSynchronizer.ConditionObject) condition).isOwnedBy(this)) {
      throw new IllegalArgumentException("not owner");
    }
    return ((AbstractQueuedSynchronizer.ConditionObject) condition).getWaitQueueLength();
  }

  public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
    if (condition == null) {
      throw new NullPointerException();
    }
    if (!((AbstractQueuedSynchronizer.ConditionObject) condition).isOwnedBy(this)) {
      throw new IllegalArgumentException("not owner");
    }
    return ((AbstractQueuedSynchronizer.ConditionObject) condition).getWaitingThreads();
  }

  public class ConditionObject implements Condition, java.io.Serializable {

    private int version = 0;

    private boolean shouldBeInterrupted = false;

    public native boolean nativeAwait(long timeout) throws InterruptedException;

    public native boolean nativeAwaitUninterruptibly();

    public boolean await(long time, TimeUnit unit) throws InterruptedException {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      int holdCount = getState();
      release(holdCount);
      boolean signalled = false;
      try {
        signalled = nativeAwait(time);
      }catch(InterruptedException e) {
        acquireLock(holdCount);
        throw e;
      }
      acquireLock(holdCount);
      return signalled;
    }

    public boolean awaitUntil(Date deadline) throws InterruptedException {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      long timeToWait = deadline.getTime() - new Date().getTime();
      if(timeToWait <= 0) return false;
      int holdCount = getState();
      release(holdCount);
      boolean signalled = false;
      try {
        signalled = nativeAwait(timeToWait);
      }catch(InterruptedException e) {
        acquireLock(holdCount);
        throw e;
      }
      acquireLock(holdCount);
      return signalled;
    }

    public long awaitNanos(long nanosTimeout) throws InterruptedException {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      int holdCount = getState();
      release(holdCount);
      try {
        nativeAwait(nanosTimeout);
      }catch(InterruptedException e) {
        acquireLock(holdCount);
        throw e;
      }
      acquireLock(holdCount);
      return 0;
    }

    public void await() throws InterruptedException {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      int holdCount = getState();
      release(holdCount);
      try {
        nativeAwait(0);
      }catch(InterruptedException e) {
        acquireLock(holdCount);
        throw e;
      }
      acquireLock(holdCount);
    }

    public void awaitUninterruptibly() {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      boolean interrupted = Thread.currentThread().isInterrupted();
      int holdCount = getState();
      release(holdCount);
      while(!nativeAwaitUninterruptibly()) {
        interrupted = interrupted || Thread.currentThread().isInterrupted();
      }
      acquireLock(holdCount);
      if(interrupted) Thread.currentThread().interrupt();
    }

    public boolean isOwnedBy(AbstractQueuedSynchronizer aqs) {
      return AbstractQueuedSynchronizer.this == aqs;
    }

    protected boolean hasWaiters() {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      return nativeHasWaiters();
    }

    protected int getWaitQueueLength() {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      return nativeGetWaitQueueLength();
    }

    protected Collection<Thread> getWaitingThreads() {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      return nativeGetWaitingThreads();
    }

    public void signal() {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      nativeSignal();
    }

    public void signalAll() {
      if(!isHeldExclusively()) throw new IllegalMonitorStateException();
      nativeSignalAll();
    }

    protected native boolean nativeHasWaiters();

    protected native int nativeGetWaitQueueLength();

    protected native Collection<Thread> nativeGetWaitingThreads();

    public native void nativeSignal();

    public native void nativeSignalAll();

    private void acquireLock(int holdCount) {
      while (holdCount != 0) {
        acquire(1);
        holdCount--;
      }
    }
  }
}
