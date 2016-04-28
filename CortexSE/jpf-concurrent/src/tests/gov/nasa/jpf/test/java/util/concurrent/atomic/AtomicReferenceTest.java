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

import gov.nasa.jpf.jvm.*;
import gov.nasa.jpf.test.java.util.concurrent.TestCaseHelpers;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.atomic.AtomicReference
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AtomicReferenceTest extends TestCaseHelpers {

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
      AtomicReference ai = new AtomicReference(one);
      assertEquals(one, ai.get());
    }
    printFinish();
  }

  /**
   * default constructed initializes to null
   */
  @Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReference ai = new AtomicReference();
      assertNull(ai.get());
    }
    printFinish();
  }

  /**
   * get returns the last value set
   */
  @Test
  public void testGetSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReference ai = new AtomicReference(one);
      assertEquals(one, ai.get());
      ai.set(two);
      assertEquals(two, ai.get());
      ai.set(m3);
      assertEquals(m3, ai.get());
    }
    printFinish();
  }

  /**
   * get returns the last value lazySet in same thread
   */
  @Test
  public void testGetLazySet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReference ai = new AtomicReference(one);
      assertEquals(one, ai.get());
      ai.lazySet(two);
      assertEquals(two, ai.get());
      ai.lazySet(m3);
      assertEquals(m3, ai.get());
    }
    printFinish();
  }

  /**
   * compareAndSet succeeds in changing value if equal to expected else fails
   */
  @Test
  public void testCompareAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReference ai = new AtomicReference(one);
      assertTrue(ai.compareAndSet(one, two));
      assertTrue(ai.compareAndSet(two, m4));
      assertEquals(m4, ai.get());
      assertFalse(ai.compareAndSet(m5, seven));
      assertFalse((seven.equals(ai.get())));
      assertTrue(ai.compareAndSet(m4, seven));
      assertEquals(seven, ai.get());
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
      final AtomicReference ai = new AtomicReference(one);
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!ai.compareAndSet(two, three)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(ai.compareAndSet(one, two));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(ai.get(), three);
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
      AtomicReference ai = new AtomicReference(one);
      while (!ai.weakCompareAndSet(one, two));
      while (!ai.weakCompareAndSet(two, m4));
      assertEquals(m4, ai.get());
      while (!ai.weakCompareAndSet(m4, seven));
      assertEquals(seven, ai.get());
    }
    printFinish();
  }

  /**
   * getAndSet returns previous value and sets to given value
   */
  @Test
  public void testGetAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReference ai = new AtomicReference(one);
      assertEquals(one, ai.getAndSet(zero));
      assertEquals(zero, ai.getAndSet(m10));
      assertEquals(m10, ai.getAndSet(one));
    }
    printFinish();
  }

  /**
   * a deserialized serialized atomic holds same value
   */
  //@Test
  public void testSerialization() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReference l = new AtomicReference();

      try {
        l.set(one);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        AtomicReference r = (AtomicReference) in.readObject();
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
      AtomicReference<Integer> ai = new AtomicReference<Integer>(one);
      assertEquals(ai.toString(), one.toString());
      ai.set(two);
      assertEquals(ai.toString(), two.toString());
    }
    printFinish();
  }
  
  @Test
  public void testKeepStrongReference() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ArrayList<Integer> list = new ArrayList<Integer>(1);
      AtomicReference<ArrayList<Integer>> ref = new AtomicReference<ArrayList<Integer>>(list);
      
      list.add(1);
      
      list = null;         // Get rid of any local references
      
      System.gc();         // Set the GCNeeded flag
      Verify.getBoolean(); // Cause the state to be examined and hence GC to happen
      
      list = ref.get();
      
      assertNotNull(list);
      assertTrue(list instanceof ArrayList);
      assertTrue(list.get(0) == 1);
    }
    printFinish();
  }
}
