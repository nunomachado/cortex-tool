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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.atomic.AtomicIntegerArray
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AtomicIntegerArrayTest extends TestCaseHelpers {

  /**
   * constructor initializes to given value
   */
  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * constructor creates array of given size with all elements zero
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertEquals(0, ai.get(i));
      }
    }
    printFinish();
  }

  /**
   * constructor with null array throws NPE
   */
  @Test
  public void testConstructor2NPE() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        int[] a = null;
        AtomicIntegerArray ai = new AtomicIntegerArray(a);
      } catch (NullPointerException success) {
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * constructor with array is of same size and has all elements
   */
  @Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      int[] a = {17, 3, -42, 99, -7};
      AtomicIntegerArray ai = new AtomicIntegerArray(a);
      assertEquals(a.length, ai.length());
      for (int i = 0; i < a.length; ++i) {
        assertEquals(a[i], ai.get(i));
      }
    }
    printFinish();
  }

  /**
   * get and set for out of bound indices throw IndexOutOfBoundsException
   */
  @Test
  public void testIndexing() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      try {
        ai.get(SIZE);
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        ai.get(-1);
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        ai.set(SIZE, 0);
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        ai.set(-1, 0);
      } catch (IndexOutOfBoundsException success) {
      }
    }
    printFinish();
  }

  /**
   * get returns the last value set at index
   */
  @Test
  public void testGetSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        assertEquals(1, ai.get(i));
        ai.set(i, 2);
        assertEquals(2, ai.get(i));
        ai.set(i, -3);
        assertEquals(-3, ai.get(i));
      }
    }
    printFinish();
  }

  /**
   * get returns the last value lazySet at index by same thread
   */
  @Test
  public void testGetLazySet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.lazySet(i, 1);
        assertEquals(1, ai.get(i));
        ai.lazySet(i, 2);
        assertEquals(2, ai.get(i));
        ai.lazySet(i, -3);
        assertEquals(-3, ai.get(i));
      }
    }
    printFinish();
  }

  /**
   * compareAndSet succeeds in changing value if equal to expected else fails
   */
  @Test
  public void testCompareAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        assertTrue(ai.compareAndSet(i, 1, 2));
        assertTrue(ai.compareAndSet(i, 2, -4));
        assertEquals(-4, ai.get(i));
        assertFalse(ai.compareAndSet(i, -5, 7));
        assertFalse((7 == ai.get(i)));
        assertTrue(ai.compareAndSet(i, -4, 7));
        assertEquals(7, ai.get(i));
      }
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
      final AtomicIntegerArray a = new AtomicIntegerArray(1);
      a.set(0, 1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!a.compareAndSet(0, 2, 3)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(a.compareAndSet(0, 1, 2));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(a.get(0), 3);
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
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        while (!ai.weakCompareAndSet(i, 1, 2));
        while (!ai.weakCompareAndSet(i, 2, -4));
        assertEquals(-4, ai.get(i));
        while (!ai.weakCompareAndSet(i, -4, 7));
        assertEquals(7, ai.get(i));
      }
    }
    printFinish();
  }

  /**
   *  getAndSet returns previous value and sets to given value at given index
   */
  @Test
  public void testGetAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        assertEquals(1, ai.getAndSet(i, 0));
        assertEquals(0, ai.getAndSet(i, -10));
        assertEquals(-10, ai.getAndSet(i, 1));
      }
    }
    printFinish();
  }

  /**
   *  getAndAdd returns previous value and adds given value
   */
  @Test
  public void testGetAndAdd() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        assertEquals(1, ai.getAndAdd(i, 2));
        assertEquals(3, ai.get(i));
        assertEquals(3, ai.getAndAdd(i, -4));
        assertEquals(-1, ai.get(i));
      }
    }
    printFinish();
  }

  /**
   * getAndDecrement returns previous value and decrements
   */
  @Test
  public void testGetAndDecrement() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        assertEquals(1, ai.getAndDecrement(i));
        assertEquals(0, ai.getAndDecrement(i));
        assertEquals(-1, ai.getAndDecrement(i));
      }
    }
    printFinish();
  }

  /**
   * getAndIncrement returns previous value and increments
   */
  @Test
  public void testGetAndIncrement() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        assertEquals(1, ai.getAndIncrement(i));
        assertEquals(2, ai.get(i));
        ai.set(i, -2);
        assertEquals(-2, ai.getAndIncrement(i));
        assertEquals(-1, ai.getAndIncrement(i));
        assertEquals(0, ai.getAndIncrement(i));
        assertEquals(1, ai.get(i));
      }
    }
    printFinish();
  }

  /**
   *  addAndGet adds given value to current, and returns current value
   */
  @Test
  public void testAddAndGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        assertEquals(3, ai.addAndGet(i, 2));
        assertEquals(3, ai.get(i));
        assertEquals(-1, ai.addAndGet(i, -4));
        assertEquals(-1, ai.get(i));
      }
    }
    printFinish();
  }

  /**
   * decrementAndGet decrements and returns current value
   */
  @Test
  public void testDecrementAndGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        assertEquals(0, ai.decrementAndGet(i));
        assertEquals(-1, ai.decrementAndGet(i));
        assertEquals(-2, ai.decrementAndGet(i));
        assertEquals(-2, ai.get(i));
      }
    }
    printFinish();
  }

  /**
   *  incrementAndGet increments and returns current value
   */
  @Test
  public void testIncrementAndGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray ai = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, 1);
        assertEquals(2, ai.incrementAndGet(i));
        assertEquals(2, ai.get(i));
        ai.set(i, -2);
        assertEquals(-1, ai.incrementAndGet(i));
        assertEquals(0, ai.incrementAndGet(i));
        assertEquals(1, ai.incrementAndGet(i));
        assertEquals(1, ai.get(i));
      }
    }
    printFinish();
  }
  static final int COUNTDOWN = 1;

  class Counter implements Runnable {

    final AtomicIntegerArray ai;
    volatile int counts;

    Counter(AtomicIntegerArray a) {
      ai = a;
    }

    public void run() {
      for (;;) {
        boolean done = true;
        for (int i = 0; i < ai.length(); ++i) {
          int v = ai.get(i);
          threadAssertTrue(v >= 0);
          if (v != 0) {
            done = false;
            if (ai.compareAndSet(i, v, v - 1)) {
              ++counts;
            }
          }
        }
        if (done) {
          break;
        }
      }
    }
  }

  /**
   * Multiple threads using same array of counters successfully
   * update a number of times equal to total count
   */
  @Test
  public void testCountingInMultipleThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        final AtomicIntegerArray ai = new AtomicIntegerArray(SIZE/2);
        for (int i = 0; i < SIZE/2; ++i) {
          ai.set(i, COUNTDOWN);
        }
        Counter c1 = new Counter(ai);
        Counter c2 = new Counter(ai);
        Thread t1 = new Thread(c1);
        Thread t2 = new Thread(c2);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(c1.counts + c2.counts, SIZE/2 * COUNTDOWN);
      } catch (InterruptedException ie) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * a deserialized serialized array holds same values
   */
  //@Test
  public void testSerialization() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicIntegerArray l = new AtomicIntegerArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        l.set(i, -i);
      }

      try {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        AtomicIntegerArray r = (AtomicIntegerArray) in.readObject();
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(l.get(i), r.get(i));
        }
      } catch (Exception e) {
        e.printStackTrace();
        unexpectedException();
      }
    }
  }

  /**
   * toString returns current value.
   */
  @Test
  public void testToString() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      int[] a = {17, 3, -42, 99, -7};
      AtomicIntegerArray ai = new AtomicIntegerArray(a);
      assertEquals(Arrays.toString(a), ai.toString());
    }
    printFinish();
  }
}
