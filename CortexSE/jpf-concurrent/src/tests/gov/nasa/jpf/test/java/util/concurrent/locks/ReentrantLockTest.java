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
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.locks.ReentrantLock
 * 
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class ReentrantLockTest extends TestCaseHelpers {

  private final static boolean fairness = false;

  private final static String[] JPF_ARGS = {
  };

  public static void main (String[] args) {
    runTestsOfThisClass(args);
  }

  class TestCustomThread extends Thread {
    protected boolean started = false;

    protected boolean finished = false;

    public TestCustomThread () {
    }

    public TestCustomThread (Runnable r) {
      super(r);
    }

    public boolean isStarted () {
      return this.started;
    }

    public boolean isFinished () {
      return this.finished;
    }
  }

  /**
   * A runnable calling lockInterruptibly
   */
  class InterruptibleLockRunnable extends TestCustomThread implements Runnable {
    final ReentrantLock lock;

    InterruptibleLockRunnable (ReentrantLock l) {
      lock = l;
    }

    public void run () {
      this.started = true;
      try {
        lock.lockInterruptibly();
      } catch (InterruptedException success) {
      }
      this.finished = true;
    }
  }

  /**
   * A runnable calling lockInterruptibly that expects to be interrupted
   */
  class InterruptedLockRunnable extends TestCustomThread implements Runnable {
    final ReentrantLock lock;

    InterruptedLockRunnable (ReentrantLock l) {
      lock = l;
    }

    public void run () {
      this.started = true;
      try {
        lock.lockInterruptibly();
        threadShouldThrow();
      } catch (InterruptedException success) {
      }
      this.finished = true;
    }
  }

  /**
   * Subclass to expose protected methods
   */
  static class PublicReentrantLock extends ReentrantLock {
    PublicReentrantLock () {
      super();
    }

    PublicReentrantLock (boolean fair) {
      super(fair);
    }

    public Collection getQueuedThreads () {
      return super.getQueuedThreads();
    }

    public Collection getWaitingThreads (Condition c) {
      return super.getWaitingThreads(c);
    }
  }

  /**
   * Constructor sets given fairness
   */

  @Test
  public void testConstructor () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ReentrantLock rl = new ReentrantLock();
      assertFalse(rl.isFair());
      ReentrantLock r2 = new ReentrantLock(true);
      assertTrue(r2.isFair());
    }
    printFinish();
  }

  /**
   * locking an unlocked lock succeeds
   */
  @Test
  public void testLock () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ReentrantLock rl = new ReentrantLock(fairness);
      rl.lock();
      assertTrue(rl.isLocked());
      rl.unlock();
    }
    printFinish();
  }

  /**
   * locking an unlocked fair lock succeeds
   */
  @Test
  public void testFairLock () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ReentrantLock rl = new ReentrantLock(fairness);
      rl.lock();
      assertTrue(rl.isLocked());
      rl.unlock();
    }
    printFinish();
  }

  /**
   * Unlocking an unlocked lock throws IllegalMonitorStateException
   */
  @Test
  public void testUnlock_IllegalMonitorStateException () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ReentrantLock rl = new ReentrantLock(fairness);
      try {
        rl.unlock();
        shouldThrow();
      } catch (IllegalMonitorStateException success) {
      }
    }
    printFinish();
  }

  /**
   * tryLock on an unlocked lock succeeds
   */
  @Test
  public void testTryLock () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ReentrantLock rl = new ReentrantLock(fairness);
      assertTrue(rl.tryLock());
      assertTrue(rl.isLocked());
      rl.unlock();
    }
    printFinish();
  }

  /**
   * hasQueuedThreads reports whether there are waiting threads
   */
  @Test
  public void testhasQueuedThreads () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      TestCustomThread t1 = new InterruptedLockRunnable(lock);
      TestCustomThread t2 = new InterruptibleLockRunnable(lock);
      try {
        assertFalse(lock.hasQueuedThreads());
        lock.lock();
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(!lock.hasQueuedThreads());
        assertTrue(lock.hasQueuedThreads());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 2);
        assertTrue(lock.hasQueuedThreads());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        assertTrue(lock.hasQueuedThreads());
        lock.unlock();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.hasQueuedThreads());
        assertFalse(lock.hasQueuedThreads());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getQueueLength reports number of waiting threads
   */
  @Test
  public void testGetQueueLength () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      Thread t1 = new Thread(new InterruptedLockRunnable(lock));
      Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
      try {
        assertEquals(0, lock.getQueueLength());
        lock.lock();
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        assertEquals(1, lock.getQueueLength());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 2);
        assertEquals(2, lock.getQueueLength());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        assertEquals(1, lock.getQueueLength());
        lock.unlock();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 0);
        assertEquals(0, lock.getQueueLength());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getQueueLength reports number of waiting threads
   */
  @Test
  public void testGetQueueLength_fair () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      Thread t1 = new Thread(new InterruptedLockRunnable(lock));
      Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
      try {
        assertEquals(0, lock.getQueueLength());
        lock.lock();
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        assertEquals(1, lock.getQueueLength());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 2);
        assertEquals(2, lock.getQueueLength());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        assertEquals(1, lock.getQueueLength());
        lock.unlock();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 0);
        assertEquals(0, lock.getQueueLength());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * hasQueuedThread(null) throws NPE
   */
  @Test
  public void testHasQueuedThreadNPE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock sync = new ReentrantLock(fairness);
      try {
        sync.hasQueuedThread(null);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * hasQueuedThread reports whether a thread is queued.
   */

  @Test
  public void testHasQueuedThread () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock sync = new ReentrantLock(fairness);
      Thread t1 = new Thread(new InterruptedLockRunnable(sync));
      Thread t2 = new Thread(new InterruptibleLockRunnable(sync));
      try {
        assertFalse(sync.hasQueuedThread(t1));
        assertFalse(sync.hasQueuedThread(t2));
        sync.lock();
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(!sync.hasQueuedThread(t1));
        assertTrue(sync.hasQueuedThread(t1));
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(!sync.hasQueuedThread(t1) || !sync.hasQueuedThread(t2));
        assertTrue(sync.hasQueuedThread(t1));
        assertTrue(sync.hasQueuedThread(t2));
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.hasQueuedThread(t1) || !sync.hasQueuedThread(t2));
        assertFalse(sync.hasQueuedThread(t1));
        assertTrue(sync.hasQueuedThread(t2));
        sync.unlock();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.hasQueuedThread(t1));
        assertFalse(sync.hasQueuedThread(t1));
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(sync.hasQueuedThread(t2));
        assertFalse(sync.hasQueuedThread(t2));
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
  public void testGetQueuedThreads () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final PublicReentrantLock lock = new PublicReentrantLock();
      Thread t1 = new Thread(new InterruptedLockRunnable(lock));
      Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
      try {
        assertTrue(lock.getQueuedThreads().isEmpty());
        lock.lock();
        assertTrue(lock.getQueuedThreads().isEmpty());
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(!lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t1));
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(!lock.getQueuedThreads().contains(t1) || !lock.getQueuedThreads().contains(t2));
        assertTrue(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueuedThreads().contains(t1) || !lock.getQueuedThreads().contains(t2));
        assertFalse(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        lock.unlock();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(!lock.getQueuedThreads().isEmpty());
        assertTrue(lock.getQueuedThreads().isEmpty());
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * timed tryLock is interruptible.
   */

  @Test
  public void testInterruptedException2 () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      lock.lock();
      Thread t = new Thread(new Runnable() {
        public void run () {
          try {
            lock.tryLock(MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(!Thread.currentThread().isInterrupted());
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * TryLock on a locked lock fails
   */

  @Test
  public void testTryLockWhenLocked () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      lock.lock();
      Thread t = new Thread(new Runnable() {
        public void run () {
          threadAssertFalse(lock.tryLock());
        }
      });
      try {
        t.start();
        t.join();
        lock.unlock();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Timed tryLock on a locked lock times out
   */

  @Test
  public void testTryLock_Timeout () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      lock.lock();
      Thread t = new Thread(new Runnable() {
        public void run () {
          try {
            boolean res = lock.tryLock(1, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(res);
            threadAssertFalse(res);
          } catch (Exception ex) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SMALL_DELAY_MS);
        t.join();
        lock.unlock();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getHoldCount returns number of recursive holds
   */

  @Test
  public void testGetHoldCount () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ReentrantLock lock = new ReentrantLock(fairness);
      for (int i = 1; i <= SIZE; i++) {
        lock.lock();
        assertEquals(i, lock.getHoldCount());
      }
      for (int i = SIZE; i > 0; i--) {
        lock.unlock();
        assertEquals(i - 1, lock.getHoldCount());
      }
    }
    printFinish();
  }

  /**
   * isLocked is true when locked and false when not
   */

  @Test
  public void testIsLocked () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      lock.lock();
      assertTrue(lock.isLocked());
      lock.unlock();
      assertFalse(lock.isLocked());
      Thread t = new Thread(new Runnable() {
        public void run () {
          lock.lock();
          try {
            Thread.sleep(SMALL_DELAY_MS);
          } catch (Exception e) {
            threadUnexpectedException();
          }
          lock.unlock();
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(!lock.isLocked());
        assertTrue(lock.isLocked());
        t.join();
        assertFalse(lock.isLocked());
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * lockInterruptibly is interruptible.
   */

  @Test
  public void testLockInterruptibly1 () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      lock.lock();
      Thread t = new Thread(new InterruptedLockRunnable(lock));
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        lock.unlock();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * lockInterruptibly succeeds when unlocked, else is interruptible
   */

  @Test
  public void testLockInterruptibly2 () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      try {
        lock.lockInterruptibly();
      } catch (Exception e) {
        unexpectedException();
      }
      Thread t = new Thread(new InterruptedLockRunnable(lock));
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        t.interrupt();
        assertTrue(lock.isLocked());
        assertTrue(lock.isHeldByCurrentThread());
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Calling await without holding lock throws IllegalMonitorStateException
   */

  @Test
  public void testAwait_IllegalMonitor () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = lock.newCondition();
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
   * Calling signal without holding lock throws IllegalMonitorStateException
   */

  @Test
  public void testSignal_IllegalMonitor () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = lock.newCondition();
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
      final ReentrantLock lock = new ReentrantLock();
      final Condition c = lock.newCondition();
      try {
        lock.lock();
        long t = c.awaitNanos(100);
        assertTrue(t <= 0);
        lock.unlock();
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }
   

  /**
   * timed await without a signal times out
  */
  @Test
  public void testAwait_Timeout() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock();
      final Condition c = lock.newCondition();
      try {
        lock.lock();
        c.await(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
        lock.unlock();
      }
      catch (Exception ex) {
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
      final ReentrantLock lock = new ReentrantLock();
      final Condition c = lock.newCondition();
      try {
        lock.lock();
        java.util.Date d = new java.util.Date();
        c.awaitUntil(new java.util.Date(d.getTime() + 10));
        lock.unlock();
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
  public void testAwait () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = lock.newCondition();
      Thread t = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            c.await();
            lock.unlock();
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(lock.getWaitQueueLength(c) != 1);
        c.signal();
        lock.unlock();
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
  public void testHasWaitersNPE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      try {
        lock.hasWaiters(null);
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
  public void testGetWaitQueueLengthNPE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      try {
        lock.getWaitQueueLength(null);
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
  public void testGetWaitingThreadsNPE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final PublicReentrantLock lock = new PublicReentrantLock();
      try {
        lock.getWaitingThreads(null);
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
  public void testHasWaitersIAE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = (lock.newCondition());
      final ReentrantLock lock2 = new ReentrantLock(fairness);
      try {
        lock2.hasWaiters(c);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * hasWaiters throws IMSE if not locked
   */
  @Test
  public void testHasWaitersIMSE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = (lock.newCondition());
      try {
        lock.hasWaiters(c);
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
  public void testGetWaitQueueLengthIAE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = (lock.newCondition());
      final ReentrantLock lock2 = new ReentrantLock(fairness);
      try {
        lock2.getWaitQueueLength(c);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitQueueLength throws IMSE if not locked
   */
  @Test
  public void testGetWaitQueueLengthIMSE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = (lock.newCondition());
      try {
        lock.getWaitQueueLength(c);
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
  public void testGetWaitingThreadsIAE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final PublicReentrantLock lock = new PublicReentrantLock();
      final Condition c = (lock.newCondition());
      final PublicReentrantLock lock2 = new PublicReentrantLock();
      try {
        lock2.getWaitingThreads(c);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getWaitingThreads throws IMSE if not locked
   */
  @Test
  public void testGetWaitingThreadsIMSE () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final PublicReentrantLock lock = new PublicReentrantLock();
      final Condition c = (lock.newCondition());
      try {
        lock.getWaitingThreads(c);
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
  public void testHasWaiters () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = lock.newCondition();
      Thread t = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            threadAssertFalse(lock.hasWaiters(c));
            threadAssertEquals(0, lock.getWaitQueueLength(c));
            c.await();
            lock.unlock();
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(!lock.hasWaiters(c));
        assertTrue(lock.hasWaiters(c));
        assertEquals(1, lock.getWaitQueueLength(c));
        c.signal();
        lock.unlock();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(lock.hasWaiters(c));
        assertFalse(lock.hasWaiters(c));
        assertEquals(0, lock.getWaitQueueLength(c));
        lock.unlock();
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
  public void testGetWaitQueueLength () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = lock.newCondition();
      Thread t1 = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            Verify.ignoreIf(lock.hasWaiters(c));
            threadAssertFalse(lock.hasWaiters(c));
            threadAssertEquals(0, lock.getWaitQueueLength(c));
            c.await();
            lock.unlock();
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            Verify.ignoreIf(!lock.hasWaiters(c));
            threadAssertTrue(lock.hasWaiters(c));
            threadAssertEquals(1, lock.getWaitQueueLength(c));
            c.await();
            lock.unlock();
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
        lock.lock();
        Verify.ignoreIf(!lock.hasWaiters(c));
        assertTrue(lock.hasWaiters(c));
        Verify.ignoreIf(lock.getWaitQueueLength(c) != 2);
        assertEquals(2, lock.getWaitQueueLength(c));
        c.signalAll();
        lock.unlock();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(lock.hasWaiters(c));
        assertFalse(lock.hasWaiters(c));
        assertEquals(0, lock.getWaitQueueLength(c));
        lock.unlock();
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
  public void testGetWaitingThreads () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final PublicReentrantLock lock = new PublicReentrantLock();
      final Condition c = lock.newCondition();
      Thread t1 = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            Verify.ignoreIf(!lock.getWaitingThreads(c).isEmpty());
            threadAssertTrue(lock.getWaitingThreads(c).isEmpty());
            c.await();
            lock.unlock();
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            Verify.ignoreIf(lock.getWaitingThreads(c).isEmpty());
            threadAssertFalse(lock.getWaitingThreads(c).isEmpty());
            c.await();
            lock.unlock();
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        lock.lock();
        assertTrue(lock.getWaitingThreads(c).isEmpty());
        lock.unlock();
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(!lock.hasWaiters(c));
        assertTrue(lock.hasWaiters(c));
        Verify.ignoreIf(!lock.getWaitingThreads(c).contains(t1));
        assertTrue(lock.getWaitingThreads(c).contains(t1));
        Verify.ignoreIf(!lock.getWaitingThreads(c).contains(t2));
        assertTrue(lock.getWaitingThreads(c).contains(t2));
        c.signalAll();
        lock.unlock();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(lock.hasWaiters(c));
        assertFalse(lock.hasWaiters(c));
        assertTrue(lock.getWaitingThreads(c).isEmpty());
        lock.unlock();
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

  /** A helper class for uninterruptible wait tests */
  class UninterruptableThread extends Thread {
    private ReentrantLock lock;

    private Condition c;

    public volatile boolean canAwake = false;

    public volatile boolean interrupted = false;

    public volatile boolean lockStarted = false;

    public UninterruptableThread (ReentrantLock lock, Condition c) {
      this.lock = lock;
      this.c = c;
    }

    public synchronized void run () {
      lock.lock();
      c.awaitUninterruptibly();
      lock.unlock();
    }
  }

  /**
   * awaitUninterruptibly doesn't abort on interrupt
   */
  @Test
  public void testAwaitUninterruptibly() { 
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock();
      final Condition c = lock.newCondition();
      UninterruptableThread thread = new UninterruptableThread(lock, c);
      try {
        thread.start();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(!lock.hasWaiters(c));
        try {
          thread.interrupt();
          Thread.sleep(SHORT_DELAY_MS);
          Verify.ignoreIf(thread.getState() != Thread.State.WAITING);
          c.signal();
        } finally {
          lock.unlock();
        }
        thread.join();
        assertTrue(thread.isInterrupted());
        assertFalse(thread.isAlive());
      } catch(Exception ex) {
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
      final ReentrantLock lock = new ReentrantLock();
      final Condition c = lock.newCondition();
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            lock.lock();
            c.await();
            lock.unlock();
            threadShouldThrow();
          } catch (InterruptedException success) { }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(lock.getWaitQueueLength(c) == 0);
        lock.unlock();
        t.interrupt();
        t.join();
        Verify.ignoreIf(t.isAlive());
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
      final ReentrantLock lock = new ReentrantLock();
      final Condition c = lock.newCondition();
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            lock.lock();
            c.awaitNanos(1000 * 1000 * 1000); // 1 sec
            lock.unlock();
          }catch(InterruptedException e) {}
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(!lock.hasWaiters(c));
        lock.unlock();
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
      final ReentrantLock lock = new ReentrantLock();
      final Condition c = lock.newCondition();
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            lock.lock();
            java.util.Date d = new java.util.Date();
            c.awaitUntil(new java.util.Date(d.getTime() + 10000));
            lock.unlock();
            Verify.ignoreIf(!Thread.currentThread().isInterrupted());
            threadShouldThrow();
          } catch (InterruptedException success) { }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
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
  public void testSignalAll () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = lock.newCondition();
      Thread t1 = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            c.await();
            lock.unlock();
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            c.await();
            lock.unlock();
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(lock.getWaitQueueLength(c) != 2);
        c.signalAll();
        lock.unlock();
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
   * await after multiple reentrant locking preserves lock count
   */
  @Test
  public void testAwaitLockCount () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final Condition c = lock.newCondition();
      Thread t1 = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            threadAssertEquals(1, lock.getHoldCount());
            c.await();
            threadAssertEquals(1, lock.getHoldCount());
            lock.unlock();
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {
        public void run () {
          try {
            lock.lock();
            lock.lock();
            threadAssertEquals(2, lock.getHoldCount());
            c.await();
            threadAssertEquals(2, lock.getHoldCount());
            lock.unlock();
            lock.unlock();
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        lock.lock();
        Verify.ignoreIf(lock.getWaitQueueLength(c) != 2);
        lock.unlock();
        lock.lock();
        c.signalAll();
        lock.unlock();
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
   * A serialized lock deserializes as unlocked
   */
  //@Test
  public void testSerialization() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ReentrantLock l = new ReentrantLock(fairness);
      l.lock();
      l.unlock();
      try {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        ReentrantLock r = (ReentrantLock) in.readObject();
        r.lock();
        r.unlock(); }
      catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }
   

  /**
   * toString indicates current lock state
   */
  @Test
  public void testToString () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ReentrantLock lock = new ReentrantLock(fairness);
      String us = lock.toString();
      assertTrue(us.indexOf("Unlocked") >= 1);
      lock.lock();
      String ls = lock.toString();
      assertTrue(ls.indexOf("Locked") >= 0);
    }
    printFinish();
  }

}