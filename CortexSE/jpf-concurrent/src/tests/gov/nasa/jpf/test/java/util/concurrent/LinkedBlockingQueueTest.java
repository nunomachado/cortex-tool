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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.LinkedBlockingQueue
 * 
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class LinkedBlockingQueueTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {"+listener=gov.nasa.jpf.concurrent.ObjectRemovalListener"};

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * Create a queue of given size containing consecutive
   * Integers 0 ... n.
   */
  private LinkedBlockingQueue populatedQueue(int n) {
    LinkedBlockingQueue q = new LinkedBlockingQueue(n);
    assertTrue(q.isEmpty());
    for (int i = 0; i < n; i++) {
      assertTrue(q.offer(new Integer(i)));
    }
    assertFalse(q.isEmpty());
    assertEquals(0, q.remainingCapacity());
    assertEquals(n, q.size());
    return q;
  }

  /**
   * A new queue has the indicated capacity, or Integer.MAX_VALUE if
   * none given
   */
  @Test
  public void testConstructor1() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      assertEquals(SIZE, new LinkedBlockingQueue(SIZE).remainingCapacity());
      assertEquals(Integer.MAX_VALUE, new LinkedBlockingQueue().remainingCapacity());
    }
    printFinish();
  }

  /**
   * Constructor throws IAE if  capacity argument nonpositive
   */
  @Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(0);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      }
    }
    printFinish();
  }

  /**
   * Initializing from null Collection throws NPE
   */
  @Test
  public void testConstructor3() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(null);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * Initializing from Collection of null elements throws NPE
   */
  @Test
  public void testConstructor4() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        Integer[] ints = new Integer[SIZE];
        LinkedBlockingQueue q = new LinkedBlockingQueue(Arrays.asList(ints));
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * Initializing from Collection with some null elements throws NPE
   */
  @Test
  public void testConstructor5() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE - 1; ++i) {
          ints[i] = new Integer(i);
        }
        LinkedBlockingQueue q = new LinkedBlockingQueue(Arrays.asList(ints));
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * Queue contains all elements of collection used to initialize
   */
  @Test
  public void testConstructor6() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE; ++i) {
          ints[i] = new Integer(i);
        }
        LinkedBlockingQueue q = new LinkedBlockingQueue(Arrays.asList(ints));
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(ints[i], q.poll());
        }
      } finally {
      }
    }
    printFinish();
  }

  /**
   * Queue transitions from empty to full when elements added
   */
  @Test
  public void testEmptyFull() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = new LinkedBlockingQueue(2);
      assertTrue(q.isEmpty());
      assertEquals("should have room for 2", 2, q.remainingCapacity());
      q.add(one);
      assertFalse(q.isEmpty());
      q.add(two);
      assertFalse(q.isEmpty());
      assertEquals(0, q.remainingCapacity());
      assertFalse(q.offer(three));
    }
    printFinish();
  }

  /**
   * remainingCapacity decreases on add, increases on remove
   */
  @Test
  public void testRemainingCapacity() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertEquals(i, q.remainingCapacity());
        assertEquals(SIZE - i, q.size());
        q.remove();
      }
      for (int i = 0; i < SIZE; ++i) {
        assertEquals(SIZE - i, q.remainingCapacity());
        assertEquals(i, q.size());
        q.add(new Integer(i));
      }
    }
    printFinish();
  }

  /**
   * offer(null) throws NPE
   */
  @Test
  public void testOfferNull() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(1);
        q.offer(null);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * add(null) throws NPE
   */
  @Test
  public void testAddNull() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(1);
        q.add(null);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * Offer succeeds if not full; fails if full
   */
  @Test
  public void testOffer() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = new LinkedBlockingQueue(1);
      assertTrue(q.offer(zero));
      assertFalse(q.offer(one));
    }
    printFinish();
  }

  /**
   * add succeeds if not full; throws ISE if full
   */
  @Test
  public void testAdd() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
          assertTrue(q.add(new Integer(i)));
        }
        assertEquals(0, q.remainingCapacity());
        q.add(new Integer(SIZE));
      } catch (IllegalStateException success) {
      }
    }
    printFinish();
  }

  /**
   * addAll(null) throws NPE
   */
  @Test
  public void testAddAll1() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(1);
        q.addAll(null);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * addAll(this) throws IAE
   */
  @Test
  public void testAddAllSelf() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = populatedQueue(SIZE);
        q.addAll(q);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      }
    }
    printFinish();
  }

  /**
   * addAll of a collection with null elements throws NPE
   */
  @Test
  public void testAddAll2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(SIZE);
        Integer[] ints = new Integer[SIZE];
        q.addAll(Arrays.asList(ints));
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * addAll of a collection with any null elements throws NPE after
   * possibly adding some elements
   */
  @Test
  public void testAddAll3() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(SIZE);
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE - 1; ++i) {
          ints[i] = new Integer(i);
        }
        q.addAll(Arrays.asList(ints));
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * addAll throws ISE if not enough room
   */
  @Test
  public void testAddAll4() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(1);
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE; ++i) {
          ints[i] = new Integer(i);
        }
        q.addAll(Arrays.asList(ints));
        shouldThrow();
      } catch (IllegalStateException success) {
      }
    }
    printFinish();
  }

  /**
   * Queue contains all elements, in traversal order, of successful addAll
   */
  @Test
  public void testAddAll5() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        Integer[] empty = new Integer[0];
        Integer[] ints = new Integer[SIZE];
        for (int i = 0; i < SIZE; ++i) {
          ints[i] = new Integer(i);
        }
        LinkedBlockingQueue q = new LinkedBlockingQueue(SIZE);
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(ints)));
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(ints[i], q.poll());
        }
      } finally {
      }
    }
    printFinish();
  }

  /**
   * put(null) throws NPE
   */
  @Test
  public void testPutNull() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(SIZE);
        q.put(null);
        shouldThrow();
      } catch (NullPointerException success) {
      } catch (InterruptedException ie) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * all elements successfully put are contained
   */
  @Test
  public void testPut() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
          Integer I = new Integer(i);
          q.put(I);
          assertTrue(q.contains(I));
        }
        assertEquals(0, q.remainingCapacity());
      } catch (InterruptedException ie) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * put blocks interruptibly if full
   */
  @Test
  public void testBlockingPut() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          int added = 0;
          try {
            LinkedBlockingQueue q = new LinkedBlockingQueue(SIZE);
            for (int i = 0; i < SIZE; ++i) {
              q.put(new Integer(i));
              ++added;
            }
            q.put(new Integer(SIZE));
            threadShouldThrow();
          } catch (InterruptedException ie) {
            threadAssertEquals(added, SIZE);
          }
        }
      });
      t.start();

      try {
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        t.interrupt();
        t.join();
      } catch (InterruptedException ie) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * put blocks waiting for take when full
   */
  @Test
  public void testPutWithTake() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
      Thread t = new Thread(new Runnable() {

        public void run() {
          int added = 0;
          try {
            q.put(new Object());
            ++added;
            q.put(new Object());
            ++added;
            q.put(new Object());
            ++added;
            q.put(new Object());
            ++added;
            threadShouldThrow();
          } catch (InterruptedException e) {
            threadAssertTrue(added >= 2);
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        q.take();
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        t.interrupt();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * timed offer times out if full and elements not taken
   */
  @Test
  public void testTimedOffer() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
      final AtomicInteger i = new AtomicInteger(1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            q.put(new Object());
            q.put(new Object());
            threadAssertFalse(q.offer(new Object(), SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
            i.set(2);
            boolean b = q.offer(new Object(), LONG_DELAY_MS, TimeUnit.MILLISECONDS);
            if(b) threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });

      try {
        t.start();
        Thread.sleep(SMALL_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.TIMED_WAITING || i.get() != 2);
        t.interrupt();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * take retrieves elements in FIFO order
   */
  @Test
  public void testTake() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(i, ((Integer) q.take()).intValue());
        }
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * take blocks interruptibly when empty
   */
  @Test
  public void testTakeFromEmpty() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            q.take();
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        t.interrupt();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Take removes existing elements until empty, then blocks interruptibly
   */
  @Test
  public void testBlockingTake() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            LinkedBlockingQueue q = populatedQueue(SIZE);
            for (int i = 0; i < SIZE; ++i) {
              assertEquals(i, ((Integer) q.take()).intValue());
            }
            q.take();
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        t.interrupt();
        t.join();
      } catch (InterruptedException ie) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * poll succeeds unless empty
   */
  @Test
  public void testPoll() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertEquals(i, ((Integer) q.poll()).intValue());
      }
      assertNull(q.poll());
    }
    printFinish();
  }

  /**
   * timed pool with zero timeout succeeds when non-empty, else times out
   */
  @Test
  public void testTimedPoll0() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(i, ((Integer) q.poll(0, TimeUnit.MILLISECONDS)).intValue());
        }
        assertNull(q.poll(0, TimeUnit.MILLISECONDS));
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * timed pool with nonzero timeout succeeds when non-empty, else times out
   */
  @Test
  public void testTimedPoll() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(i, ((Integer) q.poll(SHORT_DELAY_MS, TimeUnit.MILLISECONDS)).intValue());
        }
        assertNull(q.poll(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Interrupted timed poll throws InterruptedException instead of
   * returning timeout status
   */
  @Test
  public void testInterruptedTimedPoll() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            LinkedBlockingQueue q = populatedQueue(SIZE);
            for (int i = 0; i < SIZE; ++i) {
              threadAssertEquals(i, ((Integer) q.poll(SHORT_DELAY_MS, TimeUnit.MILLISECONDS)).intValue());
            }
            threadAssertNull(q.poll(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
          } catch (InterruptedException success) {
          }
        }
      });
      t.start();
      try {
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.TIMED_WAITING);
        t.interrupt();
        t.join();
      } catch (InterruptedException ie) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   *  timed poll before a delayed offer fails; after offer succeeds;
   *  on interruption throws
   */
  @Test
  public void testTimedPollWithOffer() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
      final AtomicInteger i = new AtomicInteger(1);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            Object b = q.poll(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(b != null);
            threadAssertNull(b);
            i.set(2);
            b = q.poll(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            i.set(3);
            b = q.poll(LONG_DELAY_MS, TimeUnit.MILLISECONDS);
            if(b != null) threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SMALL_DELAY_MS);
        Verify.ignoreIf(i.get() != 2);
        assertTrue(q.offer(zero, SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        Verify.ignoreIf(t.getState() != Thread.State.TIMED_WAITING);
        t.interrupt();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * peek returns next element, or null if empty
   */
  @Test
  public void testPeek() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertEquals(i, ((Integer) q.peek()).intValue());
        q.poll();
        assertTrue(q.peek() == null ||
                i != ((Integer) q.peek()).intValue());
      }
      assertNull(q.peek());
    }
    printFinish();
  }

  /**
   * element returns next element, or throws NSEE if empty
   */
  @Test
  public void testElement() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertEquals(i, ((Integer) q.element()).intValue());
        q.poll();
      }
      try {
        q.element();
        shouldThrow();
      } catch (NoSuchElementException success) {
      }
    }
    printFinish();
  }

  /**
   * remove removes next element, or throws NSEE if empty
   */
  @Test
  public void testRemove() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertEquals(i, ((Integer) q.remove()).intValue());
      }
      try {
        q.remove();
        shouldThrow();
      } catch (NoSuchElementException success) {
      }
    }
    printFinish();
  }

  /**
   * remove(x) removes x and returns true if present
   */
  @Test
  public void testRemoveElement() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 1; i < SIZE; i += 2) {
        assertTrue(q.remove(new Integer(i)));
      }
      for (int i = 0; i < SIZE; i += 2) {
        assertTrue(q.remove(new Integer(i)));
        assertFalse(q.remove(new Integer(i + 1)));
      }
      assertTrue(q.isEmpty());
    }
    printFinish();
  }

  /**
   * An add following remove(x) succeeds
   */
  @Test
  public void testRemoveElementAndAdd() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = new LinkedBlockingQueue();
        assertTrue(q.add(new Integer(1)));
        assertTrue(q.add(new Integer(2)));
        assertTrue(q.remove(new Integer(1)));
        assertTrue(q.remove(new Integer(2)));
        assertTrue(q.add(new Integer(3)));
        assertTrue(q.take() != null);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * contains(x) reports true when elements added but not yet removed
   */
  @Test
  public void testContains() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertTrue(q.contains(new Integer(i)));
        q.poll();
        assertFalse(q.contains(new Integer(i)));
      }
    }
    printFinish();
  }

  /**
   * clear removes all elements
   */
  @Test
  public void testClear() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      q.clear();
      assertTrue(q.isEmpty());
      assertEquals(0, q.size());
      assertEquals(SIZE, q.remainingCapacity());
      q.add(one);
      assertFalse(q.isEmpty());
      assertTrue(q.contains(one));
      q.clear();
      assertTrue(q.isEmpty());
    }
    printFinish();
  }

  /**
   * containsAll(c) is true when c contains a subset of elements
   */
  @Test
  public void testContainsAll() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      LinkedBlockingQueue p = new LinkedBlockingQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertTrue(q.containsAll(p));
        assertFalse(p.containsAll(q));
        p.add(new Integer(i));
      }
      assertTrue(p.containsAll(q));
    }
    printFinish();
  }

  /**
   * retainAll(c) retains only those elements of c and reports true if changed
   */
  @Test
  public void testRetainAll() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      LinkedBlockingQueue p = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
        boolean changed = q.retainAll(p);
        if (i == 0) {
          assertFalse(changed);
        } else {
          assertTrue(changed);
        }

        assertTrue(q.containsAll(p));
        assertEquals(SIZE - i, q.size());
        p.remove();
      }
    }
    printFinish();
  }

  /**
   * removeAll(c) removes only those elements of c and reports true if changed
   */
  @Test
  public void testRemoveAll() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      for (int i = 1; i < SIZE; ++i) {
        LinkedBlockingQueue q = populatedQueue(SIZE);
        LinkedBlockingQueue p = populatedQueue(i);
        assertTrue(q.removeAll(p));
        assertEquals(SIZE - i, q.size());
        for (int j = 0; j < i; ++j) {
          Integer I = (Integer) (p.remove());
          assertFalse(q.contains(I));
        }
      }
    }
    printFinish();
  }

  /**
   * toArray contains all elements
   */
  @Test
  public void testToArray() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      Object[] o = q.toArray();
      try {
        for (int i = 0; i < o.length; i++) {
          assertEquals(o[i], q.take());
        }
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * toArray(a) contains all elements
   */
  @Test
  public void testToArray2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      Integer[] ints = new Integer[SIZE];
      ints = (Integer[]) q.toArray(ints);
      try {
        for (int i = 0; i < ints.length; i++) {
          assertEquals(ints[i], q.take());
        }
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * toArray(null) throws NPE
   */
  @Test
  public void testToArray_BadArg() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = populatedQueue(SIZE);
        Object o[] = q.toArray(null);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * toArray with incompatible array type throws CCE
   */
  @Test
  public void testToArray1_BadArg() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        LinkedBlockingQueue q = populatedQueue(SIZE);
        Object o[] = q.toArray(new String[10]);
        shouldThrow();
      } catch (ArrayStoreException success) {
      }
    }
    printFinish();
  }

  /**
   * iterator iterates through all elements
   */
  @Test
  public void testIterator() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      Iterator it = q.iterator();
      try {
        while (it.hasNext()) {
          assertEquals(it.next(), q.take());
        }
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * iterator.remove removes current element
   */
  @Test
  public void testIteratorRemove() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = new LinkedBlockingQueue(3);
      q.add(two);
      q.add(one);
      q.add(three);

      Iterator it = q.iterator();
      it.next();
      it.remove();

      it = q.iterator();
      assertEquals(it.next(), one);
      assertEquals(it.next(), three);
      assertFalse(it.hasNext());
    }
    printFinish();
  }

  /**
   * iterator ordering is FIFO
   */
  @Test
  public void testIteratorOrdering() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = new LinkedBlockingQueue(3);
      q.add(one);
      q.add(two);
      q.add(three);
      assertEquals(0, q.remainingCapacity());
      int k = 0;
      for (Iterator it = q.iterator(); it.hasNext();) {
        int i = ((Integer) (it.next())).intValue();
        assertEquals(++k, i);
      }
      assertEquals(3, k);
    }
    printFinish();
  }

  /**
   * Modifications do not cause iterators to fail
   */
  @Test
  public void testWeaklyConsistentIteration() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = new LinkedBlockingQueue(3);
      q.add(one);
      q.add(two);
      q.add(three);
      try {
        for (Iterator it = q.iterator(); it.hasNext();) {
          q.remove();
          it.next();
        }
      } catch (ConcurrentModificationException e) {
        unexpectedException();
      }
      assertEquals(0, q.size());
    }
    printFinish();
  }

  /**
   * toString contains toStrings of elements
   */
  @Test
  public void testToString() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      String s = q.toString();
      for (int i = 0; i < SIZE; ++i) {
        assertTrue(s.indexOf(String.valueOf(i)) >= 0);
      }
    }
    printFinish();
  }

  /**
   * offer transfers elements across Executor tasks
   */
  @Test
  public void testOfferInExecutor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
      q.add(one);
      q.add(two);
      ExecutorService executor = Executors.newFixedThreadPool(2);
      executor.execute(new Runnable() {

        public void run() {
          if(q.size() != 2) return;
          threadAssertFalse(q.offer(three));
          try {
            if(q.size() != 1) return;
            threadAssertTrue(q.offer(three, MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS));
            threadAssertEquals(0, q.remainingCapacity());
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        Thread.sleep(SMALL_DELAY_MS);
      } catch (InterruptedException ex) {
        unexpectedException();
      }
      executor.execute(new Runnable() {

        public void run() {
          try {
            Thread.sleep(SMALL_DELAY_MS);
            threadAssertEquals(one, q.take());
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      joinPool(executor);
    }
    printFinish();
  }

  /**
   * poll retrieves elements across Executor threads
   */
  @Test
  public void testPollInExecutor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = new LinkedBlockingQueue(2);
      ExecutorService executor = Executors.newFixedThreadPool(2);
      executor.execute(new Runnable() {

        public void run() {
          Object o = q.poll();
          Verify.ignoreIf(o != null);
          threadAssertNull(q.poll());
          try {
            o = q.poll(MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(o == null);
            threadAssertTrue(o != null);
            threadAssertTrue(q.isEmpty());
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      executor.execute(new Runnable() {

        public void run() {
          try {
            Thread.sleep(SMALL_DELAY_MS);
            q.put(one);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      joinPool(executor);
    }
    printFinish();
  }

  /**
   * A deserialized serialized queue has same elements in same order
   */
  //@Test
  public void testSerialization() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);

      try {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(q);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        LinkedBlockingQueue r = (LinkedBlockingQueue) in.readObject();
        assertEquals(q.size(), r.size());
        while (!q.isEmpty()) {
          assertEquals(q.remove(), r.remove());
        }
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * drainTo(null) throws NPE
   */
  @Test
  public void testDrainToNull() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      try {
        q.drainTo(null);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * drainTo(this) throws IAE
   */
  @Test
  public void testDrainToSelf() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      try {
        q.drainTo(q);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      }
    }
    printFinish();
  }

  /**
   * drainTo(c) empties queue into another collection c
   */
  @Test
  public void testDrainTo() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      ArrayList l = new ArrayList();
      q.drainTo(l);
      assertEquals(q.size(), 0);
      assertEquals(l.size(), SIZE);
      for (int i = 0; i < SIZE; ++i) {
        assertEquals(l.get(i), new Integer(i));
      }
      q.add(zero);
      q.add(one);
      assertFalse(q.isEmpty());
      assertTrue(q.contains(zero));
      assertTrue(q.contains(one));
      l.clear();
      q.drainTo(l);
      assertEquals(q.size(), 0);
      assertEquals(l.size(), 2);
      for (int i = 0; i < 2; ++i) {
        assertEquals(l.get(i), new Integer(i));
      }
    }
    printFinish();
  }

  /**
   * drainTo empties full queue, unblocking a waiting put.
   */
  @Test
  public void testDrainToWithActivePut() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final LinkedBlockingQueue q = populatedQueue(SIZE);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            q.put(new Integer(SIZE + 1));
          } catch (InterruptedException ie) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        ArrayList l = new ArrayList();
        q.drainTo(l);
        assertTrue(l.size() >= SIZE);
        for (int i = 0; i < SIZE; ++i) {
          assertEquals(l.get(i), new Integer(i));
        }
        t.join();
        assertTrue(q.size() + l.size() >= SIZE);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * drainTo(null, n) throws NPE
   */
  @Test
  public void testDrainToNullN() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      try {
        q.drainTo(null, 0);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * drainTo(this, n) throws IAE
   */
  @Test
  public void testDrainToSelfN() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = populatedQueue(SIZE);
      try {
        q.drainTo(q, 0);
        shouldThrow();
      } catch (IllegalArgumentException success) {
      }
    }
    printFinish();
  }

  /**
   * drainTo(c, n) empties first max {n, size} elements of queue into c
   */
  @Test
  public void testDrainToN() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      LinkedBlockingQueue q = new LinkedBlockingQueue();
      for (int i = 0; i < SIZE + 2; ++i) {
        for (int j = 0; j < SIZE; j++) {
          assertTrue(q.offer(new Integer(j)));
        }
        ArrayList l = new ArrayList();
        q.drainTo(l, i);
        int k = (i < SIZE) ? i : SIZE;
        assertEquals(l.size(), k);
        assertEquals(q.size(), SIZE - k);
        for (int j = 0; j < k; ++j) {
          assertEquals(l.get(j), new Integer(j));
        }
        while (q.poll() != null);
      }
    }
    printFinish();
  }
}

