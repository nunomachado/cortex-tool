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

import java.util.concurrent.locks.ReentrantLock;

/*
 * This is a main benchmark of ReentrantLock performance.
 * To test performance of original SDK version just remove
 * extension from your site.properties file.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class ReentrantLockPerformanceTest {

  public static void main(String[] args) {
    System.out.println("*************** ReentrantLock PERFORMANCE TEST ***************");
    new ReentrantLockPerformanceTest().start();
  }

  public void start() {
    final ReentrantLock l1 = new ReentrantLock();
    for (int i = 0; i < 4; i++) {
      TestThread t = new TestThread(l1);
      t.start();
    }
  }

  class TestThread extends Thread {

    private ReentrantLock l;

    public TestThread(ReentrantLock lock) {
      l = lock;
    }

    public void run() {
      l.lock();
      l.unlock();
    }
  }
}
