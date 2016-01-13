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
import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.Exchanger
 * 
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 * @author Nathan Reynolds <nathanila@gmail.com>
 */
public class ExchangerTest extends TestCaseHelpers {

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * exchange exchanges objects across two threads
   */
  @Test
  public void testExchange() {
    if (verifyNoPropertyViolation()) {
      final Exchanger e = new Exchanger();
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            Object v = e.exchange(one);
            threadAssertEquals(v, two);
            Object w = e.exchange(v);
            threadAssertEquals(w, one);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            Object v = e.exchange(two);
            threadAssertEquals(v, one);
            Object w = e.exchange(v);
            threadAssertEquals(w, two);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t1.start();
        t2.start();
        t1.join();
        t2.join();
      } catch (InterruptedException ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * timed exchange exchanges objects across two threads
   */
  @Test
  public void testTimedExchange() {
    if (verifyNoPropertyViolation()) {
      final Exchanger e = new Exchanger();
      final TimeUnit u = TimeUnit.MILLISECONDS;
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            Object v = e.exchange(one, SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            threadAssertEquals(v, two);
            Object w = e.exchange(v, SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            threadAssertEquals(w, one);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          } catch (TimeoutException toe) {
            //threadUnexpectedException();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            Object v = e.exchange(two, SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            threadAssertEquals(v, one);
            Object w = e.exchange(v, SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            threadAssertEquals(w, two);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          } catch (TimeoutException toe) {
            //threadUnexpectedException();
          }
        }
      });
      try {
        t1.start();
        t2.start();
        t1.join();
        t2.join();
      } catch (InterruptedException ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * interrupt during wait for exchange throws IE
   */
  @Test
  public void testExchange_InterruptedException() {
    if (verifyNoPropertyViolation()) {
      final Exchanger e = new Exchanger();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            e.exchange(one);
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
      } catch (InterruptedException ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * interrupt during wait for timed exchange throws IE
   */
  @Test
  public void testTimedExchange_InterruptedException() {
    if (verifyNoPropertyViolation()) {
      final Exchanger e = new Exchanger();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            e.exchange(null, MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS);
            threadShouldThrow();
          } catch (InterruptedException success) {
          } catch (TimeoutException success2) {
          } catch (Exception e2) {
            threadFail("should throw IE");
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
      } catch (InterruptedException ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * timeout during wait for timed exchange throws TOE
   */
  @Test
  public void testExchange_TimeOutException() {
    if (verifyNoPropertyViolation()) {
      final Exchanger e = new Exchanger();
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            e.exchange(null, SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            threadShouldThrow();
          } catch (TimeoutException success) {
          } catch (InterruptedException e2) {
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.join();
      } catch (InterruptedException ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * If one exchanging thread is interrupted, another succeeds.
   */
  @Test
  public void testReplacementAfterExchange() {
    if (verifyNoPropertyViolation()) {
      final Exchanger e = new Exchanger();
      final AtomicInteger i = new AtomicInteger();
      final Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            Object v = e.exchange(one);
            threadAssertEquals(v, two);
            i.set(1);
            Object w = e.exchange(v);
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            Object v = e.exchange(two);
            threadAssertEquals(v, one);
            Thread.sleep(SMALL_DELAY_MS);
            Verify.ignoreIf(t1.getState() != Thread.State.TERMINATED);
            Object w = e.exchange(v);
            threadAssertEquals(w, three);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });
      Thread t3 = new Thread(new Runnable() {

        public void run() {
          try {
            Thread.sleep(SMALL_DELAY_MS);
            Verify.ignoreIf(t1.getState() != Thread.State.TERMINATED);
            Object w = e.exchange(three);
            threadAssertEquals(w, one);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        t1.start();
        t2.start();
        t3.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t1.getState() != Thread.State.WAITING || i.get() != 1);
        t1.interrupt();
        t1.join();
        t2.join();
        t3.join();
      } catch (InterruptedException ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  @Test
  public void testKeepStrongReference() {
    if (verifyNoPropertyViolation()) {
      final Exchanger e = new Exchanger();

      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            Object temp = e.exchange(new LinkedList());   // Don't explicitly keep a local reference to the offered object
            assert temp != null;
            assert temp instanceof ArrayList;
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            Object temp = e.exchange(new ArrayList());    // Don't explicitly keep a local reference to the offered object
            assert temp != null;
            assert temp instanceof LinkedList;
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
        }
      });

      Thread t3 = new Thread(new Runnable() {

        public void run() {
          System.gc();             // Set the GCNeeded flag
          Verify.getBoolean();     // Cause the state to be examined and hence GC to happen
        }
      });

      try {
        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();
      } catch (InterruptedException ex) {
        unexpectedException();
      }
    }

    printFinish();
  }

  @Test
  public void testNoLeak() throws InterruptedException {
    if (verifyNoPropertyViolation()) {
      Exchanger exchanger = new Exchanger();
      NoLeak leak1 = new NoLeak(exchanger);
      NoLeak leak2 = new NoLeak(exchanger);
      Thread thread1 = new Thread(leak1);
      Thread thread2 = new Thread(leak2);

      thread1.start();
      thread2.start();

      thread1.join();
      thread2.join();

      System.gc();                         // Set the GCNeeded flag
      Verify.getBoolean();                 // Cause the state to be examined and hence GC to happen

      assert leak1.m_ref.get() == null;
      assert leak2.m_ref.get() == null;
    }

    printFinish();
  }

  private class NoLeak implements Runnable {

    private final Exchanger m_exchanger;
    WeakReference m_ref;

    public NoLeak(Exchanger exchanger) {
      m_exchanger = exchanger;
    }

    public void run() {
      Object data;

      data = new Object();
      m_ref = new WeakReference(data);

      try {
        m_exchanger.exchange(data);
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
  }

  @Test
  public void testNoLeakInterrupted() throws InterruptedException {
    if (verifyNoPropertyViolation()) {
      Exchanger exchanger = new Exchanger();
      NoLeakInterrupted leak = new NoLeakInterrupted(exchanger);
      Thread thread = new Thread(leak);

      thread.start();
      Verify.ignoreIf(thread.getState() != Thread.State.WAITING);       // Make sure thread is blocked in Exchanger.exchange()
      thread.interrupt();
      thread.join();

      System.gc();                                                      // Set the GCNeeded flag
      Verify.getBoolean();                                              // Cause the state to be examined and hence GC to happen

      assert leak.m_ref.get() == null;
    }

    printFinish();
  }

  private class NoLeakInterrupted implements Runnable {

    private final Exchanger m_exchanger;
    WeakReference m_ref;

    public NoLeakInterrupted(Exchanger exchanger) {
      m_exchanger = exchanger;
    }

    public void run() {
      Object data;

      data = new Object();
      m_ref = new WeakReference(data);

      try {
        m_exchanger.exchange(data);
        shouldThrow();
      } catch (InterruptedException e) {
        e = null;
      }
    }
  }

  @Test
  public void tesNoLeakTimeOutException() throws InterruptedException {
    if (verifyNoPropertyViolation()) {
      Exchanger exchanger = new Exchanger();
      NoLeakTimeoutException leak = new NoLeakTimeoutException(exchanger);
      Thread thread = new Thread(leak);

      thread.start();
      thread.join();

      System.gc();                                                      // Set the GCNeeded flag
      Verify.getBoolean();                                              // Cause the state to be examined and hence GC to happen

      assert leak.m_ref.get() == null;
    }
    printFinish();
  }

  private class NoLeakTimeoutException implements Runnable {

    private final Exchanger m_exchanger;
    WeakReference m_ref;

    public NoLeakTimeoutException(Exchanger exchanger) {
      m_exchanger = exchanger;
    }

    public void run() {
      Object data;

      data = new Object();
      m_ref = new WeakReference(data);

      try {
        m_exchanger.exchange(data, SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
        threadShouldThrow();
      } catch (TimeoutException success) {
      } catch (InterruptedException e2) {
      }
    }
  }
}
