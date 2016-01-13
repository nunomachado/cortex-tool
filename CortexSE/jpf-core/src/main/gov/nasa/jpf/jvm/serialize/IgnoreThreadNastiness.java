package gov.nasa.jpf.jvm.serialize;

import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.serialize.AmmendableFilterConfiguration.FieldAmmendment;


public class IgnoreThreadNastiness implements FieldAmmendment {
  public boolean ammendFieldInclusion(FieldInfo fi, boolean sofar) {
    String cname = fi.getClassInfo().getName();
    String fname = fi.getName();
    if (cname.equals("java.lang.Thread")) {
      if (!fname.equals("target")) {
        return POLICY_IGNORE;  // nothing but perhaps `target' should be critical
        // (that includes static fields)
      }
    } else if (cname.equals("java.lang.ThreadGroup")) {
      return POLICY_IGNORE;  // hopefully none of it is critical
      // (that includes static fields; not that there are any)
    }
    return sofar;
  }
  
  
  public static final IgnoreThreadNastiness instance = new IgnoreThreadNastiness();
}
