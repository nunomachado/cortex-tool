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


import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;


/**
 * Invoke a class (static) method
 * ..., [arg1, [arg2 ...]]  => ...
 */
public class INVOKESTATIC extends InvokeInstruction {
  ClassInfo ci;
  
  protected INVOKESTATIC (String clsDescriptor, String methodName, String signature){
    super(clsDescriptor, methodName, signature);
  }


  protected ClassInfo getClassInfo () {
    if (ci == null) {
      ci = ClassInfo.getResolvedClassInfo(cname);
    }
    return ci;
  }
  
  public int getByteCode () {
    return 0xB8;
  }

  public StaticElementInfo getStaticElementInfo (){
    return getClassInfo().getStaticElementInfo();
  }

  public int getClassObjectRef(){
    return getClassInfo().getStaticElementInfo().getClassObjectRef();
  }

  public boolean isExecutable (SystemState ss, KernelState ks, ThreadInfo ti) {
    MethodInfo mi = getInvokedMethod();
    if (mi == null) {
      return true; // execute so that we get the exception
    }

    return mi.isExecutable(ti);
  }

  public boolean examineAbstraction (SystemState ss, KernelState ks,
                                     ThreadInfo ti) {
    MethodInfo mi = getInvokedMethod(ti);

   if (mi == null) {
      return true;
    }

    return !ci.isStaticMethodAbstractionDeterministic(ti, mi);
  }

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    
    // need to check first if we can find the class
    ClassInfo clsInfo = getClassInfo();
    if (clsInfo == null){
      return ti.createAndThrowException("java.lang.NoClassDefFoundError", cname);
    }

    MethodInfo callee = getInvokedMethod(ti);
    if (callee == null) {
      return ti.createAndThrowException("java.lang.NoSuchMethodException",
                                   cname + '.' + mname);
    }

    // this can be actually different than (can be a base)
    clsInfo = callee.getClassInfo();
    
    if (requiresClinitExecution(ti, clsInfo)) {
      // do class initialization before continuing
      return ti.getPC();
    }

    if (callee.isSynchronized()) {
      ElementInfo ei = clsInfo.getClassObject();
      if (checkSyncCG(ei, ss, ti)){
        return this;
      }
    }
        
    // enter the method body, return its first insn
    return callee.execute(ti);
  }
  
  public MethodInfo getInvokedMethod (ThreadInfo ti){
    return getInvokedMethod(); 
  }
  
  public MethodInfo getInvokedMethod () {
    if (invokedMethod == null) {
      ClassInfo clsInfo = getClassInfo();
      if (clsInfo != null){
        invokedMethod = clsInfo.getMethod(mname, true);
      }
    }
    return invokedMethod;
  }
  
  // can be different thatn the ci - method can be in a superclass
  public ClassInfo getInvokedClassInfo(){
    return getInvokedMethod().getClassInfo();
  }

  public String getInvokedClassName(){
    return getInvokedClassInfo().getName();
  }

  public int getArgSize () {
    if (argSize < 0) {
      argSize = Types.getArgumentsSize(signature);
    }

    return argSize;
  }

  
  public String toString() {
    // methodInfo not set outside real call context (requires target object)
    return "invokestatic " + cname + '.' + mname;
  }

  public Object getFieldValue (String id, ThreadInfo ti) {
    return getClassInfo().getStaticFieldValueObject(id);
  }
  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}

