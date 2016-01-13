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
package gov.nasa.jpf.test.java.util.concurrent.additional;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.test.java.util.concurrent.TestCaseHelpers;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.lock.AbstractQueuedSynchronizer
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AdditionalAbstractQueuedSynchronizerTest extends TestCaseHelpers {

  /**
   * constructor initializes to given value
   */
  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * A simple mutex class, adapted from the
   * AbstractQueuedSynchronizer javadoc.  Exclusive acquire tests
   * exercise this as a sample user extension.  Other
   * methods/features of AbstractQueuedSynchronizerTest are tested
   * via other test classes, including those for ReentrantLock,
   * ReentrantReadWriteLock, and Semaphore
   */
  static class Mutex extends AbstractQueuedSynchronizer {

    public boolean isHeldExclusively() {
      return getState() == 1;
    }

    public boolean tryAcquire(int acquires) {
      assertTrue(acquires == 1);
      return compareAndSetState(0, 1);
    }

    public boolean tryRelease(int releases) {
      if (getState() == 0) {
        throw new IllegalMonitorStateException();
      }
      setState(0);
      return true;
    }

    public AbstractQueuedSynchronizer.ConditionObject newCondition() {
      return new AbstractQueuedSynchronizer.ConditionObject();
    }
  }

  /**
   * acquire should not be interruptible
   */
  @Test
  public void testAcquireInterrupt() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AtomicInteger i = new AtomicInteger();
      try {
        sync.acquire(1);
        Thread t = new Thread() {
          public void run() {
            sync.acquire(1);
            assertTrue(Thread.currentThread().isInterrupted());
          }
        };
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        t.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        sync.release(1);
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * interruption before acquire
   */
  @Test
  public void testAcquireInterrupt2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Mutex sync = new Mutex();
      final AtomicInteger i = new AtomicInteger();
      try {
        sync.acquire(1);
        Thread t = new Thread() {
          public void run() {
            Thread.currentThread().interrupt();
            sync.acquire(1);
            assertTrue(Thread.currentThread().isInterrupted());
          }
        };
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        sync.release(1);
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

}
