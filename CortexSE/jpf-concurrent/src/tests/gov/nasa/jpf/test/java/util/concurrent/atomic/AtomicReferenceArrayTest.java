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
import java.util.concurrent.atomic.*;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.atomic.AtomicReferenceArray
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AtomicReferenceArrayTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * constructor creates array of given size with all elements null
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReferenceArray<Integer> ai = new AtomicReferenceArray<Integer>(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertNull(ai.get(i));
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
        Integer[] a = null;
        AtomicReferenceArray<Integer> ai = new AtomicReferenceArray<Integer>(a);
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
      Integer[] a = {two, one, three, four, seven};
      AtomicReferenceArray<Integer> ai = new AtomicReferenceArray<Integer>(a);
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
      AtomicReferenceArray<Integer> ai = new AtomicReferenceArray<Integer>(SIZE);
      try {
        ai.get(SIZE);
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        ai.get(-1);
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        ai.set(SIZE, null);
      } catch (IndexOutOfBoundsException success) {
      }
      try {
        ai.set(-1, null);
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
      AtomicReferenceArray ai = new AtomicReferenceArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, one);
        assertEquals(one, ai.get(i));
        ai.set(i, two);
        assertEquals(two, ai.get(i));
        ai.set(i, m3);
        assertEquals(m3, ai.get(i));
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
      AtomicReferenceArray ai = new AtomicReferenceArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.lazySet(i, one);
        assertEquals(one, ai.get(i));
        ai.lazySet(i, two);
        assertEquals(two, ai.get(i));
        ai.lazySet(i, m3);
        assertEquals(m3, ai.get(i));
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
      AtomicReferenceArray ai = new AtomicReferenceArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, one);
        assertTrue(ai.compareAndSet(i, one, two));
        assertTrue(ai.compareAndSet(i, two, m4));
        assertEquals(m4, ai.get(i));
        assertFalse(ai.compareAndSet(i, m5, seven));
        assertFalse((seven.equals(ai.get(i))));
        assertTrue(ai.compareAndSet(i, m4, seven));
        assertEquals(seven, ai.get(i));
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
      final AtomicReferenceArray a = new AtomicReferenceArray(1);
      a.set(0, one);
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!a.compareAndSet(0, two, three)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(a.compareAndSet(0, one, two));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(a.get(0), three);
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
      AtomicReferenceArray ai = new AtomicReferenceArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, one);
        while (!ai.weakCompareAndSet(i, one, two));
        while (!ai.weakCompareAndSet(i, two, m4));
        assertEquals(m4, ai.get(i));
        while (!ai.weakCompareAndSet(i, m4, seven));
        assertEquals(seven, ai.get(i));
      }
    }
    printFinish();
  }

  /**
   * getAndSet returns previous value and sets to given value at given index
   */
  @Test
  public void testGetAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReferenceArray ai = new AtomicReferenceArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        ai.set(i, one);
        assertEquals(one, ai.getAndSet(i, zero));
        assertEquals(0, ai.getAndSet(i, m10));
        assertEquals(m10, ai.getAndSet(i, one));
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
      AtomicReferenceArray l = new AtomicReferenceArray(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        l.set(i, new Integer(-i));
      }

      try {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        AtomicReferenceArray r = (AtomicReferenceArray) in.readObject();
        assertEquals(l.length(), r.length());
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(r.get(i), l.get(i));
        }
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
      Integer[] a = {two, one, three, four, seven};
      AtomicReferenceArray<Integer> ai = new AtomicReferenceArray<Integer>(a);
      assertEquals(Arrays.toString(a), ai.toString());
    }
    printFinish();
  }
  
  @Test
  public void testKeepStrongReference() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicReferenceArray<ArrayList<Integer>> ref = new AtomicReferenceArray<ArrayList<Integer>>(5);
      ArrayList<Integer> list;
      int i;
      
      for (i = 5; --i >= 0; ) {
        list = new ArrayList<Integer>(1);
        list.add(i);
         
        ref.set(i, list);
      }
       
      list = null;         // Get rid of any local references
       
      System.gc();         // Set the GCNeeded flag
      Verify.getBoolean(); // Cause the state to be examined and hence GC to happen
      
      for (i = 5; --i >= 0; ) {
        list = ref.get(i);

        assertNotNull(list);
        assertTrue(list instanceof ArrayList);
        assertEquals(i, (int) list.get(0));
      }
    }
    printFinish();
  }
}
