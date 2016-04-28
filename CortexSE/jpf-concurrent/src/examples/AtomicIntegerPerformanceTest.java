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

import java.util.concurrent.atomic.AtomicInteger;

/*
 * This is a main benchmark of AtomicInteger performance.
 * To test performance of original SDK version just remove
 * extension from your site.properties file.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class AtomicIntegerPerformanceTest {

  public static void main (String[] args) {
    System.out.println("*************** AtomicInteger PERFORMANCE TEST ***************");
    new AtomicIntegerPerformanceTest().start();
  }

  public void start () {
    final AtomicInteger ai = new AtomicInteger();
    for (int i = 0; i < 8; i++) {
      new Thread(new Runnable() {
        public void run () {
          int i = ai.get();
          i = i + 1;
          ai.set(i);
        }
      }).start();
    }
  }
}
