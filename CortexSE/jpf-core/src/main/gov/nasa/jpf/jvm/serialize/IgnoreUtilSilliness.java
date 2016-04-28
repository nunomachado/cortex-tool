package gov.nasa.jpf.jvm.serialize;

import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.serialize.AmmendableFilterConfiguration.FieldAmmendment;


public class IgnoreUtilSilliness implements FieldAmmendment {
  public boolean ammendFieldInclusion(FieldInfo fi, boolean sofar) {
    String cname = fi.getClassInfo().getName();
    String fname = fi.getName();
    if (cname.startsWith("java.util.")) {
      if (fname.endsWith("odCount")) {
        // catches all this `modCount' business
        return POLICY_IGNORE;
      }
    }
    return sofar;
  }
  
  
  public static final IgnoreUtilSilliness instance = new IgnoreUtilSilliness();
}
