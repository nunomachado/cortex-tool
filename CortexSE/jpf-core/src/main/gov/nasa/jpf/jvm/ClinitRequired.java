package gov.nasa.jpf.jvm;

/**
 * this one is kind of a hack for situations where we detect deep from
 * the stack that we need a clinit to be executed, but we can't flag this
 * to the currently executed insn via a return value. referencing annotation
 * elements that are enums is a good (bad) example
 */
public class ClinitRequired extends RuntimeException {
  ClassInfo ci;
  
  public ClinitRequired (ClassInfo ci){
    this.ci = ci;
  }
  
  ClassInfo getRequiredClassInfo() {
    return ci;
  }
  
  public String getMessage(){
    return ci.getName();
  }
}
