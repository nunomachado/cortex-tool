package gov.nasa.jpf.jvm.serialize;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.AnnotationInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.serialize.AmmendableFilterConfiguration.FieldAmmendment;

public class IncludesFromAnnotations
implements FieldAmmendment {
  protected Config config;
  
  public IncludesFromAnnotations(Config config)  {
    this.config = config;
  }
  
  public boolean ammendFieldInclusion(FieldInfo fi, boolean sofar) {
    AnnotationInfo ann = fi.getAnnotation("gov.nasa.jpf.annotation.UnfilterField");
    if (ann != null) {
      return POLICY_INCLUDE;
    }
    return sofar;
  }
}
