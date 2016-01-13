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
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.locks.ReentrantLock
 * 
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class AdditionalReentrantLockTest extends TestCaseHelpers {

  private final static boolean fairness = false;

  private final static String[] JPF_ARGS = {
  };

  public static void main (String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * lock should not change interrupt state
   */
  @Test
  public void testLockInterrupt () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      try {
        lock.lock();
        Thread t = new Thread() {
          public void run() {
            Thread.currentThread().interrupt();
            lock.lock();
            lock.unlock();
            assertTrue(Thread.currentThread().isInterrupted());
          }
        };
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        lock.unlock();
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * lock should not change interrupt state
   */
  @Test
  public void testLockInterrupt2 () {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final ReentrantLock lock = new ReentrantLock(fairness);
      final AtomicInteger i = new AtomicInteger();
      try {
        lock.lock();
        Thread t = new Thread() {
          public void run() {
            lock.lock();
            lock.unlock();
            Verify.ignoreIf(i.get() != 1);
            assertTrue(Thread.currentThread().isInterrupted());
          }
        };
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        i.set(1);
        lock.unlock();
        t.join();
        assertFalse(t.isAlive());
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

}