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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.atomic.AtomicInteger
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AtomicIntegerTest extends TestCaseHelpers {



  private final static String[] JPF_ARGS = {
  };

  public static void main (String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * constructor initializes to given value
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(1, ai.get());
    }
    printFinish();
  }

  /**
   * default constructed initializes to zero
   */
  @Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger();
      assertEquals(0, ai.get());
    }
    printFinish();
  }

  /**
   * get returns the last value set
   */
  @Test
  public void testGetSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(1, ai.get());
      ai.set(2);
      assertEquals(2, ai.get());
      ai.set(-3);
      assertEquals(-3, ai.get());
    }
    printFinish();
  }

  /**
   * get returns the last value lazySet in same thread
   */
  @Test
  public void testGetLazySet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(1, ai.get());
      ai.lazySet(2);
      assertEquals(2, ai.get());
      ai.lazySet(-3);
      assertEquals(-3, ai.get());
    }
    printFinish();
  }

  /**
   * compareAndSet succeeds in changing value if equal to expected else fails
   */
  @Test
  public void testCompareAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertTrue(ai.compareAndSet(1, 2));
      assertTrue(ai.compareAndSet(2, -4));
      assertEquals(-4, ai.get());
      assertFalse(ai.compareAndSet(-5, 7));
      assertFalse((7 == ai.get()));
      assertTrue(ai.compareAndSet(-4, 7));
      assertEquals(7, ai.get());
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
      final AtomicInteger ai = new AtomicInteger(1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!ai.compareAndSet(2, 3)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(ai.compareAndSet(1, 2));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(ai.get(), 3);
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
      AtomicInteger ai = new AtomicInteger(1);
      while (!ai.weakCompareAndSet(1, 2));
      while (!ai.weakCompareAndSet(2, -4));
      assertEquals(-4, ai.get());
      while (!ai.weakCompareAndSet(-4, 7));
      assertEquals(7, ai.get());
    }
    printFinish();
  }

  /**
   * getAndSet returns previous value and sets to given value
   */
  @Test
  public void testGetAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(1, ai.getAndSet(0));
      assertEquals(0, ai.getAndSet(-10));
      assertEquals(-10, ai.getAndSet(1));
    }
    printFinish();
  }

  /**
   * getAndAdd returns previous value and adds given value
   */
  @Test
  public void testGetAndAdd() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(1, ai.getAndAdd(2));
      assertEquals(3, ai.get());
      assertEquals(3, ai.getAndAdd(-4));
      assertEquals(-1, ai.get());
    }
    printFinish();
  }

  /**
   * getAndDecrement returns previous value and decrements
   */
  @Test
  public void testGetAndDecrement() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(1, ai.getAndDecrement());
      assertEquals(0, ai.getAndDecrement());
      assertEquals(-1, ai.getAndDecrement());
    }
    printFinish();
  }

  /**
   * getAndIncrement returns previous value and increments
   */
  @Test
  public void testGetAndIncrement() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(1, ai.getAndIncrement());
      assertEquals(2, ai.get());
      ai.set(-2);
      assertEquals(-2, ai.getAndIncrement());
      assertEquals(-1, ai.getAndIncrement());
      assertEquals(0, ai.getAndIncrement());
      assertEquals(1, ai.get());
    }
    printFinish();
  }

  /**
   * addAndGet adds given value to current, and returns current value
   */
  @Test
  public void testAddAndGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(3, ai.addAndGet(2));
      assertEquals(3, ai.get());
      assertEquals(-1, ai.addAndGet(-4));
      assertEquals(-1, ai.get());
    }
    printFinish();
  }

  /**
   * decrementAndGet decrements and returns current value
   */
  @Test
  public void testDecrementAndGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(0, ai.decrementAndGet());
      assertEquals(-1, ai.decrementAndGet());
      assertEquals(-2, ai.decrementAndGet());
      assertEquals(-2, ai.get());
    }
    printFinish();
  }

  /**
   * incrementAndGet increments and returns current value
   */
  @Test
  public void testIncrementAndGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger(1);
      assertEquals(2, ai.incrementAndGet());
      assertEquals(2, ai.get());
      ai.set(-2);
      assertEquals(-1, ai.incrementAndGet());
      assertEquals(0, ai.incrementAndGet());
      assertEquals(1, ai.incrementAndGet());
      assertEquals(1, ai.get());
    }
    printFinish();
  }

  /**
   * a deserialized serialized atomic holds same value
   */
  //@Test
  public void testSerialization() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger l = new AtomicInteger();

      try {
        l.set(22);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        AtomicInteger r = (AtomicInteger) in.readObject();
        assertEquals(l.get(), r.get());
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * toString returns current value.
   */
  @Test
  public void testToString() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger();
      for (int i = -12; i < 6; ++i) {
        ai.set(i);
        assertEquals(ai.toString(), Integer.toString(i));
      }
    }
    printFinish();
  }

  /**
   * intValue returns current value.
   */
  @Test
  public void testIntValue() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger();
      for (int i = -12; i < 6; ++i) {
        ai.set(i);
        assertEquals(i, ai.intValue());
      }
    }
    printFinish();
  }

  /**
   * longValue returns current value.
   */
  @Test
  public void testLongValue() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger();
      for (int i = -12; i < 6; ++i) {
        ai.set(i);
        assertEquals((long) i, ai.longValue());
      }
    }
    printFinish();
  }

  /**
   * floatValue returns current value.
   */
  @Test
  public void testFloatValue() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger();
      for (int i = -12; i < 6; ++i) {
        ai.set(i);
        assertEquals((float) i, ai.floatValue(),0);
      }
    }
    printFinish();
  }

  /**
   * doubleValue returns current value.
   */
  @Test
  public void testDoubleValue() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicInteger ai = new AtomicInteger();
      for (int i = -12; i < 6; ++i) {
        ai.set(i);
        assertEquals((double) i, ai.doubleValue(),0);
      }
    }
    printFinish();
  }
}
