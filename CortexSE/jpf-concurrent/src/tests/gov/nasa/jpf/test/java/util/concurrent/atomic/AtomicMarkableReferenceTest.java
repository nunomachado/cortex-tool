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
 * JPF test driver for java.util.concurrent.atomic.AtomicMarkableReference
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AtomicMarkableReferenceTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   *  constructor initializes to given reference and mark
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      AtomicMarkableReference ai = new AtomicMarkableReference(one, false);
      assertEquals(one, ai.getReference());
      assertFalse(ai.isMarked());
      AtomicMarkableReference a2 = new AtomicMarkableReference(null, true);
      assertNull(a2.getReference());
      assertTrue(a2.isMarked());
    }
    printFinish();
  }

  /**
   *  get returns the last values of reference and mark set
   */
  @Test
  public void testGetSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      boolean[] mark = new boolean[1];
      AtomicMarkableReference ai = new AtomicMarkableReference(one, false);
      assertEquals(one, ai.getReference());
      assertFalse(ai.isMarked());
      assertEquals(one, ai.get(mark));
      assertFalse(mark[0]);
      ai.set(two, false);
      assertEquals(two, ai.getReference());
      assertFalse(ai.isMarked());
      assertEquals(two, ai.get(mark));
      assertFalse(mark[0]);
      ai.set(one, true);
      assertEquals(one, ai.getReference());
      assertTrue(ai.isMarked());
      assertEquals(one, ai.get(mark));
      assertTrue(mark[0]);
    }
    printFinish();
  }

  /**
   * attemptMark succeeds in single thread
   */
  @Test
  public void testAttemptMark() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      boolean[] mark = new boolean[1];
      AtomicMarkableReference ai = new AtomicMarkableReference(one, false);
      assertFalse(ai.isMarked());
      assertTrue(ai.attemptMark(one, true));
      assertTrue(ai.isMarked());
      assertEquals(one, ai.get(mark));
      assertTrue(mark[0]);
    }
    printFinish();
  }

  /**
   * compareAndSet succeeds in changing values if equal to expected reference
   * and mark else fails
   */
  @Test
  public void testCompareAndSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      boolean[] mark = new boolean[1];
      AtomicMarkableReference ai = new AtomicMarkableReference(one, false);
      assertEquals(one, ai.get(mark));
      assertFalse(ai.isMarked());
      assertFalse(mark[0]);

      assertTrue(ai.compareAndSet(one, two, false, false));
      assertEquals(two, ai.get(mark));
      assertFalse(mark[0]);

      assertTrue(ai.compareAndSet(two, m3, false, true));
      assertEquals(m3, ai.get(mark));
      assertTrue(mark[0]);

      assertFalse(ai.compareAndSet(two, m3, true, true));
      assertEquals(m3, ai.get(mark));
      assertTrue(mark[0]);
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
      final AtomicMarkableReference ai = new AtomicMarkableReference(one, false);
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!ai.compareAndSet(two, three, false, false)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(ai.compareAndSet(one, two, false, false));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(ai.getReference(), three);
        assertFalse(ai.isMarked());
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * compareAndSet in one thread enables another waiting for mark value
   * to succeed
   */
  @Test
  public void testCompareAndSetInMultipleThreads2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final AtomicMarkableReference ai = new AtomicMarkableReference(one, false);
      Thread t = new Thread(new Runnable() {

        public void run() {
          while (!ai.compareAndSet(one, one, true, false)) {
            Thread.yield();
          }
        }
      });
      try {
        t.start();
        assertTrue(ai.compareAndSet(one, one, false, true));
        t.join();
        assertFalse(t.isAlive());
        assertEquals(ai.getReference(), one);
        assertFalse(ai.isMarked());
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
      boolean[] mark = new boolean[1];
      AtomicMarkableReference ai = new AtomicMarkableReference(one, false);
      assertEquals(one, ai.get(mark));
      assertFalse(ai.isMarked());
      assertFalse(mark[0]);

      while (!ai.weakCompareAndSet(one, two, false, false));
      assertEquals(two, ai.get(mark));
      assertFalse(mark[0]);

      while (!ai.weakCompareAndSet(two, m3, false, true));
      assertEquals(m3, ai.get(mark));
      assertTrue(mark[0]);
    }
    printFinish();
  }

  @Test
  public void testKeepStrongReference() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ArrayList<Integer> list = new ArrayList<Integer>(1);
      AtomicMarkableReference<ArrayList<Integer>> ref = new AtomicMarkableReference<ArrayList<Integer>>(list, true);
      boolean mark[];
       
      list.add(1);
       
      list = null;         // Get rid of any local references
       
      System.gc();         // Set the GCNeeded flag
      Verify.getBoolean(); // Cause the state to be examined and hence GC to happen
       
      mark = new boolean[1];
      list = ref.get(mark);
       
      assertNotNull(list);
      assertTrue(list instanceof ArrayList);
      assertEquals(1, (int) list.get(0));
      assertTrue(mark[0]);
    }
    printFinish();
  }
}

