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
package java.util.concurrent;

import java.util.Collection;

/*
 * Implements logic for Semaphore.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class Semaphore implements java.io.Serializable {

  private int version = 0;

  public Semaphore(int permits) {
    newSemaphore(permits, false);
  }

  public Semaphore(int permits, boolean fair) {
    newSemaphore(permits, fair);
  }

  public native void newSemaphore(int permits, boolean fair);

  public void acquire() throws InterruptedException {
    while (!nativeAcquire(1));
  }

  public void acquireUninterruptibly() {
    while (!nativeAcquireUninterruptibly(1));
  }

  public boolean tryAcquire() {
    return tryAcquire(1);
  }

  public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
    return tryAcquire(1, timeout, unit);
  }

  public void release() {
    release(1);
  }

  public void acquire(int permits) throws InterruptedException {
    while (!nativeAcquire(permits));
  }

  public void acquireUninterruptibly(int permits) {
    while (!nativeAcquireUninterruptibly(permits));
  }

  public native boolean tryAcquire(int permits);

  public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
    boolean r = nativeTryAcquire(permits,timeout,unit);
    removeThreadFromQueue();
    return r;
  }

  private native boolean nativeTryAcquire(int permits, long timeout, TimeUnit unit);

  private native void removeThreadFromQueue();

  public native void release(int permits);

  public native int availablePermits();

  public int drainPermits() {
    int permits = availablePermits();
    if (tryAcquire(permits)) {
      return permits;
    } else {
      return 0;
    }
  }

  protected native void reducePermits(int reduction);

  public native boolean isFair();

  public final boolean hasQueuedThreads() {
    return getQueueLength() > 0;
  }

  public native final int getQueueLength();

  protected native Collection<Thread> getQueuedThreads();

  public String toString() {
    return super.toString() + "[Permits = " + availablePermits() + "]";
  }

  public native boolean nativeAcquireUninterruptibly(int permits);
  public native boolean nativeAcquire(int permits) throws InterruptedException;
}
