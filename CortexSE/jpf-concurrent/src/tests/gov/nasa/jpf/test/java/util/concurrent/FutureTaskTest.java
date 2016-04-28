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
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.Exchanger
 * 
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class FutureTaskTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {};

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /**
   * Subclass to expose protected methods
   */
  static class PublicFutureTask extends FutureTask {

    public PublicFutureTask(Callable r) {
      super(r);
    }

    public boolean runAndReset() {
      return super.runAndReset();
    }

    public void set(Object x) {
      super.set(x);
    }

    public void setException(Throwable t) {
      super.setException(t);
    }
  }

  /**
   * Creating a future with a null callable throws NPE
   */
  @Test
  public void testConstructor() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        FutureTask task = new FutureTask(null);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * creating a future with null runnable fails
   */
  @Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        FutureTask task = new FutureTask(null, Boolean.TRUE);
        shouldThrow();
      } catch (NullPointerException success) {
      }
    }
    printFinish();
  }

  /**
   * isDone is true when a task completes
   */
  @Test
  public void testIsDone() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      FutureTask task = new FutureTask(new NoOpCallable());
      task.run();
      assertTrue(task.isDone());
      assertFalse(task.isCancelled());
    }
    printFinish();
  }

  /**
   * runAndReset of a non-cancelled task succeeds
   */
  @Test
  public void testRunAndReset() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
      assertTrue(task.runAndReset());
      assertFalse(task.isDone());
    }
    printFinish();
  }

  /**
   * runAndReset after cancellation fails
   */
  @Test
  public void testResetAfterCancel() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
      assertTrue(task.cancel(false));
      assertFalse(task.runAndReset());
      assertTrue(task.isDone());
      assertTrue(task.isCancelled());
    }
    printFinish();
  }

  /**
   * setting value causes get to return it
   */
  @Test
  public void testSet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
      task.set(one);
      try {
        assertEquals(task.get(), one);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * setException causes get to throw ExecutionException
   */
  @Test
  public void testSetException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Exception nse = new NoSuchElementException();
      PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
      task.setException(nse);
      try {
        Object x = task.get();
        shouldThrow();
      } catch (ExecutionException ee) {
        Throwable cause = ee.getCause();
        assertEquals(cause, nse);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   *  Cancelling before running succeeds
   */
  @Test
  public void testCancelBeforeRun() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      FutureTask task = new FutureTask(new NoOpCallable());
      assertTrue(task.cancel(false));
      task.run();
      assertTrue(task.isDone());
      assertTrue(task.isCancelled());
    }
    printFinish();
  }

  /**
   * Cancel(true) before run succeeds
   */
  @Test
  public void testCancelBeforeRun2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      FutureTask task = new FutureTask(new NoOpCallable());
      assertTrue(task.cancel(true));
      task.run();
      assertTrue(task.isDone());
      assertTrue(task.isCancelled());
    }
    printFinish();
  }

  /**
   * cancel of a completed task fails
   */
  @Test
  public void testCancelAfterRun() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      FutureTask task = new FutureTask(new NoOpCallable());
      task.run();
      assertFalse(task.cancel(false));
      assertTrue(task.isDone());
      assertFalse(task.isCancelled());
    }
    printFinish();
  }

  /**
   * cancel(true) interrupts a running task
   */
  @Test
  public void testCancelInterrupt() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      FutureTask task = new FutureTask(new Callable() {

        public Object call() {
          try {
            Thread.sleep(MEDIUM_DELAY_MS);
            Verify.ignoreIf(true);
            threadShouldThrow();
          } catch (InterruptedException success) {
          }
          return Boolean.TRUE;
        }
      });
      Thread t = new Thread(task);
      t.start();

      try {
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(task.cancel(true));
        t.join();
        assertTrue(task.isDone());
        assertTrue(task.isCancelled());
      } catch (InterruptedException e) {
        unexpectedException();
      }

    }
    printFinish();
  }

  /**
   * cancel(false) does not interrupt a running task
   */
  @Test
  public void testCancelNoInterrupt() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      FutureTask task = new FutureTask(new Callable() {

        public Object call() {
          try {
            Thread.sleep(MEDIUM_DELAY_MS);
          } catch (InterruptedException success) {
            threadFail("should not interrupt");
          }
          return Boolean.TRUE;
        }
      });
      Thread t = new Thread(task);
      t.start();

      try {
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(task.isDone());
        assertTrue(task.cancel(false));
        t.join();
        assertTrue(task.isDone());
        assertTrue(task.isCancelled());
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * set in one thread causes get in another thread to retrieve value
   */
  @Test
  public void testGet1() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final FutureTask ft = new FutureTask(new Callable() {

        public Object call() {
          try {
            Thread.sleep(MEDIUM_DELAY_MS);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
          return Boolean.TRUE;
        }
      });
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            ft.get();
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        assertFalse(ft.isDone());
        assertFalse(ft.isCancelled());
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        ft.run();
        t.join();
        assertTrue(ft.isDone());
        assertFalse(ft.isCancelled());
      } catch (InterruptedException e) {
        unexpectedException();

      }
    }
    printFinish();
  }

  /**
   * set in one thread causes timed get in another thread to retrieve value
   */
  @Test
  public void testTimedGet1() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final FutureTask ft = new FutureTask(new Callable() {

        public Object call() {
          try {
            Thread.sleep(MEDIUM_DELAY_MS);
          } catch (InterruptedException e) {
            threadUnexpectedException();
          }
          return Boolean.TRUE;
        }
      });
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            ft.get(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
          } catch (TimeoutException success) {
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        assertFalse(ft.isDone());
        assertFalse(ft.isCancelled());
        t.start();
        ft.run();
        t.join();
        assertTrue(ft.isDone());
        assertFalse(ft.isCancelled());
      } catch (InterruptedException e) {
        unexpectedException();

      }
    }
    printFinish();
  }

  /**
   *  Cancelling a task causes timed get in another thread to throw CancellationException
   */
  @Test
  public void testTimedGet_Cancellation() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final FutureTask ft = new FutureTask(new Callable() {

        public Object call() {
          try {
            Thread.sleep(SMALL_DELAY_MS);
            Verify.ignoreIf(true);
            threadShouldThrow();
          } catch (InterruptedException e) {
          }
          return Boolean.TRUE;
        }
      });
      try {
        Thread t1 = new Thread(new Runnable() {

          public void run() {
            try {
              ft.get(MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS);
              Verify.ignoreIf(true);
              threadShouldThrow();
            } catch (CancellationException success) {
            } catch (TimeoutException e1) {
            } catch (Exception e2) {
              threadUnexpectedException();
            }
          }
        });
        Thread t2 = new Thread(ft);
        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        ft.cancel(true);
        t1.join();
        t2.join();
      } catch (InterruptedException ie) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Cancelling a task causes get in another thread to throw CancellationException
   */
  @Test
  public void testGet_Cancellation() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final FutureTask ft = new FutureTask(new Callable() {

        public Object call() {
          try {
            Thread.sleep(MEDIUM_DELAY_MS);
            Verify.ignoreIf(true);
            threadShouldThrow();
          } catch (InterruptedException e) {
          }
          return Boolean.TRUE;
        }
      });
      try {
        Thread t1 = new Thread(new Runnable() {

          public void run() {
            try {
              ft.get();
              threadShouldThrow();
            } catch (CancellationException success) {
            } catch (Exception e) {
              threadUnexpectedException();
            }
          }
        });
        Thread t2 = new Thread(ft);
        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        ft.cancel(true);
        t1.join();
        t2.join();
      } catch (InterruptedException success) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A runtime exception in task causes get to throw ExecutionException
   */
  @Test
  public void testGet_ExecutionException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final FutureTask ft = new FutureTask(new Callable() {

        @SuppressWarnings("divzero")
        public Object call() {
          int i = 5 / 0;
          return Boolean.TRUE;
        }
      });
      try {
        ft.run();
        ft.get();
        shouldThrow();
      } catch (ExecutionException success) {
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   *  A runtime exception in task causes timed get to throw ExecutionException
   */
  @Test
  public void testTimedGet_ExecutionException2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final FutureTask ft = new FutureTask(new Callable() {

        @SuppressWarnings("divzero")
        public Object call() {
          int i = 5 / 0;
          return Boolean.TRUE;
        }
      });
      try {
        ft.run();
        ft.get(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
        shouldThrow();
      } catch (ExecutionException success) {
      } catch (TimeoutException success) {
      } // unlikely but OK
      catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Interrupting a waiting get causes it to throw InterruptedException
   */
  @Test
  public void testGet_InterruptedException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final FutureTask ft = new FutureTask(new NoOpCallable());
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            ft.get();
            threadShouldThrow();
          } catch (InterruptedException success) {
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   *  Interrupting a waiting timed get causes it to throw InterruptedException
   */
  @Test
  public void testTimedGet_InterruptedException2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final FutureTask ft = new FutureTask(new NoOpCallable());
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            ft.get(LONG_DELAY_MS, TimeUnit.MILLISECONDS);
            threadShouldThrow();
          } catch (InterruptedException success) {
          } catch (TimeoutException e1) {
          } catch (Exception e2) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A timed out timed get throws TimeoutException
   */
  @Test
  public void testGet_TimeoutException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        FutureTask ft = new FutureTask(new NoOpCallable());
        ft.get(1, TimeUnit.MILLISECONDS);
        shouldThrow();
      } catch (TimeoutException success) {
      } catch (Exception success) {
        unexpectedException();
      }
    }
    printFinish();
  }
}

