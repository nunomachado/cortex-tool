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
package gov.nasa.jpf.test.java.util.concurrent;

import gov.nasa.jpf.jvm.Verify;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.Semaphore
 * 
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class SemaphoreTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {};

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * Subclass to expose protected methods
   */
  static class PublicSemaphore extends Semaphore {

    PublicSemaphore(int p, boolean f) {
      super(p, f);
    }

    public Collection<Thread> getQueuedThreads() {
      return super.getQueuedThreads();
    }

    public void reducePermits(int p) {
      super.reducePermits(p);
    }
  }

  /**
   * A runnable calling acquire
   */
  class InterruptibleLockRunnable implements Runnable {

    final Semaphore lock;

    InterruptibleLockRunnable(Semaphore l) {
      lock = l;
    }

    public void run() {
      try {
        lock.acquire();
      } catch (InterruptedException success) {
      }
    }
  }

  /**
   * A runnable calling acquire that expects to be
   * interrupted
   */
  class InterruptedLockRunnable implements Runnable {

    final Semaphore lock;

    InterruptedLockRunnable(Semaphore l) {
      lock = l;
    }

    public void run() {
      try {
        lock.acquire();
        threadShouldThrow();
      } catch (InterruptedException success) {
      }
    }
  }

  /**
   * Zero, negative, and positive initial values are allowed in constructor
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s0 = new Semaphore(0, false);
      assertEquals(0, s0.availablePermits());
      assertFalse(s0.isFair());
      Semaphore s1 = new Semaphore(-1, false);
      assertEquals(-1, s1.availablePermits());
      assertFalse(s1.isFair());
      Semaphore s2 = new Semaphore(-1, false);
      assertEquals(-1, s2.availablePermits());
      assertFalse(s2.isFair());
    }
    printFinish();
  }

  /**
   * Constructor without fairness argument behaves as nonfair
   */
  @Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s0 = new Semaphore(0);
      assertEquals(0, s0.availablePermits());
      assertFalse(s0.isFair());
      Semaphore s1 = new Semaphore(-1);
      assertEquals(-1, s1.availablePermits());
      assertFalse(s1.isFair());
      Semaphore s2 = new Semaphore(-1);
      assertEquals(-1, s2.availablePermits());
      assertFalse(s2.isFair());
    }
    printFinish();
  }

  /**
   * tryAcquire succeeds when sufficient permits, else fails
   */
  @Test
  public void testTryAcquireInSameThread() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(2, false);
      assertEquals(2, s.availablePermits());
      assertTrue(s.tryAcquire());
      assertTrue(s.tryAcquire());
      assertEquals(0, s.availablePermits());
      assertFalse(s.tryAcquire());
    }
    printFinish();
  }

  /**
   * Acquire and release of semaphore succeed if initially available
   */
  @Test
  public void testAcquireReleaseInSameThread() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(1, false);
      try {
        s.acquire();
        s.release();
        s.acquire();
        s.release();
        s.acquire();
        s.release();
        s.acquire();
        s.release();
        s.acquire();
        s.release();
        assertEquals(1, s.availablePermits());
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Uninterruptible acquire and release of semaphore succeed if
   * initially available
   */
  @Test
  public void testAcquireUninterruptiblyReleaseInSameThread() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(1, false);
      try {
        s.acquireUninterruptibly();
        s.release();
        s.acquireUninterruptibly();
        s.release();
        s.acquireUninterruptibly();
        s.release();
        s.acquireUninterruptibly();
        s.release();
        s.acquireUninterruptibly();
        s.release();
        assertEquals(1, s.availablePermits());
      } finally {
      }
    }
    printFinish();
  }

  /**
   * Timed Acquire and release of semaphore succeed if
   * initially available
   */
  @Test
  public void testTimedAcquireReleaseInSameThread() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(1, false);
      try {
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertEquals(1, s.availablePermits());
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A release in one thread enables an acquire in another thread
   */
  @Test
  public void testAcquireReleaseInDifferentThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(0, false);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            s.acquire();
            s.release();
            s.release();
            s.acquire();
          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        s.release();
        s.release();
        s.acquire();
        s.acquire();
        s.release();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A release in one thread enables an uninterruptible acquire in another thread
   */
  @Test
  public void testUninterruptibleAcquireReleaseInDifferentThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(0, false);
      Thread t = new Thread(new Runnable() {

        public void run() {
          s.acquireUninterruptibly();
          s.release();
          s.release();
          s.acquireUninterruptibly();
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        s.release();
        s.release();
        s.acquireUninterruptibly();
        s.acquireUninterruptibly();
        s.release();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   *  A release in one thread enables a timed acquire in another thread
   */
  @Test
  public void testTimedAcquireReleaseInDifferentThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(1, false);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            s.release();
            threadAssertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
            s.release();
            threadAssertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));

          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        s.release();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A waiting acquire blocks interruptibly
   */
  @Test
  public void testAcquire_InterruptedException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(0, false);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            s.acquire();
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(s.getQueueLength() != 1);
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   *  A waiting timed acquire blocks interruptibly
   */
  @Test
  public void testTryAcquire_InterruptedException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(0, false);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            if(s.tryAcquire(MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS))
              threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(s.getQueueLength() != 1);
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * hasQueuedThreads reports whether there are waiting threads
   */
  @Test
  public void testHasQueuedThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore lock = new Semaphore(1, false);
      Thread t1 = new Thread(new InterruptedLockRunnable(lock));
      Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
      try {
        assertFalse(lock.hasQueuedThreads());
        lock.acquireUninterruptibly();
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        assertTrue(lock.hasQueuedThreads());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 2);
        assertTrue(lock.hasQueuedThreads());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        assertTrue(lock.hasQueuedThreads());
        lock.release();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 0);
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
  public void testGetQueueLength() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore lock = new Semaphore(1, false);
      Thread t1 = new Thread(new InterruptedLockRunnable(lock));
      Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
      try {
        assertEquals(0, lock.getQueueLength());
        lock.acquireUninterruptibly();
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
        lock.release();
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
   * getQueuedThreads includes waiting threads
   */
  @Test
  public void testGetQueuedThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final PublicSemaphore lock = new PublicSemaphore(1, false);
      Thread t1 = new Thread(new InterruptedLockRunnable(lock));
      Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
      try {
        assertTrue(lock.getQueuedThreads().isEmpty());
        lock.acquireUninterruptibly();
        assertTrue(lock.getQueuedThreads().isEmpty());
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        assertTrue(lock.getQueuedThreads().contains(t1));
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 2);
        assertTrue(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 1);
        assertFalse(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        lock.release();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(lock.getQueueLength() != 0);
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
   * drainPermits reports and removes given number of permits
   */
  @Test
  public void testDrainPermits() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(0, false);
      assertEquals(0, s.availablePermits());
      assertEquals(0, s.drainPermits());
      s.release(10);
      assertEquals(10, s.availablePermits());
      assertEquals(10, s.drainPermits());
      assertEquals(0, s.availablePermits());
      assertEquals(0, s.drainPermits());
    }
    printFinish();
  }

  /**
   * reducePermits reduces number of permits
   */
  @Test
  public void testReducePermits() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      PublicSemaphore s = new PublicSemaphore(10, false);
      assertEquals(10, s.availablePermits());
      s.reducePermits(1);
      assertEquals(9, s.availablePermits());
      s.reducePermits(10);
      assertEquals(-1, s.availablePermits());
    }
    printFinish();
  }

  /**
   * a deserialized serialized semaphore has same number of permits
   */
  //@Test
  public void testSerialization() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore l = new Semaphore(3, false);
      try {
        l.acquire();
        l.release();
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        Semaphore r = (Semaphore) in.readObject();
        assertEquals(3, r.availablePermits());
        assertFalse(r.isFair());
        r.acquire();
        r.release();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Zero, negative, and positive initial values are allowed in constructor
   */
  @Test
  public void testConstructor_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s0 = new Semaphore(0, true);
      assertEquals(0, s0.availablePermits());
      assertTrue(s0.isFair());
      Semaphore s1 = new Semaphore(-1, true);
      assertEquals(-1, s1.availablePermits());
      Semaphore s2 = new Semaphore(-1, true);
      assertEquals(-1, s2.availablePermits());
    }
    printFinish();
  }

  /**
   * tryAcquire succeeds when sufficient permits, else fails
   */
  @Test
  public void testTryAcquireInSameThread_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(2, true);
      assertEquals(2, s.availablePermits());
      assertTrue(s.tryAcquire());
      assertTrue(s.tryAcquire());
      assertEquals(0, s.availablePermits());
      assertFalse(s.tryAcquire());
    }
    printFinish();
  }

  /**
   * tryAcquire(n) succeeds when sufficient permits, else fails
   */
  @Test
  public void testTryAcquireNInSameThread_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(2, true);
      assertEquals(2, s.availablePermits());
      assertTrue(s.tryAcquire(2));
      assertEquals(0, s.availablePermits());
      assertFalse(s.tryAcquire());
    }
    printFinish();
  }

  /**
   * Acquire and release of semaphore succeed if initially available
   */
  @Test
  public void testAcquireReleaseInSameThread_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(1, true);
      try {
        s.acquire();
        s.release();
        s.acquire();
        s.release();
        s.acquire();
        s.release();
        s.acquire();
        s.release();
        s.acquire();
        s.release();
        assertEquals(1, s.availablePermits());
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Acquire(n) and release(n) of semaphore succeed if initially available
   */
  @Test
  public void testAcquireReleaseNInSameThread_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(1, true);
      try {
        s.release(1);
        s.acquire(1);
        s.release(2);
        s.acquire(2);
        s.release(3);
        s.acquire(3);
        s.release(4);
        s.acquire(4);
        s.release(5);
        s.acquire(5);
        assertEquals(1, s.availablePermits());
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Acquire(n) and release(n) of semaphore succeed if initially available
   */
  @Test
  public void testAcquireUninterruptiblyReleaseNInSameThread_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(1, true);
      try {
        s.release(1);
        s.acquireUninterruptibly(1);
        s.release(2);
        s.acquireUninterruptibly(2);
        s.release(3);
        s.acquireUninterruptibly(3);
        s.release(4);
        s.acquireUninterruptibly(4);
        s.release(5);
        s.acquireUninterruptibly(5);
        assertEquals(1, s.availablePermits());
      } finally {
      }
    }
    printFinish();
  }

  /**
   * release(n) in one thread enables timed acquire(n) in another thread
   */
  @Test
  public void testTimedAcquireReleaseNInSameThread_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(1, true);
      try {
        s.release(1);
        assertTrue(s.tryAcquire(1, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release(2);
        assertTrue(s.tryAcquire(2, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release(3);
        assertTrue(s.tryAcquire(3, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release(4);
        assertTrue(s.tryAcquire(4, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release(5);
        assertTrue(s.tryAcquire(5, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, s.availablePermits());
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * release in one thread enables timed acquire in another thread
   */
  @Test
  public void testTimedAcquireReleaseInSameThread_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(1, true);
      try {
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertTrue(s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release();
        assertEquals(1, s.availablePermits());
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A release in one thread enables an acquire in another thread
   */
  @Test
  public void testAcquireReleaseInDifferentThreads_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(0, true);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            s.acquire();
            s.acquire();
            s.acquire();
            s.acquire();
          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        s.release();
        s.release();
        s.release();
        s.release();
        s.release();
        s.release();
        t.join();
        assertEquals(2, s.availablePermits());
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * release(n) in one thread enables acquire(n) in another thread
   */
  @Test
  public void testAcquireReleaseNInDifferentThreads_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(0, true);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            s.acquire();
            s.release();
          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        s.release();
        s.acquire();
        s.release();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * release(n) in one thread enables acquire(n) in another thread
   */
  @Test
  public void testAcquireReleaseNInDifferentThreads_fair2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(0, true);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            s.acquire(2);
            s.acquire(2);
            s.release(4);
          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        s.release(6);
        s.acquire(2);
        s.acquire(2);
        s.release(2);
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * release in one thread enables timed acquire in another thread
   */
  @Test
  public void testTimedAcquireReleaseInDifferentThreads_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(1, true);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            boolean r = s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(!r);
            threadAssertTrue(r);
            r = s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(!r);
            threadAssertTrue(r);
            r = s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(!r);
            threadAssertTrue(r);
            r = s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(!r);
            threadAssertTrue(r);
            r = s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(!r);
            threadAssertTrue(r);
            r = s.tryAcquire(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(!r);
            threadAssertTrue(r);
          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        s.release();
        s.release();
        s.release();
        s.release();
        s.release();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * release(n) in one thread enables timed acquire(n) in another thread
   */
  @Test
  public void testTimedAcquireReleaseNInDifferentThreads_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(2, true);
      Thread t = new Thread(new Runnable() {
        public void run() {
          try {
            threadAssertTrue(s.tryAcquire(2, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
            s.release(2);
            threadAssertTrue(s.tryAcquire(2, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
            s.release(2);
          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(s.tryAcquire(2, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release(2);
        assertTrue(s.tryAcquire(2, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        s.release(2);
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A waiting acquire blocks interruptibly
   */
  @Test
  public void testAcquire_InterruptedException_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(0, true);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            s.acquire();
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A waiting acquire(n) blocks interruptibly
   */
  @Test
  public void testAcquireN_InterruptedException_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(2, true);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            s.acquire(3);
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   *  A waiting tryAcquire blocks interruptibly
   */
  @Test
  public void testTryAcquire_InterruptedException_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(0, true);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            boolean r = s.tryAcquire(MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS);
            if(r) threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   *  A waiting tryAcquire(n) blocks interruptibly
   */
  @Test
  public void testTryAcquireN_InterruptedException_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore s = new Semaphore(1, true);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            boolean r = s.tryAcquire(4, MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS);
            if(r) threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * getQueueLength reports number of waiting threads
   */
  @Test
  public void testGetQueueLength_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Semaphore lock = new Semaphore(1, true);
      Thread t1 = new Thread(new InterruptedLockRunnable(lock));
      Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
      try {
        assertEquals(0, lock.getQueueLength());
        lock.acquireUninterruptibly();
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
        lock.release();
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
   * a deserialized serialized semaphore has same number of permits
   */
  //@Test
  public void testSerialization_fair() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore l = new Semaphore(3, true);

      try {
        l.acquire();
        l.release();
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        Semaphore r = (Semaphore) in.readObject();
        assertEquals(3, r.availablePermits());
        assertTrue(r.isFair());
        r.acquire();
        r.release();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * toString indicates current number of permits
   */
  @Test
  public void testToString() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Semaphore s = new Semaphore(0);
      String us = s.toString();
      assertTrue(us.indexOf("Permits = 0") >= 0);
      s.release();
      String s1 = s.toString();
      assertTrue(s1.indexOf("Permits = 1") >= 0);
      s.release();
      String s2 = s.toString();
      assertTrue(s2.indexOf("Permits = 2") >= 0);
    }
    printFinish();
  }
}
