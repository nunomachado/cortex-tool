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
package gov.nasa.jpf.symbc.bytecode;

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.Heap;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.NoClassInfoException;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.symbc.string.SymbolicStringBuilder;


// Corina's comment: probably this is not necessary here

// TODO: to review



/**
 * Create new object
 * ... => ..., objectref
 */
public class NEW extends gov.nasa.jpf.jvm.bytecode.NEW {
	public NEW (String clsName) {
	    super(clsName);
	  }
  @Override
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
	  Heap da = ti.getHeap();
	    ClassInfo ci;

	    try {
	      ci = ClassInfo.getResolvedClassInfo(cname);

	    } catch (NoClassInfoException cx){
	      // can be any inherited class or required interface
	      return ti.createAndThrowException("java.lang.NoClassDefFoundError", cx.getMessage());
	    }

	    String className = ci.getName();
	      if(!(className.equals("java.lang.StringBuilder") || className.equals("java.lang.StringBuffer")))
	    	  return super.execute(ss, ks, ti);

	    if (!ci.isRegistered()){
	      ci.registerClass(ti);
	    }

	    // since this is a NEW, we also have to pushClinit
	    if (!ci.isInitialized()) {
	      if (ci.initializeClass(ti)) {
	        return ti.getPC();  // reexecute this instruction once we return from the clinits
	      }
	    }

	    if (da.isOutOfMemory()) { // simulate OutOfMemoryError
	      return ti.createAndThrowException("java.lang.OutOfMemoryError",
	                                        "trying to allocate new " + cname);
	    }

	    int objRef = da.newObject(ci, ti);

	    // pushes the return value onto the stack
	    ti.push(objRef, true);

	// TODO: to review
	    //insert dummy expressions for StringBuilder and StringBuffer
	    //String className = ci.getName();
	    if(className.equals("java.lang.StringBuilder") || className.equals("java.lang.StringBuffer")){
	    	SymbolicStringBuilder t = new SymbolicStringBuilder();
	    	StackFrame sf = ti.getTopFrame();
	    	sf.setOperandAttr(t);
	    }


	    ss.checkGC(); // has to happen after we push the new object ref

	    return getNext(ti);


  }

}