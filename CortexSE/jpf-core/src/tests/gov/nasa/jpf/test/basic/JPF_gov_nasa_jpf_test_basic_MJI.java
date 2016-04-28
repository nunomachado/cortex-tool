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

import gov.nasa.jpf.jvm.DirectCallStackFrame;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.UncaughtException;

/**
 * native peer class for unit testing MJI
 */
public class JPF_gov_nasa_jpf_test_basic_MJI {

  // intercept <clinit>
  public static void $clinit (MJIEnv env, int rcls) {
    System.out.println("# entering native <clinit>");
    env.setStaticIntField(rcls, "sdata", 42);
  }

  // intercept MJITest(int i) ctor
  public static void $init__I__V (MJIEnv env, int robj, int i) {
    // NOTE : if we directly intercept the ctor, then we also have
    // to take care of calling the proper superclass ctors
    // better approach is to refactor this into a separate native method
    // (say init0(..))
    System.out.println("# entering native <init>(I)");
    env.setIntField(robj, "idata", i);
  }

  public static int nativeCreate2DimIntArray__II___3_3I (MJIEnv env, int robj, int size1,
                                              int size2) {
    System.out.println("# entering nativeCreate2DimIntArray()");
    int ar = env.newObjectArray("[I", size1);

    for (int i = 0; i < size1; i++) {
      int ea = env.newIntArray(size2);

      if (i == 1) {
        env.setIntArrayElement(ea, 1, 42);
      }

      env.setReferenceArrayElement(ar, i, ea);
    }

    return ar;
  }

  // check if the non-mangled name lookup works
  public static int nativeCreateIntArray (MJIEnv env, int robj, int size) {
    System.out.println("# entering nativeCreateIntArray()");

    int ar = env.newIntArray(size);

    env.setIntArrayElement(ar, 1, 1);

    return ar;
  }

  public static int nativeCreateStringArray (MJIEnv env, int robj, int size) {
    System.out.println("# entering nativeCreateStringArray()");

    int ar = env.newObjectArray("Ljava/lang/String;", size);
    env.setReferenceArrayElement(ar, 1, env.newString("one"));

    return ar;
  }

  public static void nativeException____V (MJIEnv env, int robj) {
    System.out.println("# entering nativeException()");
    env.throwException("java.lang.UnsupportedOperationException", "caught me");
  }

  @SuppressWarnings("null")
  public static int nativeCrash (MJIEnv env, int robj) {
    System.out.println("# entering nativeCrash()");
    String s = null;
    return s.length();
  }

  public static int nativeInstanceMethod (MJIEnv env, int robj, double d,
                                          char c, boolean b, int i) {
    System.out.println("# entering nativeInstanceMethod() d=" + d +
            ", c=" + c + ", b=" + b + ", i=" + i);

    if ((d == 2.0) && (c == '?') && b) {
      return i + 2;
    }

    return 0;
  }

  public static long nativeStaticMethod__JLjava_lang_String_2__J (MJIEnv env, int rcls, long l,
                                                                  int stringRef) {
    System.out.println("# entering nativeStaticMethod()");

    String s = env.getStringObject(stringRef);

    if ("Blah".equals(s)) {
      return l + 2;
    }

    return 0;
  }


  /*
   * nativeRoundtripLoop shows how to
   *
   *  (1) round trip into JPF executed code from within native methods
   *
   *  (2) loop inside of native methods that do round trips (using the
   *      DirectCallStackFrame's local slots)
   *
   * the call chain is:
   *
   *   JPF: testRoundtripLoop
   *     JVM: nativeRoundTripLoop  x 3
   *       JPF: roundtrip
   *         JVM: nativeInnerRoundtrip
   */

  public static int nativeInnerRoundtrip__I__I (MJIEnv env, int robj, int a){
    System.out.println("# entering nativeInnerRoundtrip()");

    return a+2;
  }

  public static int nativeRoundtripLoop__I__I (MJIEnv env, int robj, int a) {
    System.out.println("# entering nativeRoundtripLoop(): " + a);

    ThreadInfo ti = env.getThreadInfo();
    StackFrame frame = ti.getReturnedDirectCall();

    if (frame == null){ // first time
      MethodInfo mi = env.getClassInfo(robj).getMethod("roundtrip(I)I",false);
      MethodInfo stub = mi.createDirectCallStub("[roundtrip]" + mi.getName());
      frame = new DirectCallStackFrame(stub, 2, 1);

      frame.setLocalVariable(0, 0, false);
      frame.pushRef(robj);
      frame.push(a+1);
      ti.pushFrame(frame);

      return 42; // whatever, we come back

    } else { // direct call returned

      // this shows how to get information back from the JPF roundtrip into
      // the native method
      int r = frame.pop(); // the return value of the direct call above
      int i = frame.getLocalVariable(0);

      if (i < 3) { // repeat the round trip
        // we have to reset so that the PC is re-initialized
        frame.reset();
        frame.setLocalVariable(0, i + 1, false);
        frame.pushRef(robj);
        frame.push(r + 1);
        ti.pushFrame(frame);
        return 42;

      } else { // done, return the final value
        return r;
      }
    }
  }

  /**
   * this shows how to synchronously JPF-execute a method from native peer or
   * listener code
   */
  public static int nativeHiddenRoundtrip__I__I (MJIEnv env, int robj, int a){
    System.out.println("# entering nativeHiddenRoundtrip: " + a);
    MethodInfo mi = env.getClassInfo(robj).getMethod("atomicStuff(I)I",false);

    MethodInfo stub = mi.createDirectCallStub("[roundtrip]" + mi.getName());
    stub.setFirewall(true); // we don't want to let exceptions pass through this

    DirectCallStackFrame frame = new DirectCallStackFrame(stub);
    frame.push(robj); // push 'this'
    frame.push(a);    // push 'a'

    ThreadInfo ti = env.getThreadInfo();
    try {
      ti.executeMethodHidden(frame);
      //ti.advancePC();

    } catch (UncaughtException ux) {  // frame's method is firewalled
      System.out.println("# hidden method execution failed, leaving nativeHiddenRoundtrip: " + ux);
      ti.clearPendingException();
      ti.popFrame(); // this is still the DirectCallStackFrame, and we want to continue execution
      return -1;
    }

    // get the return value from the (already popped) frame
    int res = frame.peek();

    System.out.println("# exit nativeHiddenRoundtrip: " + res);
    return res;
  }

}
