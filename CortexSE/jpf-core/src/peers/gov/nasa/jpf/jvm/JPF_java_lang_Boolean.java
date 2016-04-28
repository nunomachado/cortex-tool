package gov.nasa.jpf.jvm;

public class JPF_java_lang_Boolean {
  // <2do> at this point we deliberately do not override clinit

  public static int valueOf__Z__Ljava_lang_Boolean_2 (MJIEnv env, int clsRef, boolean val) {
    return env.valueOfBoolean(val);
  }
}
