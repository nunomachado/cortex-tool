
package gov.nasa.jpf.util.test;

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DirectCallStackFrame;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;

import java.util.ArrayList;

/**
 * native peer for our test class root
 */
public class JPF_gov_nasa_jpf_util_test_TestJPF {

  static ClassInfo testClass;
  static MethodInfo testClassCtor;

  static MethodInfo[] testMethods = null;
  static int index = 0;
  static int testObjRef = MJIEnv.NULL;

  static boolean done;

  private static void pushDirectCallFrame(MJIEnv env, MethodInfo mi, int objRef) {
    ThreadInfo ti = env.getThreadInfo();

    MethodInfo stub = mi.createDirectCallStub("[test]");
    DirectCallStackFrame frame = new DirectCallStackFrame(stub);
    frame.pushRef(objRef);
    ti.pushFrame(frame);
  }

  private static boolean initializeTestMethods(MJIEnv env, String[] selectedTests) {
    if (selectedTests != null && selectedTests.length > 0) {
      testMethods = new MethodInfo[selectedTests.length];
      int i = 0;
      for (String test : selectedTests) {
        MethodInfo mi = testClass.getMethod(test + "()V", false);
        if (mi != null && mi.isPublic() && !mi.isStatic()) {
          testMethods[i++] = mi;
        } else {
          reset____V();
          env.throwException("java.lang.RuntimeException",
                  "no such test method: public void " + test + "()");
          return false;
        }
      }
    } else { // collect all public void test..() methods
      ArrayList<MethodInfo> list = new ArrayList<MethodInfo>();
      for (MethodInfo mi : testClass) {
        if (mi.getName().startsWith("test") && mi.isPublic() && !mi.isStatic() &&
                mi.getSignature().equals("()V")) {
          list.add(mi);
        }
      }
      testMethods = list.toArray(new MethodInfo[list.size()]);
    }

    return true;
  }

  //--- our exported native methods

  public static void init() {
    reset____V();
  }

  public static void reset____V(){
    done = false;
    index = 0;
    testObjRef = MJIEnv.NULL;
    testMethods = null;
    testClass = null;
    testClassCtor = null;
  }

  public static void $init____V (MJIEnv env, int objRef){
    // nothing
  }

  public static void runTestsOfThisClass___3Ljava_lang_String_2__V (MJIEnv env, int clsObjRef,
                                                                    int selectedTestsRef) {
    ThreadInfo ti = env.getThreadInfo();

    if (!done) {
      if (testMethods == null) {
        StackFrame frame = env.getCallerStackFrame(); // the runTestsOfThisClass() caller

        testClass = frame.getClassInfo();
        testClassCtor = testClass.getMethod("<init>()V", true);

        String[] selectedTests = env.getStringArrayObject(selectedTestsRef);
        if (initializeTestMethods(env, selectedTests)) {
          env.repeatInvocation();
        }

      } else { // this is re-executed
        if (testObjRef == MJIEnv.NULL) { // create a new test object
          testObjRef = env.newObject(testClass);

          if (testClassCtor != null) {
            pushDirectCallFrame(env, testClassCtor, testObjRef);
            env.repeatInvocation();
          }

        } else { // execute the next test
          if (testMethods != null && (index < testMethods.length)) {
            MethodInfo miTest = testMethods[index++];
            pushDirectCallFrame(env, miTest, testObjRef);

            if (index < testMethods.length) {
              testObjRef = MJIEnv.NULL;
            } else {
              done = true;
            }

            env.repeatInvocation();
          }
        }
      }
    }
  }

  public static int createAndRunJPF___3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef, int argsRef){
    // don't get recursive
    return MJIEnv.NULL;
  }

  /**
   * if any of our methods are executed, we know that we already run under JPF
   */
  public static boolean isJPFRun____Z (MJIEnv env, int clsObjRef){
    return true;
  }
  public static boolean isJUnitRun____Z (MJIEnv env, int clsObjRef){
    return false;
  }
  public static boolean isRunTestRun____Z (MJIEnv env, int clsObjRef){
    return false;
  }


  // we need to override these so that the actual test code gets executed
  // if we fail to intercept, the bytecode will actually start JPF
  public static int noPropertyViolation___3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }
  public static boolean verifyNoPropertyViolation___3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return true;
  }

  public static boolean verifyAssertionErrorDetails__Ljava_lang_String_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int detailsRef, int jpfArgsRef){
    return true;
  }
  public static boolean verifyAssertionError___3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return true;
  }


  public static int unhandledException__Ljava_lang_String_2Ljava_lang_String_2_3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef,
                                  int xClassNameRef, int detailsRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }
  public static boolean verifyUnhandledException__Ljava_lang_String_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int xClassNameRef, int jpfArgsRef){
    return true;
  }
  public static boolean verifyUnhandledExceptionDetails__Ljava_lang_String_2Ljava_lang_String_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int xClassNameRef, int detailsRef, int jpfArgsRef){
    return true;
  }


  public static int propertyViolation__Ljava_lang_Class_2_3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef,
                                  int propClsRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }
  public static boolean verifyPropertyViolation__Lgov_nasa_jpf_util_TypeRef_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int propClsRef, int jpfArgsRef){
    return true;
  }


  public static int jpfException__Ljava_lang_Class_2_3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef,
                                  int xClsRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }
  public static boolean verifyJPFException__Lgov_nasa_jpf_util_TypeRef_2_3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef,
                                  int xClsRef, int jpfArgsRef){
    return true;
  }


  public static int deadlock___3Ljava_lang_String_2__Lgov_nasa_jpf_JPF_2 (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return MJIEnv.NULL;
  }
  public static boolean verifyDeadlock___3Ljava_lang_String_2__Z (MJIEnv env, int clsObjRef, int jpfArgsRef){
    return true;
  }


}
