//
// Copyright (C) 2011 United States Government as represented by the
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

package gov.nasa.jpf.jvm;

import gov.nasa.jpf.classfile.ClassFile;
import gov.nasa.jpf.classfile.ClassFileException;
import gov.nasa.jpf.util.test.TestJPF;

import java.io.File;

import org.junit.Test;

/**
 * unit test for MethodInfos
 */
public class MethodInfoTest extends TestJPF {

  static class MyClass {
    static double staticNoArgs() {int a=42; double b=42.0; b+=a; return b;}
    static double staticInt (int intArg) {int a=42; double b=42.0; b+=a; return b;}
    static double staticIntString (int intArg, String stringArg) {int a=42; double b=42.0; b+=a; return b;}
    
    double instanceNoArgs() {int a=42; double b=42.0; b+=a; return b;}
    double instanceInt( int intArg) {int a=42; double b=42.0; b+=a; return b;}
    double instanceIntString  (int intArg, String stringArg) {int a=42; double b=42.0; b+=a; return b;}
  }
  
  @Test
  public void testMethodArgs() {
    File file = new File("build/tests/gov/nasa/jpf/jvm/MethodInfoTest$MyClass.class");

    try {
      ClassFile cf = new ClassFile(file);
      ClassInfo ci = new NonResolvedClassInfo(cf);
      MethodInfo mi;
      LocalVarInfo[] args;

      //--- the statics
      mi = ci.getMethod("staticNoArgs", "()D", false);
      System.out.println("-- checking: " + mi);
      args = mi.getArgumentLocalVars();
      assertTrue("args not empty or null", args != null && args.length == 0);

      mi = ci.getMethod("staticInt", "(I)D", false);
      System.out.println("-- checking: " + mi);
      args = mi.getArgumentLocalVars();
      assertTrue("args null", args != null);
      for (LocalVarInfo lvi : args){
        System.out.println("     " + lvi);
      }
      assertTrue(args.length == 1 && args[0].getName().equals("intArg"));

      mi = ci.getMethod("staticIntString", "(ILjava/lang/String;)D", false);
      System.out.println("-- checking: " + mi);
      args = mi.getArgumentLocalVars();
      assertTrue("args null", args != null);
      for (LocalVarInfo lvi : args){
        System.out.println("     " + lvi);
      }
      assertTrue(args.length == 2 && args[0].getName().equals("intArg") && args[1].getName().equals("stringArg"));

      
      //--- the instances
      mi = ci.getMethod("instanceNoArgs", "()D", false);
      System.out.println("-- checking: " + mi);
      args = mi.getArgumentLocalVars();
      assertTrue("args null", args != null);
      for (LocalVarInfo lvi : args){
        System.out.println("     " + lvi);
      }
      assertTrue(args.length == 1 && args[0].getName().equals("this"));
      
      mi = ci.getMethod("instanceInt", "(I)D", false);
      System.out.println("-- checking: " + mi);
      args = mi.getArgumentLocalVars();
      assertTrue("args null", args != null);
      for (LocalVarInfo lvi : args){
        System.out.println("     " + lvi);
      }
      assertTrue(args.length == 2 && args[0].getName().equals("this") && args[1].getName().equals("intArg"));

      mi = ci.getMethod("instanceIntString", "(ILjava/lang/String;)D", false);
      System.out.println("-- checking: " + mi);
      args = mi.getArgumentLocalVars();
      assertTrue("args null", args != null);
      for (LocalVarInfo lvi : args){
        System.out.println("     " + lvi);
      }
      assertTrue(args.length == 3 && args[0].getName().equals("this") 
          && args[1].getName().equals("intArg") && args[2].getName().equals("stringArg"));

    } catch (NullPointerException npe){
      fail("method not found");
    } catch (ClassFileException cfx){
      //cfx.printStackTrace();
      fail("ClassFileException: " + cfx);
    }
  }
}
