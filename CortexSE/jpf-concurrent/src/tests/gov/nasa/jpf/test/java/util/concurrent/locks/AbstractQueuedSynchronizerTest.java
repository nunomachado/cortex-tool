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
package gov.nasa.jpf.test.java.util.concurrent.locks;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.test.java.util.concurrent.TestCaseHelpers;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.locks.AbstractQueuedSynchronizer
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AbstractQueuedSynchronizerTest extends TestCaseHelpers {

  /**
   * constructor initializes to given value
   */
  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * A simple mutex class, adapted from the
   * AbstractQueuedSynchronizer javadoc.  Exclusive acquire tests
   * exercise this as a sample user extension.  Other
   * methods/features of AbstractQueuedSynchronizerTest are tested
   * via other test classes, including those for ReentrantLock,
   * ReentrantReadWriteLock, and Semaphore
   */
  static class Mutex extends AbstractQueuedSynchronizer {

    public boolean isHeldExclusively() {
      return getState() == 1;
    }

    public boolean tryAcquire(int acquires) {
      assertTrue(acquires == 1);
      return compareAndSetState(0, 1);
    }

    public boolean tryRelease(int releases) {
      if (getState() == 0) {
        throw new IllegalMonitorStateException();
      }
      setState(0);
      return true;
    }

    public AbstractQueuedSynchronizer.ConditionObject newCondition() {
      return new AbstractQueuedSynchronizer.ConditionObject();
    }
  }

  /**
   * A simple latch class, to test shared mode.
   */
  static class BooleanLatch extends AbstractQueuedSynchronizer {

    public boolean isSignalled() {
      return getState() != 0;
    }

    public int tryAcquireShared(int ignore) {
      return isSignalled() ? 1 : -1;
    }

    public boolean tryReleaseShared(int ignore) {
      setState(1);
      return true;
    }
  }

  /**
   * A runnable calling acquireInterruptibly
   */
  class InterruptibleSyncRunnable implements Runnable {

    final Mutex sync;

    InterruptibleSyncRunnable(Mutex l) {
      sync = l;
    }

    public void run() {
      try {
        sync.acquireInterruptibly(1);
      } catch (InterruptedException success) {
      }
    }
  }

  /**
   * A runnable calling acquireInterruptibly that expects to be
   * interrupted
   */
  class InterruptedSyncRunnable implements Runnable {

    final Mutex sync;

    InterruptedSyncRunnable(Mutex l) {
      sync = l;
    }

    public void run() {
      try {
        sync.acquireInterruptibly(1);
        threadShouldThrow();
      } catch (InterruptedException success) {
      }
    }
  }

  /**
   * isHeldExclusively is false upon construction
   */
  @Test
  public void testIsHeldExclusively() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Mutex rl = new Mutex();
      assertFalse(rl.isHeldExclusively());
    }
    printFinish();
  }

  /**
   * acquiring released sync succeeds
   */
  @Test
  public void testAcquire() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Mutex rl = new Mutex();
      rl.acquire(1);
      assertTrue(rl.isHeldExclusively());
      rl.release(1);
      assertFalse(rl.isHeldExclusively());
    }
    printFinish();
  }

  /**
   * tryAcquire on an released sync succeeds
   */
  @Test
  public void testTryAcquire() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Mutex rl = new Mutex();
      assertTrue(rl.tryAcquire(1));
      assertTrue(rl.isHeldExclusively());
      rl.release(1);
    }
    printFinish();
  }

  /**
   * hasQueuedThreads reports whether there are waiting threads
   */
  @Test
  public void testhasQueuedThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
      Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
      try {
        assertFalse(sync.hasQueuedThreads());
        sync.acquire(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertTrue(sync.hasQueuedThreads());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 2);
        assertTrue(sync.hasQueuedThreads());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertTrue(sync.hasQueuedThreads());
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 0);
        assertFalse(sync.hasQueuedThreads());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * isQueued(null) throws NPE
   */
  @Test
  public void testIsQueuedNPE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      try {
        sync.isQueued(null);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * isQueued reports whether a thread is queued.
   */
  @Test
  public void testIsQueued() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
      Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
      try {
        assertFalse(sync.isQueued(t1));
        assertFalse(sync.isQueued(t2));
        sync.acquire(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertTrue(sync.isQueued(t1));
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 2);
        assertTrue(sync.isQueued(t1));
        assertTrue(sync.isQueued(t2));
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertFalse(sync.isQueued(t1));
        assertTrue(sync.isQueued(t2));
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 0);
        assertFalse(sync.isQueued(t1));
        Thread.sleep(SHORT_DELAY_MS);
        assertFalse(sync.isQueued(t2));
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getFirstQueuedThread returns first waiting thread or null if none
   */
  @Test
  public void testGetFirstQueuedThread() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
      Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
      try {
        assertNull(sync.getFirstQueuedThread());
        sync.acquire(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertEquals(t1, sync.getFirstQueuedThread());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 2);
        assertEquals(t1, sync.getFirstQueuedThread());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertEquals(t2, sync.getFirstQueuedThread());
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 0);
        assertNull(sync.getFirstQueuedThread());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * hasContended reports false if no thread has ever blocked, else true
   */
  @Test
  public void testHasContended() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
      Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
      try {
        assertFalse(sync.hasContended());
        sync.acquire(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertTrue(sync.hasContended());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 2);
        assertTrue(sync.hasContended());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertTrue(sync.hasContended());
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 0);
        assertTrue(sync.hasContended());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getQueuedThreads includes waiting threads
   */
  @Test
  public void testGetQueuedThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
      Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
      try {
        assertTrue(sync.getQueuedThreads().isEmpty());
        sync.acquire(1);
        assertTrue(sync.getQueuedThreads().isEmpty());
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertTrue(sync.getQueuedThreads().contains(t1));
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 2);
        assertTrue(sync.getQueuedThreads().contains(t1));
        assertTrue(sync.getQueuedThreads().contains(t2));
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertFalse(sync.getQueuedThreads().contains(t1));
        assertTrue(sync.getQueuedThreads().contains(t2));
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 0);
        assertTrue(sync.getQueuedThreads().isEmpty());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getExclusiveQueuedThreads includes waiting threads
   */
  @Test
  public void testGetExclusiveQueuedThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
      Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
      try {
        assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
        sync.acquire(1);
        assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertTrue(sync.getExclusiveQueuedThreads().contains(t1));
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 2);
        assertTrue(sync.getExclusiveQueuedThreads().contains(t1));
        assertTrue(sync.getExclusiveQueuedThreads().contains(t2));
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertFalse(sync.getExclusiveQueuedThreads().contains(t1));
        assertTrue(sync.getExclusiveQueuedThreads().contains(t2));
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 0);
        assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getSharedQueuedThreads does not include exclusively waiting threads
   */
  @Test
  public void testGetSharedQueuedThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
      Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
      try {
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        sync.acquire(1);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 2);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 0);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * tryAcquireNanos is interruptible.
   */
  @Test
  public void testInterruptedException2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      sync.acquire(1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            sync.tryAcquireNanos(1, MEDIUM_DELAY_MS * 1000 * 1000);
            Verify.ignoreIf(!Thread.currentThread().isInterrupted());
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        t.interrupt();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * TryAcquire on exclusively held sync fails
   */
  @Test
  public void testTryAcquireWhenSynced() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      sync.acquire(1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          threadAssertFalse(sync.tryAcquire(1));
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.join();
        sync.release(1);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * tryAcquireNanos on an exclusively held sync times out
   */
  @Test
  public void testAcquireNanos_Timeout() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      sync.acquire(1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            threadAssertFalse(sync.tryAcquireNanos(1, 1000 * 1000));
          } catch (Exception ex) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.join();
        sync.release(1);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getState is true when acquired and false when not
   */
  @Test
  public void testGetState() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      sync.acquire(1);
      assertTrue(sync.isHeldExclusively());
      sync.release(1);
      assertFalse(sync.isHeldExclusively());
      Thread t = new Thread(new Runnable() {

        public void run() {
          sync.acquire(1);
          try {
            Thread.sleep(SMALL_DELAY_MS);
          } catch (Exception e) {
            threadUnexpectedException();
          }
          sync.release(1);
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(!sync.isHeldExclusively());
        assertTrue(sync.isHeldExclusively());
        t.join();
        assertFalse(sync.isHeldExclusively());
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * acquireInterruptibly is interruptible.
   */
  @Test
  public void testAcquireInterruptibly1() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      sync.acquire(1);
      Thread t = new Thread(new InterruptedSyncRunnable(sync));
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        t.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        sync.release(1);
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * acquireInterruptibly succeeds when released, else is interruptible
   */
  @Test
  public void testAcquireInterruptibly2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      try {
        sync.acquireInterruptibly(1);
      } catch (Exception e) {
        unexpectedException();
      }
      Thread t = new Thread(new InterruptedSyncRunnable(sync));
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.getQueueLength() != 1);
        t.interrupt();
        assertTrue(sync.isHeldExclusively());
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * owns is true for a condition created by sync else false
   */
  @Test
  public void testOwns() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      final Mutex sync2 = new Mutex();
      assertTrue(sync.owns(c));
      assertFalse(sync2.owns(c));
    }
    printFinish();
  }

  /**
   * Calling await without holding sync throws IllegalMonitorStateException
   */
  @Test
  public void testAwait_IllegalMonitor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      try {
        c.await();
        shouldThrow();
      } catch (IllegalMonitorStateException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Calling signal without holding sync throws IllegalMonitorStateException
   */
  @Test
  public void testSignal_IllegalMonitor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      try {
        c.signal();
        shouldThrow();
      } catch (IllegalMonitorStateException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * awaitNanos without a signal times out
   */
  @Test
  public void testAwaitNanos_Timeout() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      try {
        sync.acquire(1);
        long t = c.awaitNanos(100);
        assertTrue(t <= 0);
        sync.release(1);
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   *  Timed await without a signal times out
   */
  @Test
  public void testAwait_Timeout() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      try {
        sync.acquire(1);
        assertFalse(c.await(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        sync.release(1);
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * awaitUntil without a signal times out
   */
  @Test
  public void testAwaitUntil_Timeout() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      try {
        sync.acquire(1);
        java.util.Date d = new java.util.Date();
        assertFalse(c.awaitUntil(new java.util.Date(d.getTime() + 10)));
        sync.release(1);
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * await returns when signalled
   */
  @Test
  public void testAwait() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            c.await();
            sync.release(1);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(sync.getWaitQueueLength(c) != 1);
        c.signal();
        sync.release(1);
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * hasWaiters throws NPE if null
   */
  @Test
  public void testHasWaitersNPE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      try {
        sync.hasWaiters(null);
        shouldThrow();
      } catch (NullPointerException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitQueueLength throws NPE if null
   */
  @Test
  public void testGetWaitQueueLengthNPE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      try {
        sync.getWaitQueueLength(null);
        shouldThrow();
      } catch (NullPointerException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitingThreads throws NPE if null
   */
  @Test
  public void testGetWaitingThreadsNPE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      try {
        sync.getWaitingThreads(null);
        shouldThrow();
      } catch (NullPointerException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * hasWaiters throws IAE if not owned
   */
  @Test
  public void testHasWaitersIAE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = (sync.newCondition());
      final Mutex sync2 = new Mutex();
      try {
        sync2.hasWaiters(c);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * hasWaiters throws IMSE if not synced
   */
  @Test
  public void testHasWaitersIMSE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = (sync.newCondition());
      try {
        sync.hasWaiters(c);
        shouldThrow();
      } catch (IllegalMonitorStateException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitQueueLength throws IAE if not owned
   */
  @Test
  public void testGetWaitQueueLengthIAE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = (sync.newCondition());
      final Mutex sync2 = new Mutex();
      try {
        sync2.getWaitQueueLength(c);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitQueueLength throws IMSE if not synced
   */
  @Test
  public void testGetWaitQueueLengthIMSE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = (sync.newCondition());
      try {
        sync.getWaitQueueLength(c);
        shouldThrow();
      } catch (IllegalMonitorStateException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitingThreads throws IAE if not owned
   */
  @Test
  public void testGetWaitingThreadsIAE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = (sync.newCondition());
      final Mutex sync2 = new Mutex();
      try {
        sync2.getWaitingThreads(c);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitingThreads throws IMSE if not synced
   */
  @Test
  public void testGetWaitingThreadsIMSE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = (sync.newCondition());
      try {
        sync.getWaitingThreads(c);
        shouldThrow();
      } catch (IllegalMonitorStateException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * hasWaiters returns true when a thread is waiting, else false
   */
  @Test
  public void testHasWaiters() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            threadAssertFalse(sync.hasWaiters(c));
            threadAssertEquals(0, sync.getWaitQueueLength(c));
            c.await();
            sync.release(1);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(!sync.hasWaiters(c));
        assertTrue(sync.hasWaiters(c));
        assertEquals(1, sync.getWaitQueueLength(c));
        c.signal();
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(sync.hasWaiters(c));
        assertFalse(sync.hasWaiters(c));
        assertEquals(0, sync.getWaitQueueLength(c));
        sync.release(1);
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitQueueLength returns number of waiting threads
   */
  @Test
  public void testGetWaitQueueLength() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            Verify.ignoreIf(sync.hasWaiters(c));
            threadAssertFalse(sync.hasWaiters(c));
            threadAssertEquals(0, sync.getWaitQueueLength(c));
            c.await();
            sync.release(1);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            Verify.ignoreIf(!sync.hasWaiters(c));
            threadAssertTrue(sync.hasWaiters(c));
            threadAssertEquals(1, sync.getWaitQueueLength(c));
            c.await();
            sync.release(1);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(!sync.hasWaiters(c));
        assertTrue(sync.hasWaiters(c));
        Verify.ignoreIf(sync.getWaitQueueLength(c) != 2);
        assertEquals(2, sync.getWaitQueueLength(c));
        c.signalAll();
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(sync.hasWaiters(c));
        assertFalse(sync.hasWaiters(c));
        assertEquals(0, sync.getWaitQueueLength(c));
        sync.release(1);
        t1.join();
        t2.join();
        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitingThreads returns only and all waiting threads
   */
  @Test
  public void testGetWaitingThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            Verify.ignoreIf(!sync.getWaitingThreads(c).isEmpty());
            threadAssertTrue(sync.getWaitingThreads(c).isEmpty());
            c.await();
            sync.release(1);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            Verify.ignoreIf(sync.getWaitingThreads(c).isEmpty());
            threadAssertFalse(sync.getWaitingThreads(c).isEmpty());
            c.await();
            sync.release(1);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        sync.acquire(1);
        assertTrue(sync.getWaitingThreads(c).isEmpty());
        sync.release(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(!sync.hasWaiters(c));
        assertTrue(sync.hasWaiters(c));
        Verify.ignoreIf(!sync.getWaitingThreads(c).contains(t1));
        assertTrue(sync.getWaitingThreads(c).contains(t1));
        Verify.ignoreIf(!sync.getWaitingThreads(c).contains(t2));
        assertTrue(sync.getWaitingThreads(c).contains(t2));
        c.signalAll();
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(sync.hasWaiters(c));
        assertFalse(sync.hasWaiters(c));
        assertTrue(sync.getWaitingThreads(c).isEmpty());
        sync.release(1);
        t1.join();
        t2.join();
        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * awaitUninterruptibly doesn't abort on interrupt
   */
  @Test
  public void testAwaitUninterruptibly() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      final AtomicInteger status = new AtomicInteger(0);
      Thread t = new Thread(new Runnable() {

        public void run() {
          sync.acquire(1);
          c.awaitUninterruptibly();
          threadAssertEquals(status.get(), 2);
          sync.release(1);
        }
      });

      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        status.set(1);
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(!sync.hasWaiters(c));
        status.set(2);
        c.signal();
        sync.release(1);
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * await is interruptible
   */
  @Test
  public void testAwait_Interrupt() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            c.await();
            sync.release(1);
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });

      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(!sync.hasWaiters(c));
        sync.release(1);
        t.interrupt();
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * awaitNanos is interruptible
   */
  @Test
  public void testAwaitNanos_Interrupt() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            c.awaitNanos(1000 * 1000 * 1000); // 1 sec
            sync.release(1);
            Verify.ignoreIf(!Thread.currentThread().isInterrupted());
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });

      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(!sync.hasWaiters(c));
        sync.release(1);
        t.interrupt();
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * awaitUntil is interruptible
   */
  @Test
  public void testAwaitUntil_Interrupt() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            java.util.Date d = new java.util.Date();
            c.awaitUntil(new java.util.Date(d.getTime() + 10000));
            sync.release(1);
            Verify.ignoreIf(!Thread.currentThread().isInterrupted());
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });

      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(!sync.hasWaiters(c));
        sync.release(1);
        t.interrupt();
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * signalAll wakes up all threads
   */
  @Test
  public void testSignalAll() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            c.await();
            sync.release(1);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            sync.acquire(1);
            c.await();
            sync.release(1);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        Verify.ignoreIf(sync.getWaitQueueLength(c) != 2);
        c.signalAll();
        sync.release(1);
        t1.join();
        t2.join();
        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * toString indicates current state
   */
  @Test
  public void testToString() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Mutex sync = new Mutex();
      String us = sync.toString();
      assertTrue(us.indexOf("State = 0") >= 0);
      sync.acquire(1);
      String ls = sync.toString();
      assertTrue(ls.indexOf("State = 1") >= 0);
    }
    printFinish();
  }

  /**
   * A serialized AQS deserializes with current state
   */
  //@Test
  public void testSerialization() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Mutex l = new Mutex();
      l.acquire(1);
      assertTrue(l.isHeldExclusively());

      try {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        Mutex r = (Mutex) in.readObject();
        assertTrue(r.isHeldExclusively());
      } catch (Exception e) {
        e.printStackTrace();
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * tryReleaseShared setting state changes getState
   */
  @Test
  public void testGetStateWithReleaseShared() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final BooleanLatch l = new BooleanLatch();
      assertFalse(l.isSignalled());
      l.releaseShared(0);
      assertTrue(l.isSignalled());
    }
    printFinish();
  }

  /**
   * releaseShared has no effect when already signalled
   */
  @Test
  public void testReleaseShared() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final BooleanLatch l = new BooleanLatch();
      assertFalse(l.isSignalled());
      l.releaseShared(0);
      assertTrue(l.isSignalled());
      l.releaseShared(0);
      assertTrue(l.isSignalled());
    }
    printFinish();
  }

  /**
   * acquireSharedInterruptibly returns after release, but not before
   */
  @Test
  public void testAcquireSharedInterruptibly() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final BooleanLatch l = new BooleanLatch();

      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            Verify.ignoreIf(l.isSignalled());
            threadAssertFalse(l.isSignalled());
            l.acquireSharedInterruptibly(0);
            threadAssertTrue(l.isSignalled());
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(l.isSignalled());
        assertFalse(l.isSignalled());
        Thread.sleep(SHORT_DELAY_MS);
        l.releaseShared(0);
        assertTrue(l.isSignalled());
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * acquireSharedTimed returns after release
   */
  @Test
  public void testAsquireSharedTimed() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final BooleanLatch l = new BooleanLatch();

      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            Verify.ignoreIf(l.isSignalled());
            threadAssertFalse(l.isSignalled());
            boolean r = l.tryAcquireSharedNanos(0, MEDIUM_DELAY_MS * 1000 * 1000);
            Verify.ignoreIf(!r);
            threadAssertTrue(r);
            threadAssertTrue(l.isSignalled());

          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(l.isSignalled());
        assertFalse(l.isSignalled());
        Thread.sleep(SHORT_DELAY_MS);
        l.releaseShared(0);
        assertTrue(l.isSignalled());
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * acquireSharedInterruptibly throws IE if interrupted before released
   */
  @Test
  public void testAcquireSharedInterruptibly_InterruptedException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final BooleanLatch l = new BooleanLatch();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            threadAssertFalse(l.isSignalled());
            l.acquireSharedInterruptibly(0);
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Verify.ignoreIf(l.isSignalled());
        assertFalse(l.isSignalled());
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * acquireSharedTimed throws IE if interrupted before released
   */
  @Test
  public void testAcquireSharedNanos_InterruptedException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final BooleanLatch l = new BooleanLatch();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            threadAssertFalse(l.isSignalled());
            l.tryAcquireSharedNanos(0, SMALL_DELAY_MS * 1000 * 1000);
            Verify.ignoreIf(!Thread.currentThread().isInterrupted());
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(l.isSignalled());
        assertFalse(l.isSignalled());
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * acquireSharedTimed times out if not released before timeout
   */
  @Test
  public void testAcquireSharedNanos_Timeout() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final BooleanLatch l = new BooleanLatch();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            threadAssertFalse(l.isSignalled());
            threadAssertFalse(l.tryAcquireSharedNanos(0, SMALL_DELAY_MS * 1000 * 1000));
          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(l.isSignalled());
        assertFalse(l.isSignalled());
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

}
