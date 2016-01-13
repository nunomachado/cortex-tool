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

package gov.nasa.jpf.test.basic;

import gov.nasa.jpf.JPFNativePeerException;
import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

/**
 * JPF test driver for MJI.
 *
 * this is our most basic test since if MJI fails there is hardly anything we
 * can run with JPF
 *
 * Since this is so fundamental, we use the smallest amount of functionality from
 * the testing harness, which is based on MJI itself, i.e. would not work properly
 * if this test fails
 */
public class MJITest extends TestJPF {

  static String TEST_CLASS = "gov.nasa.jpf.test.basic.MJI";

  /**************************** tests **********************************/

  /* codes to select test functions in model class
   * (we need to keep this simple to avoid depending on MJI for the test itself)
         'a' : testNativeClInit()
         'b' : testNativeStaticMethod()
         'c' : testNativeInstanceMethod()
         'd' : testNativeInit()
         'e' : testNativeCreateIntArray()
         'f' : testNativeCreateStringArray()
         'g' : testNativeCreate2DimIntArray()
         'h' : testNativeException()
         'i' : testNativeCrash()
         'j' : testRoundtripLoop()
         'k' : testHiddenRoundtrip()
         'l' : testHiddenRoundtripException()
   */

  @Test public void testNativeClInit () {
    noPropertyViolation(TEST_CLASS, "a");
  }
  @Test public void testNativeStaticMethod () {
    noPropertyViolation(TEST_CLASS, "b");
  }
  @Test public void testNativeInstanceMethod () {
    noPropertyViolation(TEST_CLASS, "c");
  }
  @Test public void testNativeInit () {
    noPropertyViolation(TEST_CLASS, "d");
  }
  @Test public void testNativeCreateIntArray () {
    noPropertyViolation(TEST_CLASS, "e");
  }
  @Test public void testNativeCreateStringArray () {
    noPropertyViolation(TEST_CLASS, "f");
  }
  @Test public void testNativeCreate2DimIntArray () {
    noPropertyViolation(TEST_CLASS, "g");
  }
  @Test public void testNativeException () {
    noPropertyViolation(TEST_CLASS, "h");
  }
  @Test public void testNativeCrash () {
    jpfException(JPFNativePeerException.class, TEST_CLASS, "i");
  }
  @Test public void testRoundtripLoop () {
    noPropertyViolation(TEST_CLASS, "j");
  }
  @Test public void testHiddenRoundtrip () {
    noPropertyViolation(TEST_CLASS, "k");
  }
  @Test public void testHiddenRoundtripException () {
    noPropertyViolation(TEST_CLASS, "l");
  }

}
