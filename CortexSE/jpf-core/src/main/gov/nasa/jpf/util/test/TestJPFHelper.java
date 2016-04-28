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
package gov.nasa.jpf.util.test;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.Misc;
import gov.nasa.jpf.util.Reflection;

/**
 * helper class to run test classes that don't have a main() method
 * Note - this gets loaded by the JPFClassLoader, i.e. all the paths are already set
 * (we can use everything from jpf.jar)
 */
public class TestJPFHelper {

  public static void main(String args[]) throws Throwable {
    if (!TestJPF.isJPFRun()) {
      if (args.length == 0) {
        throw new JPFException("no test target class specified");
      }

      try {
        Class<?> cls = Class.forName(args[0]);
        args = Misc.arrayWithoutFirst(args, 1);

        if (!Reflection.tryCallMain(cls,args)){
          runWithoutMain(cls,args);
        }

      } catch (ClassNotFoundException cnfx) {
        throw new JPFException("testClass not found by TestJPFHelper: " + args[0]);
      }

    } else {
      TestJPF.runTestOfClass(args);
    }
  }


  // no main in test class, start via TestJPF
  static void runWithoutMain(Class<?> cls, String[] args){
    if (TestJPF.class.isAssignableFrom(cls)) {
      Class<? extends TestJPF> testCls = cls.asSubclass(TestJPF.class);

      TestJPF.runTests(testCls, args);

    } else {
      throw new JPFException("testClass not a gov.nasa.jpf.util.test.TestJPF subclass");
    }
  }
}
