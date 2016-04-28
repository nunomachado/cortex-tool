//
// Copyright (C) 2007 United States Government as represented by the
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
package gov.nasa.jpf.test.java.io;

import gov.nasa.jpf.util.test.TestJPF;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * raw test for java.io.BufferedInputStream
 */
public class BufferedInputStreamTest extends TestJPF {

  //-------------------- driver to execute single test methods
  public static void main(String[] args) throws Throwable {
    createTestFile();           // <2do> Fix TestJPF so that main() is not needed and @Before and @After are executed for each @Test
    try {
      runTestsOfThisClass(args);
    } finally {
      deleteTestFile();
    }
  }

  @Before
  public void setUp() {
    createTestFile();
  }

  @After
  public void tearDown() {
    deleteTestFile();
  }

  public static void createTestFile() {
    try {
      FileOutputStream fo = new FileOutputStream(testFile);
      fo.write(TEST_DATA);
      fo.close();
    } catch (Throwable t) {
      throw new RuntimeException("failed to create test file", t);
    }
  }

  public static void deleteTestFile() {
    if (testFile.exists()) {
      testFile.delete();
    }
  }

  //--- the tests
  static File testFile = new File("__test__");
  static final byte[] TEST_DATA = {42, 42, 42};

  @Test
  public void testSimpleRead() {
    if (verifyNoPropertyViolation()) {
      try {
        FileInputStream fis = new FileInputStream(testFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        int n = bis.available();

        assert n == TEST_DATA.length : "wrong available count: " + n;

        for (int i = 0; i < n; i++) {
          int d = bis.read();
          System.out.print(d);
          System.out.print(',');
          assert d == TEST_DATA[i] : "wrong read data";
        }
        System.out.println();

        bis.close();

      } catch (Throwable t) {
        assert false : "BufferedInputStream test failed: " + t;
      }
    }
  }
}
