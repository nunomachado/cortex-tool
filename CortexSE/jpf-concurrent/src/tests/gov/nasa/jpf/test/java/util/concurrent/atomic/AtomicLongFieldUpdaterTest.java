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
package gov.nasa.jpf.test.java.util.concurrent.atomic;

import gov.nasa.jpf.test.java.util.concurrent.TestCaseHelpers;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.atomic.AtomicLongFieldUpdater
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AtomicLongFieldUpdaterTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  volatile long x = 0;
  int z;
  long w;

  /**
   * Construction with non-existent field throws RuntimeException
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "y");
        shouldThrow();
      } catch (RuntimeException rt) {
      }
    }
    printFinish();
  }

  /**
   * construction with field not of given type throws RuntimeException
   */
  @Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "z");
        shouldThrow();
      } catch (RuntimeException rt) {
      }
    }
    printFinish();
  }

  /**
   * construction with non-volatile field throws RuntimeException
   */
  @Test
  public void testConstructor3() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "w");
        shouldThrow();
      } catch (RuntimeException rt) {
      }
    }
    printFinish();
  }

  /**
   *  get returns the last value set or assigned
   */
  @Test
  public void testGetSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertEquals(1, a.get(this));
      a.set(this, 2);
      assertEquals(2, a.get(this));
      a.set(this, -3);
      assertEquals(-3, a.get(this));
    }
    printFinish();
  }

  /**
   *  get returns the last value lazySet by same thread
   */
  @Test
  public void testGetLazySet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertEquals(1, a.get(this));
      a.lazySet(this, 2);
      assertEquals(2, a.get(this));
      a.lazySet(this, -3);
      assertEquals(-3, a.get(this));
    }
    printFinish();
  }

  /**
   * compareAndSet succeeds in changing value if equal to expected else fails
   */
  @Test
  public void testCompareAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertTrue(a.compareAndSet(this, 1, 2));
      assertTrue(a.compareAndSet(this, 2, -4));
      assertEquals(-4, a.get(this));
      assertFalse(a.compareAndSet(this, -5, 7));
      assertFalse((7 == a.get(this)));
      assertTrue(a.compareAndSet(this, -4, 7));
      assertEquals(7, a.get(this));
    }
    printFinish();
  }

  /**
   * compareAndSet in one thread enables another waiting for value
   * to succeed
   */
  @Test
  public void testCompareAndSetInMultipleThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      x = 1;
      final AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }

      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!a.compareAndSet(AtomicLongFieldUpdaterTest.this, 2, 3)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(a.compareAndSet(this, 1, 2));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(a.get(this), 3);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * repeated weakCompareAndSet succeeds in changing value when equal
   * to expected
   */
  @Test
  public void testWeakCompareAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      while (!a.weakCompareAndSet(this, 1, 2));
      while (!a.weakCompareAndSet(this, 2, -4));
      assertEquals(-4, a.get(this));
      while (!a.weakCompareAndSet(this, -4, 7));
      assertEquals(7, a.get(this));
    }
    printFinish();
  }

  /**
   *  getAndSet returns previous value and sets to given value
   */
  @Test
  public void testGetAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertEquals(1, a.getAndSet(this, 0));
      assertEquals(0, a.getAndSet(this, -10));
      assertEquals(-10, a.getAndSet(this, 1));
    }
    printFinish();
  }

  /**
   * getAndAdd returns previous value and adds given value
   */
  @Test
  public void testGetAndAdd() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertEquals(1, a.getAndAdd(this, 2));
      assertEquals(3, a.get(this));
      assertEquals(3, a.getAndAdd(this, -4));
      assertEquals(-1, a.get(this));
    }
    printFinish();
  }

  /**
   * getAndDecrement returns previous value and decrements
   */
  @Test
  public void testGetAndDecrement() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertEquals(1, a.getAndDecrement(this));
      assertEquals(0, a.getAndDecrement(this));
      assertEquals(-1, a.getAndDecrement(this));
    }
    printFinish();
  }

  /**
   * getAndIncrement returns previous value and increments
   */
  @Test
  public void testGetAndIncrement() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertEquals(1, a.getAndIncrement(this));
      assertEquals(2, a.get(this));
      a.set(this, -2);
      assertEquals(-2, a.getAndIncrement(this));
      assertEquals(-1, a.getAndIncrement(this));
      assertEquals(0, a.getAndIncrement(this));
      assertEquals(1, a.get(this));
    }
    printFinish();
  }

  /**
   * addAndGet adds given value to current, and returns current value
   */
  @Test
  public void testAddAndGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertEquals(3, a.addAndGet(this, 2));
      assertEquals(3, a.get(this));
      assertEquals(-1, a.addAndGet(this, -4));
      assertEquals(-1, a.get(this));
    }
    printFinish();
  }

  /**
   *  decrementAndGet decrements and returns current value
   */
  @Test
  public void testDecrementAndGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertEquals(0, a.decrementAndGet(this));
      assertEquals(-1, a.decrementAndGet(this));
      assertEquals(-2, a.decrementAndGet(this));
      assertEquals(-2, a.get(this));
    }
    printFinish();
  }

  /**
   * incrementAndGet increments and returns current value
   */
  @Test
  public void testIncrementAndGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicLongFieldUpdater<AtomicLongFieldUpdaterTest> a;
      try {
        a = AtomicLongFieldUpdater.newUpdater(AtomicLongFieldUpdaterTest.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = 1;
      assertEquals(2, a.incrementAndGet(this));
      assertEquals(2, a.get(this));
      a.set(this, -2);
      assertEquals(-1, a.incrementAndGet(this));
      assertEquals(0, a.incrementAndGet(this));
      assertEquals(1, a.incrementAndGet(this));
      assertEquals(1, a.get(this));
    }
    printFinish();
  }
}
