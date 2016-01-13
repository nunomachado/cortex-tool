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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.atomic.AtomicReferenceFieldUpdater
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AtomicReferenceFieldUpdaterTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  volatile Integer x = null;
  Object z;
  Integer w;

  /**
   * Construction with non-existent field throws RuntimeException
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest, Integer> a = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldUpdaterTest.class, Integer.class, "y");
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
        AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest, Integer> a = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldUpdaterTest.class, Integer.class, "z");
        shouldThrow();
      } catch (RuntimeException rt) {
      }
    }
    printFinish();
  }

  /**
   * Constructor with non-volatile field throws exception
   */
  @Test
  public void testConstructor3() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest, Integer> a = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldUpdaterTest.class, Integer.class, "w");
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
      AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest, Integer> a;
      try {
        a = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldUpdaterTest.class, Integer.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = one;
      assertEquals(one, a.get(this));
      a.set(this, two);
      assertEquals(two, a.get(this));
      a.set(this, m3);
      assertEquals(m3, a.get(this));
    }
    printFinish();
  }

  /**
   *  get returns the last value lazySet by same thread
   */
  @Test
  public void testGetLazySet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest, Integer> a;
      try {
        a = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldUpdaterTest.class, Integer.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = one;
      assertEquals(one, a.get(this));
      a.lazySet(this, two);
      assertEquals(two, a.get(this));
      a.lazySet(this, m3);
      assertEquals(m3, a.get(this));
    }
    printFinish();
  }

  /**
   * compareAndSet succeeds in changing value if equal to expected else fails
   */
  @Test
  public void testCompareAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest, Integer> a;
      try {
        a = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldUpdaterTest.class, Integer.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = one;
      assertTrue(a.compareAndSet(this, one, two));
      assertTrue(a.compareAndSet(this, two, m4));
      assertEquals(m4, a.get(this));
      assertFalse(a.compareAndSet(this, m5, seven));
      assertFalse((seven == a.get(this)));
      assertTrue(a.compareAndSet(this, m4, seven));
      assertEquals(seven, a.get(this));
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
      x = one;
      final AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest, Integer> a;
      try {
        a = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldUpdaterTest.class, Integer.class, "x");
      } catch (RuntimeException ok) {
        return;
      }

      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!a.compareAndSet(AtomicReferenceFieldUpdaterTest.this, two, three)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(a.compareAndSet(this, one, two));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(a.get(this), three);
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
      AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest, Integer> a;
      try {
        a = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldUpdaterTest.class, Integer.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = one;
      while (!a.weakCompareAndSet(this, one, two));
      while (!a.weakCompareAndSet(this, two, m4));
      assertEquals(m4, a.get(this));
      while (!a.weakCompareAndSet(this, m4, seven));
      assertEquals(seven, a.get(this));
    }
    printFinish();
  }

  /**
   * getAndSet returns previous value and sets to given value
   */
  @Test
  public void testGetAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReferenceFieldUpdater<AtomicReferenceFieldUpdaterTest, Integer> a;
      try {
        a = AtomicReferenceFieldUpdater.newUpdater(AtomicReferenceFieldUpdaterTest.class, Integer.class, "x");
      } catch (RuntimeException ok) {
        return;
      }
      x = one;
      assertEquals(one, a.getAndSet(this, zero));
      assertEquals(zero, a.getAndSet(this, m10));
      assertEquals(m10, a.getAndSet(this, 1));
    }
    printFinish();
  }
}
