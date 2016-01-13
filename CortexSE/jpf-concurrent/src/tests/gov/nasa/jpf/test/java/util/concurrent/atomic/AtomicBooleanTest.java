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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.atomic.AtomicBoolean
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AtomicBooleanTest extends TestCaseHelpers {

  /**
   * constructor initializes to given value
   */
  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * constructor initializes to given value
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicBoolean ai = new AtomicBoolean(true);
      assertEquals(true, ai.get());
    }
    printFinish();
  }

  /**
   * default constructed initializes to false
   */
  @Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicBoolean ai = new AtomicBoolean();
      assertEquals(false, ai.get());
    }
    printFinish();
  }

  /**
   * get returns the last value set
   */
  @Test
  public void testGetSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicBoolean ai = new AtomicBoolean(true);
      assertEquals(true, ai.get());
      ai.set(false);
      assertEquals(false, ai.get());
      ai.set(true);
      assertEquals(true, ai.get());
    }
    printFinish();
  }

  /**
   * get returns the last value lazySet in same thread
   */
  @Test
  public void testGetLazySet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        AtomicBoolean ai = new AtomicBoolean(true);
        assertEquals(true, ai.get());
        ai.lazySet(false);
        assertEquals(false, ai.get());
        ai.lazySet(true);
        assertEquals(true, ai.get());
      }catch(Exception e) {
        assertTrue(false);
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
      AtomicBoolean ai = new AtomicBoolean(true);
      assertTrue(ai.compareAndSet(true, false));
      assertEquals(false, ai.get());
      assertTrue(ai.compareAndSet(false, false));
      assertEquals(false, ai.get());
      assertFalse(ai.compareAndSet(true, false));
      assertFalse((ai.get()));
      assertTrue(ai.compareAndSet(false, true));
      assertEquals(true, ai.get());
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
      final AtomicBoolean ai = new AtomicBoolean(true);
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!ai.compareAndSet(false, true)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(ai.compareAndSet(true, false));
        t.join();
        assertFalse(t.isAlive());
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
      AtomicBoolean ai = new AtomicBoolean(true);
      while (!ai.weakCompareAndSet(true, false));
      assertEquals(false, ai.get());
      while (!ai.weakCompareAndSet(false, false));
      assertEquals(false, ai.get());
      while (!ai.weakCompareAndSet(false, true));
      assertEquals(true, ai.get());
    }
    printFinish();
  }

  /**
   * getAndSet returns previous value and sets to given value
   */
  @Test
  public void testGetAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicBoolean ai = new AtomicBoolean(true);
      assertEquals(true, ai.getAndSet(false));
      assertEquals(false, ai.getAndSet(false));
      assertEquals(false, ai.getAndSet(true));
      assertEquals(true, ai.get());
    }
    printFinish();
  }

  /**
   * a deserialized serialized atomic holds same value
   */
  //@Test
  public void testSerialization() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicBoolean l = new AtomicBoolean();

      try {
        l.set(true);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        AtomicBoolean r = (AtomicBoolean) in.readObject();
        assertEquals(l.get(), r.get());
      } catch (Exception e) {
        e.printStackTrace();
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
      AtomicBoolean ai = new AtomicBoolean();
      assertEquals(ai.toString(), Boolean.toString(false));
      ai.set(true);
      assertEquals(ai.toString(), Boolean.toString(true));
    }
    printFinish();
  }
}
