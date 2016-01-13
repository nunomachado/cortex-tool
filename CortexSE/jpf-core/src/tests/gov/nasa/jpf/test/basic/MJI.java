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

/**
 * model class for MJI test
 */
public class MJI {

  static int sdata;

  static {
    // only here to be intercepted
    sdata = 0; // dummy insn required for the Eclipse compiler (skips empty methods)
  }

  int idata = 0;

  public static void main (String[] args){
    MJI test = new MJI();

    // unfortunately. String.equals() is using MJI too, so we don't want to
    // rely on this to select the test methods

    for (String arg : args){
      switch (arg.charAt(0)){
        case 'a' : test.testNativeClInit(); break;
        case 'b' : test.testNativeStaticMethod(); break;
        case 'c' : test.testNativeInstanceMethod(); break;
        case 'd' : test.testNativeInit(); break;
        case 'e' : test.testNativeCreateIntArray(); break;
        case 'f' : test.testNativeCreateStringArray(); break;
        case 'g' : test.testNativeCreate2DimIntArray(); break;
        case 'h' : test.testNativeException(); break;
        case 'i' : test.testNativeCrash(); break;
        case 'j' : test.testRoundtripLoop(); break;
        case 'k' : test.testHiddenRoundtrip(); break;
        case 'l' : test.testHiddenRoundtripException(); break;
        default:
          throw new RuntimeException("unknown test method: " + arg);
      }
    }
  }


  MJI () {
    // not intercepted
  }

  MJI (int data) {
    // only here to be intercepted
  }


  public void testNativeClInit () {
    assert (sdata == 42) : "native '<clinit>' failed";
  }

  public void testNativeInit () {
    MJI t = new MJI(42);
    assert (t.idata == 42)  : "native '<init>' failed";
  }

  public void testNativeCreate2DimIntArray () {
    int[][] a = nativeCreate2DimIntArray(2, 3);

    assert (a != null)  : "native int[][]  creation failed: null";

    assert (a.getClass().isArray()) : "native int[][] creation failed: not an array";

    assert (a.getClass().getComponentType().getName().equals("[I")) :
      "native int[][] creation failed: wrong component type";

    assert ((a[1][1] == 42)) : "native int[][] element init failed";
  }

  public void testNativeCreateIntArray () {
    int[] a = nativeCreateIntArray(3);

    assert (a != null)  : "native int array creation failed: null";

    assert (a.getClass().isArray()) : "native int array creation failed: not an array";

    assert (a.getClass().getComponentType() == int.class) :
            "native int array creation failed: wrong component type";

    assert ((a[1] == 1)) : "native int array element init failed";
  }

  public void testNativeCreateStringArray () {
    String[] a = nativeCreateStringArray(3);

    assert (a != null) : "native String array creation failed: null";

    assert (a.getClass().isArray()) :
      "native String array creation failed: not an array";

    assert (a.getClass().getComponentType() == String.class):
            "native String array creation failed: wrong component type";

    assert ("one".equals(a[1])) : "native String array element init failed";
  }

  public void testNativeException () {
    try {
      nativeException();
    } catch (UnsupportedOperationException ux) {
      String details = ux.getMessage();

      if ("caught me".equals(details)) {
        ux.printStackTrace();
        return;
      } else {
        assert false : "wrong native exception details: " + details;
      }
    } catch (Throwable t) {
      assert false : "wrong native exception type: " + t.getClass();
    }

    assert false : "no native exception thrown";
  }

  public void testNativeCrash () {
    nativeCrash();
  }

  public void testNativeInstanceMethod () {
    int res = nativeInstanceMethod(2.0, '?', true, 40);

    assert (res == 42) : "native instance method failed: " + res;
  }

  public void testNativeStaticMethod () {
    long res = nativeStaticMethod(40, "Blah");

    assert (res == 42L) : "native static method failed";
  }


  int roundtrip (int a){ // that's called from the native testRoundtripLoop0
    System.out.println("### roundtrip " + a);
    return nativeInnerRoundtrip(a);
  }

  public void testRoundtripLoop () {
    int res = nativeRoundtripLoop(42);

    assert (res == 54) : ("roundtrip loop failed (expected 54) : " + res);
  }

  public void testHiddenRoundtrip () {
    System.out.println("## entering testHiddenroundtrip()");
    int res = echo(20) + nativeHiddenRoundtrip(21); // get something on the operand stack
    assert (res == 42) : ("atomic roundtrip failed (expected 42): " + res);

    System.out.println("## exiting testHiddenroundtrip()");
  }

  public void testHiddenRoundtripException () {
    System.out.println("## entering testHiddenroundtripException()");
    int res = echo(20) + nativeHiddenRoundtrip(-1); // get something on the operand stack
    assert (res == 19) : "atomic roundtrip exception not caught";

    System.out.println("## exiting testHiddenroundtripException()");
  }

  int atomicStuff (int a) {  // this is called from nativeAtomicRoundtrip()
    System.out.print("## in atomicStuff : ");
    System.out.println(a);

    if (a < 0) {
      System.out.println("## atomicStuff throwing IllegalArgumentException");
      throw new IllegalArgumentException("negative atomicStuff argument");
    }

    int res = echo(a + 1);
    return res;
  }
  int echo (int a) {
    System.out.print("## in echo : ");
    System.out.println(a);
    return a;
  }

  native int nativeHiddenRoundtrip (int a);

  native int nativeInnerRoundtrip (int a);

  native int nativeRoundtripLoop (int a);

  native int[][] nativeCreate2DimIntArray (int s1, int s2);

  native int[] nativeCreateIntArray (int size);

  native String[] nativeCreateStringArray (int size);

  native void nativeException ();

  native int nativeCrash ();

  native int nativeInstanceMethod (double d, char c, boolean b, int i);

  native long nativeStaticMethod (long l, String s);

}
