//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.test.vm.basic;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

/**
 * test various aspects of SUT end states
 */
public class EndStateTest extends TestJPF {

  public static void main (String[] args) {  // <2do> Fix test so that it doesn't require a main
    runTestsOfThisClass(args);
  }


  @Test public void testSingleThread () {
    JPF jpf = null;

    if ((jpf = noPropertyViolation( "+listener=.test.vm.basic.EndStateListener",
                                    getClass().getName(), "testSingleThread")) == null){
      synchronized(this){
        System.out.println("** this is testSingleThread - it should succeed");
      }
    }

    if (jpf != null){
      assert jpf.getSearchErrors().isEmpty() : "unexpected JPF search errors";
      // could have a lot more here..
    }
  }

  @Test public void testMultipleThreads () {
    JPF jpf = null;

    if ((jpf = noPropertyViolation( "+listener=.test.vm.basic.EndStateListener",
                                    getClass().getName(), "testMultipleThreads")) == null){
      System.out.println("** this is testMultipleThreads - it should succeed");

      Thread t = new Thread() {
        public synchronized void run() {
          System.out.println("** this is " + Thread.currentThread().getName() + " terminating");
        }
      };
      t.start();

      synchronized(this){
        System.out.println("** this is thread main terminating");
      }
    }

    if (jpf != null){
      assert jpf.getSearchErrors().isEmpty() : "unexpected JPF search errors";
      // could have a lot more here..
    }
  }

}
