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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.LinkedBlockingQueue
 * 
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class CountDownLatchTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {"+listener=gov.nasa.jpf.concurrent.ObjectRemovalListener"};

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * negative constructor argument throws IAE
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        new CountDownLatch(-1);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      }
    }
    printFinish();
  }

  /**
   * getCount returns initial count and decreases after countDown
   */
  @Test
  public void testGetCount() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CountDownLatch l = new CountDownLatch(2);
      assertEquals(2, l.getCount());
      l.countDown();
      assertEquals(1, l.getCount());
    }
    printFinish();
  }

  /**
   * countDown decrements count when positive and has no effect when zero
   */
  @Test
  public void testCountDown() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CountDownLatch l = new CountDownLatch(1);
      assertEquals(1, l.getCount());
      l.countDown();
      assertEquals(0, l.getCount());
      l.countDown();
      assertEquals(0, l.getCount());
    }
    printFinish();
  }

  /**
   * await returns after countDown to zero, but not before
   */
  @Test
  public void testAwait() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CountDownLatch l = new CountDownLatch(2);

      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            threadAssertTrue(l.getCount() > 0);
            l.await();
            threadAssertTrue(l.getCount() == 0);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      t.start();
      try {
        assertEquals(l.getCount(), 2);
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        l.countDown();
        assertEquals(l.getCount(), 1);
        l.countDown();
        assertEquals(l.getCount(), 0);
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * timed await returns after countDown to zero
   */
  @Test
  public void testTimedAwait() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CountDownLatch l = new CountDownLatch(2);

      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            threadAssertTrue(l.getCount() > 0);
            boolean b = l.await(SMALL_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(!b);
            threadAssertTrue(b);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.TIMED_WAITING);
        assertEquals(l.getCount(), 2);
        Thread.sleep(SHORT_DELAY_MS);
        l.countDown();
        assertEquals(l.getCount(), 1);
        l.countDown();
        assertEquals(l.getCount(), 0);
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * await throws IE if interrupted before counted down
   */
  @Test
  public void testAwait_InterruptedException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CountDownLatch l = new CountDownLatch(1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            threadAssertTrue(l.getCount() > 0);
            l.await();
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        assertEquals(l.getCount(), 1);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * timed await throws IE if interrupted before counted down
   */
  @Test
  public void testTimedAwait_InterruptedException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CountDownLatch l = new CountDownLatch(1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            threadAssertTrue(l.getCount() > 0);
            l.await(MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(true);
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        assertEquals(l.getCount(), 1);
        Verify.ignoreIf(t.getState() != Thread.State.TIMED_WAITING);
        t.interrupt();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * timed await times out if not counted down before timeout
   */
  @Test
  public void testAwaitTimeout() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CountDownLatch l = new CountDownLatch(1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            threadAssertTrue(l.getCount() > 0);
            threadAssertFalse(l.await(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
            threadAssertTrue(l.getCount() > 0);
          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      t.start();
      try {
        assertEquals(l.getCount(), 1);
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * toString indicates current count
   */
  @Test
  public void testToString() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      CountDownLatch s = new CountDownLatch(2);
      String us = s.toString();
      assertTrue(us.indexOf("Count = 2") >= 0);
      s.countDown();
      String s1 = s.toString();
      assertTrue(s1.indexOf("Count = 1") >= 0);
      s.countDown();
      String s2 = s.toString();
      assertTrue(s2.indexOf("Count = 0") >= 0);
    }
    printFinish();
  }
}
