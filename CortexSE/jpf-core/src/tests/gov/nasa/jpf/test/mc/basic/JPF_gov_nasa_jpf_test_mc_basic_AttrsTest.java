package gov.nasa.jpf.test.mc.basic;

import gov.nasa.jpf.jvm.MJIEnv;

public class JPF_gov_nasa_jpf_test_mc_basic_AttrsTest {

  public static double goNative__DI__D (MJIEnv env, int objRef, double d, int i) {

    Object[] attrs = env.getArgAttributes();

    if ((attrs[0] != null) && (attrs[0] instanceof Integer) &&
        (attrs[1] != null) && (attrs[1] instanceof Integer) &&
        (attrs[2] != null) && (attrs[2] instanceof Integer)) {
      Integer ra = (Integer)attrs[0] + (Integer)attrs[1] + (Integer)attrs[2];
      env.setReturnAttribute(ra);
    }

    return d * i;
  }
}
