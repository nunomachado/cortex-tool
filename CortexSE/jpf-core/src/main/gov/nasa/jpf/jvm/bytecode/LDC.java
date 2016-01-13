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
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.NoClassInfoException;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;


/**
 * Push item from runtime constant pool
 * ... => ..., value
 */
public class LDC extends Instruction {

  public enum Type {STRING, CLASS, INT, FLOAT};

  Type type;

  protected String  string;  // the string value if Type.STRING, classname if Type.CLASS
  protected int     value;

  public LDC() {}

  public LDC (String s, boolean isClass){
    if (isClass){
      string = Types.getClassNameFromTypeName(s);
      type = Type.CLASS;
    } else {
      string = s;
      type = Type.STRING;
    }
  }

  public LDC (int v){
    value = v;
    type = Type.INT;
  }

  public LDC (float f){
    value = Float.floatToIntBits(f);
    type = Type.FLOAT;
  }


  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    switch (type){
      case STRING:
        // too bad we can't cache it, since location might change between different paths
        value = ti.getHeap().newInternString(string, ti);
        ti.push(value, true);
        break;

      case INT:
      case FLOAT:
        ti.push(value, false);
        break;

      case CLASS:
        try {
          ClassInfo ci = ClassInfo.getResolvedClassInfo(string);

          // LDC doesn't cause a <clinit> - we only register all required classes
          // to make sure we have class objects. <clinit>s are called prior to
          // GET/PUT or INVOKE
          if (!ci.isRegistered()) {
            ci.registerClass(ti);
          }

          ti.push(ci.getClassObjectRef(), true);

        } catch (NoClassInfoException cx) {
          // can be any inherited class or required interface
          return ti.createAndThrowException("java.lang.NoClassDefFoundError", cx.getMessage());
        }
        break;
    }
    
    return getNext(ti);
  }

  public int getLength() {
    return 2; // opcode, index
  }

  public int getByteCode () {
    return 0x12;
  }
  
  public int getValue() {
    return value;
  }
  
  public Type getType() {
    return type;
  }
  
  public boolean isString() {
    return (type == Type.STRING);
  }
  
  public float getFloatValue(){
	  if(type!=Type.FLOAT){
      throw new IllegalStateException();
	  }
    
	  return Float.intBitsToFloat(value);
	}

  public String getStringValue() { // if it is a String (not acquired from the class const pool)
    if (type == Type.STRING) {
      return string;
    } else {
      return null;
    }
  }
  
  public String getClassValue() { // if it is the name of a Class (acquired from the class const pool)
	    if (type == Type.CLASS) {
	      return string;
	    } else {
	      return null;
	    }
	  }

  
  public void accept(InstructionVisitor insVisitor) {
	  insVisitor.visit(this);
  }
}
