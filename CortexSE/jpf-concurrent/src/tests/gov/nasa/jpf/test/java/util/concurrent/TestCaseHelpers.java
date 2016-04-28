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
import gov.nasa.jpf.util.Reflection;
import gov.nasa.jpf.util.test.TestJPF;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class TestCaseHelpers extends TestJPF {

  protected volatile boolean threadFailed;

  public void unexpectedException() {
    fail("Unexpected exception");
  }

  public void threadShouldThrow() {
    threadFailed = true;
    fail("should throw exception");
  }

  public void shouldThrow() {
    fail("Should throw exception");
  }

  public void threadUnexpectedException() {
    threadFailed = true;
    fail("Unexpected exception");
  }
  public static final long SHORT_DELAY_MS = 100;
  public static final long SMALL_DELAY_MS = SHORT_DELAY_MS * 5;
  public static final long MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
  public static final long LONG_DELAY_MS = SHORT_DELAY_MS * 50;
  // Some convenient Integer constants
  protected static final Integer zero = Integer.valueOf(0);
  protected static final Integer one = Integer.valueOf(1);
  protected static final Integer two = Integer.valueOf(2);
  protected static final Integer three = Integer.valueOf(3);
  protected static final Integer four = Integer.valueOf(4);
  protected static final Integer five = Integer.valueOf(5);
  protected static final Integer six = Integer.valueOf(6);
  protected static final Integer seven = Integer.valueOf(7);
  protected static final Integer eight = Integer.valueOf(8);
  protected static final Integer nine = Integer.valueOf(9);
  protected static final Integer m1 = Integer.valueOf(-1);
  protected static final Integer m2 = Integer.valueOf(-2);
  protected static final Integer m3 = Integer.valueOf(-3);
  protected static final Integer m4 = Integer.valueOf(-4);
  protected static final Integer m5 = Integer.valueOf(-5);
  protected static final Integer m6 = Integer.valueOf(-6);
  protected static final Integer m10 = Integer.valueOf(-10);

  static class NoOpCallable implements Callable {

    public Object call() {
      return Boolean.TRUE;
    }
  }

  public void threadAssertFalse(boolean b) {
    if (b) {
      threadFailed = true;
      assertFalse(b);
    }
  }
  protected static final int SIZE = 20;

  public void threadAssertEquals(long x, long y) {
    if (x != y) {
      threadFailed = true;
      assertEquals(x, y);
    }
  }

  public void threadAssertTrue(boolean b) {
    if (!b) {
      threadFailed = true;
      assertTrue(b);
    }
  }

  public void threadAssertNull(Object x) {
    if (x != null) {
      threadFailed = true;
      assertNull(x);
    }
  }

  public void threadAssertEquals(Object x, Object y) {
    if (x != y && (x == null || !x.equals(y))) {
      threadFailed = true;
      assertEquals(x, y);
    }
  }

  public void joinPool(ExecutorService exec) {
    try {
      exec.shutdown();
      boolean b = exec.awaitTermination(LONG_DELAY_MS, MILLISECONDS);
      Verify.ignoreIf(!b);
      assertTrue(b);
    } catch (SecurityException ok) {
      // Allowed in case test doesn't have privs
    } catch (InterruptedException ie) {
      fail("Unexpected exception");
    }
  }

  /**
   * Fail, also setting status to indicate current testcase should fail
   */
  public void threadFail(String reason) {
    threadFailed = true;
    fail(reason);
  }

  public void printFinish() {
    // pcm - if we use gov.nasa.jpf.util.Reflection here it means we have to
    // include the jpf.jar in the classpath (this is executed from JPF!). This
    // seems overkill
    //System.out.println(Reflection.getCallerElement().getMethodName() + "-Finish");
  }
}
