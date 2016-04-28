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
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.atomic.AtomicStampedReference
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AtomicStampedReferenceTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * constructor initializes to given reference and stamp
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicStampedReference ai = new AtomicStampedReference(one, 0);
      assertEquals(one, ai.getReference());
      assertEquals(0, ai.getStamp());
      AtomicStampedReference a2 = new AtomicStampedReference(null, 1);
      assertNull(a2.getReference());
      assertEquals(1, a2.getStamp());
    }
    printFinish();
  }

  /**
   *  get returns the last values of reference and stamp set
   */
  @Test
  public void testGetSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      int[] mark = new int[1];
      AtomicStampedReference ai = new AtomicStampedReference(one, 0);
      assertEquals(one, ai.getReference());
      assertEquals(0, ai.getStamp());
      assertEquals(one, ai.get(mark));
      assertEquals(0, mark[0]);
      ai.set(two, 0);
      assertEquals(two, ai.getReference());
      assertEquals(0, ai.getStamp());
      assertEquals(two, ai.get(mark));
      assertEquals(0, mark[0]);
      ai.set(one, 1);
      assertEquals(one, ai.getReference());
      assertEquals(1, ai.getStamp());
      assertEquals(one, ai.get(mark));
      assertEquals(1, mark[0]);
    }
    printFinish();
  }

  /**
   *  attemptStamp succeeds in single thread
   */
  @Test
  public void testAttemptStamp() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      int[] mark = new int[1];
      AtomicStampedReference ai = new AtomicStampedReference(one, 0);
      assertEquals(0, ai.getStamp());
      assertTrue(ai.attemptStamp(one, 1));
      assertEquals(1, ai.getStamp());
      assertEquals(one, ai.get(mark));
      assertEquals(1, mark[0]);
    }
    printFinish();
  }

  /**
   * compareAndSet succeeds in changing values if equal to expected reference
   * and stamp else fails
   */
  @Test
  public void testCompareAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      int[] mark = new int[1];
      AtomicStampedReference ai = new AtomicStampedReference(one, 0);
      assertEquals(one, ai.get(mark));
      assertEquals(0, ai.getStamp());
      assertEquals(0, mark[0]);

      assertTrue(ai.compareAndSet(one, two, 0, 0));
      assertEquals(two, ai.get(mark));
      assertEquals(0, mark[0]);

      assertTrue(ai.compareAndSet(two, m3, 0, 1));
      assertEquals(m3, ai.get(mark));
      assertEquals(1, mark[0]);

      assertFalse(ai.compareAndSet(two, m3, 1, 1));
      assertEquals(m3, ai.get(mark));
      assertEquals(1, mark[0]);
    }
    printFinish();
  }

  /**
   * compareAndSet in one thread enables another waiting for reference value
   * to succeed
   */
  @Test
  public void testCompareAndSetInMultipleThreads() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final AtomicStampedReference ai = new AtomicStampedReference(one, 0);
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!ai.compareAndSet(two, three, 0, 0)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(ai.compareAndSet(one, two, 0, 0));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(ai.getReference(), three);
        assertEquals(ai.getStamp(), 0);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * compareAndSet in one thread enables another waiting for stamp value
   * to succeed
   */
  @Test
  public void testCompareAndSetInMultipleThreads2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final AtomicStampedReference ai = new AtomicStampedReference(one, 0);
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!ai.compareAndSet(one, one, 1, 2)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(ai.compareAndSet(one, one, 0, 1));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(ai.getReference(), one);
        assertEquals(ai.getStamp(), 2);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * repeated weakCompareAndSet succeeds in changing values when equal
   * to expected
   */
  @Test
  public void testWeakCompareAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      int[] mark = new int[1];
      AtomicStampedReference ai = new AtomicStampedReference(one, 0);
      assertEquals(one, ai.get(mark));
      assertEquals(0, ai.getStamp());
      assertEquals(0, mark[0]);

      while (!ai.weakCompareAndSet(one, two, 0, 0));
      assertEquals(two, ai.get(mark));
      assertEquals(0, mark[0]);

      while (!ai.weakCompareAndSet(two, m3, 0, 1));
      assertEquals(m3, ai.get(mark));
      assertEquals(1, mark[0]);
    }
    printFinish();
  }

  @Test
  public void testKeepStrongReference() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ArrayList<Integer> list = new ArrayList<Integer>(1);
      AtomicStampedReference<ArrayList<Integer>> ref = new AtomicStampedReference<ArrayList<Integer>>(list, 10);
      int mark[] = new int[1];
       
      list.add(1);
       
      list = null;         // Get rid of any local references
       
      System.gc();         // Set the GCNeeded flag
      Verify.getBoolean(); // Cause the state to be examined and hence GC to happen
       
      list = ref.get(mark);
       
      assertNotNull(list);
      assertTrue(list instanceof ArrayList);
      assertEquals(1, (int) list.get(0));
      assertEquals(10, mark[0]);
    }
    printFinish();
  }
}
