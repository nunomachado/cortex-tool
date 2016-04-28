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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

/**
 * JPF test driver for java.util.concurrent.CountDownLatch
 * 
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class CyclicBarrierTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {""};

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }
  private volatile int countAction;

  private class MyAction implements Runnable {

    public void run() {
      ++countAction;
    }
  }

  /**
   * Creating with negative parties throws IAE
   */
  //@Test
  public void testConstructor1() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        new CyclicBarrier(-1, (Runnable) null);
        shouldThrow();
      } catch (IllegalArgumentException e) {
      }
    }
    printFinish();
  }

  /**
   * Creating with negative parties and no action throws IAE
   */
  //@Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        new CyclicBarrier(-1);
        shouldThrow();
      } catch (IllegalArgumentException e) {
      }
    }
    printFinish();
  }

  /**
   * getParties returns the number of parties given in constructor
   */
  //@Test
  public void testGetParties() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      CyclicBarrier b = new CyclicBarrier(2);
      assertEquals(2, b.getParties());
      assertEquals(0, b.getNumberWaiting());
    }
    printFinish();
  }

  /**
   * A 1-party barrier triggers after single await
   */
  //@Test
  public void testSingleParty() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        CyclicBarrier b = new CyclicBarrier(1);
        assertEquals(1, b.getParties());
        assertEquals(0, b.getNumberWaiting());
        b.await();
        b.await();
        assertEquals(0, b.getNumberWaiting());
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * The supplied barrier action is run at barrier
   */
  //@Test
  public void testBarrierAction() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        countAction = 0;
        CyclicBarrier b = new CyclicBarrier(1, new MyAction());
        assertEquals(1, b.getParties());
        assertEquals(0, b.getNumberWaiting());
        b.await();
        b.await();
        assertEquals(0, b.getNumberWaiting());
        assertEquals(countAction, 2);
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A 2-party/thread barrier triggers after both threads invoke await
   */
  @Test
  public void testTwoParties() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CyclicBarrier b = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            b.await();
            b.await();
            b.await();
            b.await();
          } catch (Exception e) {
            threadUnexpectedException();
          }
        }
      });

      try {
        t.start();
        b.await();
        b.await();
        b.await();
        b.await();
        t.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * An interruption in one party causes others waiting in await to
   * throw BrokenBarrierException
   */
  //@Test
  public void testAwait1_Interrupted_BrokenBarrier() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CyclicBarrier c = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await();
            threadShouldThrow();
          } catch (InterruptedException success) {
          } catch (Exception b) {
            threadUnexpectedException();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await();
            threadShouldThrow();
          } catch (BrokenBarrierException success) {
          } catch (Exception i) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t1.getState() != Thread.State.WAITING || t2.getState() != Thread.State.WAITING);
        t1.interrupt();
        t1.join();
        t2.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * An interruption in one party causes others waiting in timed await to
   * throw BrokenBarrierException
   */
  //@Test
  public void testAwait2_Interrupted_BrokenBarrier() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CyclicBarrier c = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await(LONG_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(true);
            threadShouldThrow();
          } catch (InterruptedException success) {
          } catch (Exception b) {
            threadUnexpectedException();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await(LONG_DELAY_MS, TimeUnit.MILLISECONDS);
            Verify.ignoreIf(true);
            threadShouldThrow();
          } catch (BrokenBarrierException success) {
          } catch (Exception i) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        Verify.ignoreIf(t1.getState() != Thread.State.TIMED_WAITING || t2.getState() != Thread.State.TIMED_WAITING);
        t1.interrupt();
        t1.join();
        t2.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A timeout in timed await throws TimeoutException
   */
  //@Test
  public void testAwait3_TimeOutException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CyclicBarrier c = new CyclicBarrier(2);
      Thread t = new Thread(new Runnable() {

        public void run() {
          try {
            c.await(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            threadShouldThrow();
          } catch (TimeoutException success) {
            
          } catch (Exception b) {
            threadUnexpectedException();

          }
        }
      });
      try {
        t.start();
        t.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A timeout in one party causes others waiting in timed await to
   * throw BrokenBarrierException
   */
  //@Test
  public void testAwait4_Timeout_BrokenBarrier() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CyclicBarrier c = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            threadShouldThrow();
          } catch (TimeoutException success) {
          } catch (Exception b) {
            threadUnexpectedException();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await(MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS);
            threadShouldThrow();
          } catch (BrokenBarrierException success) {
          } catch (Exception i) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t1.start();
        t2.start();
        t1.join();
        t2.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A timeout in one party causes others waiting in await to
   * throw BrokenBarrierException
   */
  //@Test
  public void testAwait5_Timeout_BrokenBarrier() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CyclicBarrier c = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await(SHORT_DELAY_MS, TimeUnit.MILLISECONDS);
            threadShouldThrow();
          } catch (TimeoutException success) {
          } catch (Exception b) {
            threadUnexpectedException();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await();
            threadShouldThrow();
          } catch (BrokenBarrierException success) {
          } catch (Exception i) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t1.start();
        t2.start();
        t1.join();
        t2.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A reset of an active barrier causes waiting threads to throw
   * BrokenBarrierException
   */
  //@Test
  public void testReset_BrokenBarrier() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CyclicBarrier c = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await();
            threadShouldThrow();
          } catch (BrokenBarrierException success) {
          } catch (Exception b) {
            threadUnexpectedException();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await();
            threadShouldThrow();
          } catch (BrokenBarrierException success) {
          } catch (Exception i) {
            threadUnexpectedException();
          }
        }
      });
      try {
        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        c.reset();
        t1.join();
        t2.join();
      } catch (InterruptedException e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * A reset before threads enter barrier does not throw
   * BrokenBarrierException
   */
  //@Test
  public void testReset_NoBrokenBarrier() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      final CyclicBarrier c = new CyclicBarrier(3);
      Thread t1 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await();
          } catch (Exception b) {
            threadUnexpectedException();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            c.await();
          } catch (Exception i) {
            threadUnexpectedException();
          }
        }
      });
      try {
        c.reset();
        t1.start();
        t2.start();
        c.await();
        t1.join();
        t2.join();
      } catch (Exception e) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * All threads block while a barrier is broken.
   */
  //@Test
  public void testReset_Leakage() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        final CyclicBarrier c = new CyclicBarrier(2);
        final AtomicBoolean done = new AtomicBoolean();
        Thread t = new Thread() {

          public void run() {
            while (!done.get()) {
              try {
                while (c.isBroken()) {
                  c.reset();
                }

                c.await();
                threadFail("await should not return");
              } catch (BrokenBarrierException e) {
              } catch (InterruptedException ie) {
              }
            }
          }
        };

        t.start();
        for (int i = 0; i < 4; i++) {
          Thread.sleep(SHORT_DELAY_MS);
          t.interrupt();
        }
        done.set(true);
        t.interrupt();
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Reset of a non-broken barrier does not break barrier
   */
  //@Test
  public void testResetWithoutBreakage() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        final CyclicBarrier start = new CyclicBarrier(3);
        final CyclicBarrier barrier = new CyclicBarrier(3);
        for (int i = 0; i < 3; i++) {
          Thread t1 = new Thread(new Runnable() {

            public void run() {
              try {
                start.await();
              } catch (Exception ie) {
                threadFail("start barrier");
              }
              try {
                barrier.await();
              } catch (Throwable thrown) {
                unexpectedException();
              }
            }
          });

          Thread t2 = new Thread(new Runnable() {

            public void run() {
              try {
                start.await();
              } catch (Exception ie) {
                threadFail("start barrier");
              }
              try {
                barrier.await();
              } catch (Throwable thrown) {
                unexpectedException();
              }
            }
          });


          t1.start();
          t2.start();
          try {
            start.await();
          } catch (Exception ie) {
            threadFail("start barrier");
          }
          barrier.await();
          t1.join();
          t2.join();
          assertFalse(barrier.isBroken());
          assertEquals(0, barrier.getNumberWaiting());
          if (i == 1) {
            barrier.reset();
          }
          assertFalse(barrier.isBroken());
          assertEquals(0, barrier.getNumberWaiting());
        }
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Reset of a barrier after interruption reinitializes it.
   */
  //@Test
  public void testResetAfterInterrupt() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        final CyclicBarrier start = new CyclicBarrier(3);
        final CyclicBarrier barrier = new CyclicBarrier(3);
        for (int i = 0; i < 2; i++) {
          Thread t1 = new Thread(new Runnable() {

            public void run() {
              try {
                start.await();
              } catch (Exception ie) {
                threadFail("start barrier");
              }
              try {
                barrier.await();
              } catch (InterruptedException ok) {
              } catch (Throwable thrown) {
                unexpectedException();
              }
            }
          });

          Thread t2 = new Thread(new Runnable() {

            public void run() {
              try {
                start.await();
              } catch (Exception ie) {
                threadFail("start barrier");
              }
              try {
                barrier.await();
              } catch (BrokenBarrierException ok) {
              } catch (Throwable thrown) {
                unexpectedException();
              }
            }
          });

          t1.start();
          t2.start();
          try {
            start.await();
          } catch (Exception ie) {
            threadFail("start barrier");
          }
          t1.interrupt();
          t1.join();
          t2.join();
          assertTrue(barrier.isBroken());
          assertEquals(0, barrier.getNumberWaiting());
          barrier.reset();
          assertFalse(barrier.isBroken());
          assertEquals(0, barrier.getNumberWaiting());
        }
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Reset of a barrier after timeout reinitializes it.
   */
  //@Test
  public void testResetAfterTimeout() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        final CyclicBarrier start = new CyclicBarrier(3);
        final CyclicBarrier barrier = new CyclicBarrier(3);
        for (int i = 0; i < 2; i++) {
          Thread t1 = new Thread(new Runnable() {

            public void run() {
              try {
                start.await();
              } catch (Exception ie) {
                threadFail("start barrier");
              }
              try {
                barrier.await(MEDIUM_DELAY_MS, TimeUnit.MILLISECONDS);
              } catch (TimeoutException ok) {
              } catch (Throwable thrown) {
                unexpectedException();
              }
            }
          });

          Thread t2 = new Thread(new Runnable() {

            public void run() {
              try {
                start.await();
              } catch (Exception ie) {
                threadFail("start barrier");
              }
              try {
                barrier.await();
              } catch (BrokenBarrierException ok) {
              } catch (Throwable thrown) {
                unexpectedException();
              }
            }
          });

          t1.start();
          t2.start();
          try {
            start.await();
          } catch (Exception ie) {
            threadFail("start barrier");
          }
          t1.join();
          t2.join();
          assertTrue(barrier.isBroken());
          assertEquals(0, barrier.getNumberWaiting());
          barrier.reset();
          assertFalse(barrier.isBroken());
          assertEquals(0, barrier.getNumberWaiting());
        }
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * Reset of a barrier after a failed command reinitializes it.
   */
  //@Test
  public void testResetAfterCommandException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        final CyclicBarrier start = new CyclicBarrier(3);
        final CyclicBarrier barrier =
                new CyclicBarrier(3, new Runnable() {

          public void run() {
            throw new NullPointerException();
          }
        });
        for (int i = 0; i < 2; i++) {
          Thread t1 = new Thread(new Runnable() {

            public void run() {
              try {
                start.await();
              } catch (Exception ie) {
                threadFail("start barrier");
              }
              try {
                barrier.await();
              } catch (BrokenBarrierException ok) {
              } catch (Throwable thrown) {
                unexpectedException();
              }
            }
          });

          Thread t2 = new Thread(new Runnable() {

            public void run() {
              try {
                start.await();
              } catch (Exception ie) {
                threadFail("start barrier");
              }
              try {
                barrier.await();
              } catch (BrokenBarrierException ok) {
              } catch (Throwable thrown) {
                unexpectedException();
              }
            }
          });

          t1.start();
          t2.start();
          try {
            start.await();
          } catch (Exception ie) {
            threadFail("start barrier");
          }
          while (barrier.getNumberWaiting() < 2) {
            Thread.yield();
          }
          try {
            barrier.await();
          } catch (Exception ok) {
          }
          t1.join();
          t2.join();
          assertTrue(barrier.isBroken());
          assertEquals(0, barrier.getNumberWaiting());
          barrier.reset();
          assertFalse(barrier.isBroken());
          assertEquals(0, barrier.getNumberWaiting());
        }
      } catch (Exception ex) {
        unexpectedException();
      }
    }
    printFinish();
  }
}
