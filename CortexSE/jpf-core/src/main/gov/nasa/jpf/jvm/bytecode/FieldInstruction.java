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
package gov.nasa.jpf.jvm.bytecode;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.FieldLockInfo;
import gov.nasa.jpf.jvm.FieldLockInfoFactory;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;

/**
 * parent class for PUT/GET FIELD/STATIC insns
 *
 * <2do> there is a inheritance level missing to deal with instance/static
 * fields - w/o the instance/static helper methods we would have to duplicate
 * code in the getters/setters
 */
public abstract class FieldInstruction extends Instruction implements VariableAccessor
{
  //--- vm.por.sync_detection related settings
  static FieldLockInfoFactory fliFactory;
  static boolean skipFinals; // do we ignore final fields for POR
  static boolean skipStaticFinals;  // do we ignore static final fields for POR
  static boolean skipConstructedFinals;  // do we ignore final fields for POR after the object's constructor has finished?

  
  protected String fname;
  protected String className;
  protected String varId;

  protected FieldInfo fi; // lazy eval, hence not public

  protected int    size;  // is it a word or a double word field
  protected boolean isReferenceField;

  protected long lastValue;
  
  public static void init (Config config) {
    if (config.getBoolean("vm.por") && config.getBoolean("vm.por.sync_detection")) {
      fliFactory = config.getEssentialInstance("vm.por.fli_factory.class", FieldLockInfoFactory.class);

      skipFinals = config.getBoolean("vm.por.skip_finals", true);
      skipStaticFinals = config.getBoolean("vm.por.skip_static_finals", false);
      skipConstructedFinals = config.getBoolean("vm.por.skip_constructed_finals", false);
    }
  }

  protected FieldInstruction() {}

  protected FieldInstruction(String name, String clsName, String fieldDescriptor){
    fname = name;
    className = Types.getClassNameFromTypeName(clsName);
    isReferenceField = Types.isReferenceSignature(fieldDescriptor);
    size = Types.getTypeSize(fieldDescriptor);
  }

  public String getClassName(){
     return className;
  }

  /**
   * only defined in instructionExecuted() notification context
   */
  public long getLastValue() {
    return lastValue;
  }

  
  public abstract boolean isRead();

  public abstract FieldInfo getFieldInfo ();

  // that's for an instructionExecuted() context
  public abstract ElementInfo getLastElementInfo();

  // that's for an executeInstruction() or choiceGeneratorSet() context
  public abstract ElementInfo peekElementInfo (ThreadInfo ti);

  public boolean isReferenceField () {
    return isReferenceField;
  }

  public int getFieldSize() {
    return size;
  }

  public String getId(ElementInfo ei) {
    // <2do> - OUTCH, should be optimized (so far, it's only called during reporting)
    if (ei != null){
      return (ei.toString() + '.' + fname);
    } else {
      return ("?." + fname);
    }
  }

  public String getVariableId () {
    if (varId == null) {
      varId = className + '.' + fname;
    }
    return varId;
  }

  /**
   * is this field supposed to be protected by a lock?
   * this only gets called if on-the-fly POR is in effect
   * 
   * NOTE this has the side effect of setting FieldLockInfos, which
   * in turn can be heuristic and volatile (i.e. might force an
   * object to stay in memory just because we have to detect
   * subsequent lock assumption violations
   */
  protected boolean isLockProtected (ThreadInfo ti, ElementInfo ei) {

    FieldInfo fi = getFieldInfo(); // so that we make sure it's computed
    FieldLockInfo flInfo = ei.getFieldLockInfo(fi);
    FieldLockInfo flInfoNext;
        
    if (flInfo == null) {
      flInfoNext = fliFactory.createFieldLockInfo(ti, ei, fi);
      ei.setFieldLockInfo(fi, flInfoNext);
      
    }  else {
      flInfoNext = flInfo.checkProtection(ti,ei,fi);
      if (flInfo != flInfoNext) {
        ei.setFieldLockInfo(fi, flInfoNext);
      }
    }
    
    return flInfoNext.isProtected();
  }
  
  /**
   * do a little bytecode pattern analysis on the fly, to find out if a
   * GETFIELD or GETSTATIC is just part of a "..synchronized (obj) {..} .."
   * pattern, which usually translates into some
   *   ...
   *   getfield
   *   dup
   *   [astore]
   *   monitorenter
   *   ...
   *
   *   pattern. If it does, there is no need to break the transition.
   *
   *   <2do> We might want to extend this in the future to also cover sync on
   *   local vars, like "Object o = myField; synchronized(o){..}..", but then
   *   the check becomes more expensive since we get interspersed aload/astore
   *   insns, and some of the locals could be used outside the sync block. Not
   *   sure if it buys much on the bottom line
   *   
   *   <2do> does this rely on javac code patterns? The dup/astore could
   *   lead to subsequent use of the object reference w/o corresponding get/putfield
   *   insns (if it's not a volatile), but this access would be either a call
   *   or a get/putfield on a share object, i.e. would be checked separately 
   */
  protected boolean isMonitorEnterPrologue () {
    Instruction[] code = mi.getInstructions();
    int off = insnIndex+1;

    if (off < code.length-3) {
      // we don't reach out further than 3 instructions
      if (code[off] instanceof DUP) {
        off++;

        if (code[off] instanceof ASTORE) {
          off++;
        }

        if (code[off] instanceof MONITORENTER) {
          return true;
        }
      }
    }
    
    return false; // if in doubt, we break the transition
  }

  
  protected boolean createAndSetFieldCG ( SystemState ss, ElementInfo ei, ThreadInfo ti) {
    ChoiceGenerator<?> cg = ss.getSchedulerFactory().createSharedFieldAccessCG(ei, ti);
    if (cg != null) {
      if (ss.setNextChoiceGenerator(cg)){
        ti.skipInstructionLogging(); // <2do> Hmm, might be more confusing not to see it
        return true;
      }
    }

    return false;
  }

  /**
   * for explicit construction
   */
  public void setField(String fname, String fclsName) {
    this.fname = fname;
    this.className = fclsName;
    if (fclsName.equals("long") || fclsName.equals("double")) {
      this.size = 2;
      this.isReferenceField = false;
    } else {
      this.size = 1;
      if (fclsName.equals("boolean") || fclsName.equals("byte") || fclsName.equals("char") || fclsName.equals("short") || fclsName.equals("int")) {
        this.isReferenceField = false;
      } else {
        this.isReferenceField = true;
      }
    }
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }

}








