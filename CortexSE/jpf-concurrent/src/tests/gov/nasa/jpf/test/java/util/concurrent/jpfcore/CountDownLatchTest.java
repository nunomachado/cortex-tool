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
package gov.nasa.jpf.test.java.util.concurrent.jpfcore;

import gov.nasa.jpf.test.java.util.concurrent.TestCaseHelpers;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

// <2do> there seems to be some rather nasty state explosion - revisit
// once we model java.util.concurrent
public class CountDownLatchTest extends TestCaseHelpers {

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  @Test
  public void testCountDown() {
    if (verifyNoPropertyViolation()) {

      final int n = 2; // <2do> bump up once we model java.util.concurrent

      final CountDownLatch done = new CountDownLatch(n);
      final Exchanger<Object> exchanger = new Exchanger<Object>();
      final ExecutorService es = Executors.newFixedThreadPool(3);
      for (int i = 0; i < n; i++) {
        es.submit(new Runnable() {

          public void run() {
            try {
              exchanger.exchange(new Object(), 1L, TimeUnit.SECONDS);
            } catch (Throwable e) {
              throw new Error(e);
            } finally {
              done.countDown();
            }
          }
        });
      }
      try {
        done.await();
        es.shutdown();
      } catch (InterruptedException ix) {
      }
    }
    printFinish();
  }
}
