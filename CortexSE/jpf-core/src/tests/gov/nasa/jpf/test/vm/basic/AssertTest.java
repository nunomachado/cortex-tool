//
// Copyright (C) 2006 United States Government as represented by the
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

import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

/**
 * JPF part of assertion test
 */
public class AssertTest extends TestJPF {

  @Test public void testAssertionViolation () {
    if (verifyAssertionErrorDetails("oops, assertion failed")){
      int i = 1;
      assert i == 0 : "oops, assertion failed";
    }
  }

  @Test public void testNoAssertionViolation () {
    if (verifyNoPropertyViolation("+vm.disable_assertions=*AssertTest")){
      int i = 1;
      assert i == 0 : "oops, assertion failed";
    }
  }
}
