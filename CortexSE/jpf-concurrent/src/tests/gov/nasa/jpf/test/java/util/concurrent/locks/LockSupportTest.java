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
package gov.nasa.jpf.test.java.util.concurrent.locks;

import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.test.java.util.concurrent.TestCaseHelpers;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.locks.LockSupport
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class LockSupportTest extends TestCaseHelpers {

  /**
   * constructor initializes to given value
   */
  private final static String[] JPF_ARGS = {
  };

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * park is released by unpark occurring after park
   */
  @Test
  public void testPark() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            LockSupport.park();
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        LockSupport.unpark(t);
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * park is released by unpark occurring before park
   */
  @Test
  public void testPark2() {
    final AtomicInteger i = new AtomicInteger(0);
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            Thread.sleep(SHORT_DELAY_MS);
            Verify.ignoreIf(i.get() != 1);
            LockSupport.park();
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        LockSupport.unpark(t);
        i.set(1);
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * park is released by interrupt
   */
  @Test
  public void testPark3() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            LockSupport.park();
          } catch (Exception e) {
            threadUnexpectedException();
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
   * park returns if interrupted before park
   */
  @Test
  public void testPark4() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            LockSupport.park();
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.RUNNABLE);
        t.interrupt();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * parkNanos times out if not unparked
   */
  @Test
  public void testParkNanos() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            LockSupport.parkNanos(1000);
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * parkUntil times out if not unparked
   */
  @Test
  public void testParkUntil() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            long d = new Date().getTime() + 100;
            LockSupport.parkUntil(d);
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * park with blocker
   */
  @Test
  public void testParkWithBlocker() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Object blocker = new Object();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            LockSupport.park(blocker);
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        assertEquals(blocker, LockSupport.getBlocker(t));
        LockSupport.unpark(t);
        assertEquals(null, LockSupport.getBlocker(t));
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * park with blocker cleans blocker reference after interruption
   */
  @Test
  public void testParkWithBlocker2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Object blocker = new Object();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            LockSupport.park(blocker);
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t.getState() != Thread.State.WAITING);
        assertEquals(blocker, LockSupport.getBlocker(t));
        t.interrupt();
        t.join();
        assertEquals(null, LockSupport.getBlocker(t));
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * parkNanos with blocker times out if not unparked
   */
  @Test
  public void testParkNanosWithBlocker() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Object blocker = new Object();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            LockSupport.parkNanos(blocker, 1000);
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(LockSupport.getBlocker(t) == null);
        assertEquals(blocker, LockSupport.getBlocker(t));
        t.join();
        assertEquals(null, LockSupport.getBlocker(t));
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * parkUntil with blocker times out if not unparked
   */
  @Test
  public void testParkUntilWithBlocker() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final Object blocker = new Object();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            long d = new Date().getTime() + 100;
            LockSupport.parkUntil(blocker,d);
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(LockSupport.getBlocker(t) == null);
        assertEquals(blocker, LockSupport.getBlocker(t));
        t.join();
        assertEquals(null, LockSupport.getBlocker(t));
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }
}
