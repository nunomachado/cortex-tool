//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.jvm;

import gov.nasa.jpf.jvm.bytecode.NATIVERETURN;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.Misc;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * a stack frame for MJI methods
 *
 * NOTE: operands and locals can be, but are not automatically used during
 * native method execution.
 */
public class NativeStackFrame extends StackFrame {


  // we don't use the operand stack or locals for arguments and return value
  // because (1) they don't have the right representation (host VM),
  // (2) for performance reasons (no array alloc), and (3) because there is no
  // choice point once we enter a native method, so there is no need to do
  // copy-on-write on the ThreadInfo callstack. Native method execution is
  // atomic (leave alone roundtrips of course)

  // return value registers
  Object ret;
  Object retAttr;

  // our argument registers
  Object[] args;

  public NativeStackFrame (NativeMethodInfo mi, StackFrame caller, Object[] argValues){
    super(mi,0,0);

    if (!mi.isStatic()){
      thisRef = caller.getCalleeThis(mi);
    }

    args = argValues;
  }

  public StackFrame clone () {
    NativeStackFrame sf = (NativeStackFrame) super.clone();

    if (args != null) {
      sf.args = args.clone();
    }

    return sf;
  }

  @Override
  public boolean isNative() {
    return true;
  }

  @Override
  public boolean isSynthetic() {
    return true;
  }

  @Override
  public boolean modifiesState() {
    // native stackframes don't do anything with their operands or locals per se
    // they are executed atomically, so there is no need to ever restore them
    return false;
  }

  @Override
  public boolean hasAnyRef() {
    return false;
  }

  public void setReturnAttr (Object a){
    retAttr = a;
  }

  public void setReturnValue(Object r){
    ret = r;
  }

  public void clearReturnValue() {
    ret = null;
    retAttr = null;
  }

  public Object getReturnValue() {
    return ret;
  }

  public Object getReturnAttr() {
    return retAttr;
  }

  public Object[] getArguments() {
    return args;
  }

  public void markThreadRoots (Heap heap, int tid) {
    // what if some listener creates a CG post-EXECUTENATIVE or pre-NATIVERETURN?
    // and the native method returned an object?
    // on the other hand, we have to make sure we don't mark a return value from
    // a previous transition

    if (pc instanceof NATIVERETURN){
      if (ret != null && ret instanceof Integer && mi.isReferenceReturnType()) {
        int ref = ((Integer) ret).intValue();
        heap.markThreadRoot(ref, tid);
      }
    }
  }

  protected void hash (HashData hd) {
    super.hash(hd);

    if (ret != null){
      hd.add(ret);
    }
    if (retAttr != null){
      hd.add(retAttr);
    }

    for (Object a : args){
      hd.add(a);
    }
  }

  public boolean equals (Object object) {
    if (object == null || !(object instanceof NativeStackFrame)){
      return false;
    }

    if (!super.equals(object)){
      return false;
    }

    NativeStackFrame o = (NativeStackFrame)object;

    if (ret != o.ret){
      return false;
    }
    if (retAttr != o.retAttr){
      return false;
    }

    if (args.length != o.args.length){
      return false;
    }

    if (!Misc.compare(args.length, args, o.args)){
      return false;
    }

    return true;
  }

  public String toString () {
    StringWriter sw = new StringWriter(128);
    PrintWriter pw = new PrintWriter(sw);

    pw.print("NativeStackFrame@");
    pw.print(Integer.toHexString(objectHashCode()));
    pw.print("{ret=");
    pw.print(ret);
    if (retAttr != null){
      pw.print('(');
      pw.print(retAttr);
      pw.print(')');
    }
    pw.print(',');
    printContentsOn(pw);
    pw.print('}');

    return sw.toString();
  }
}
