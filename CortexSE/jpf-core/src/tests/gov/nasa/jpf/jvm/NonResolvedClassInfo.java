//
// Copyright (C) 2011 United States Government as represented by the
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

import gov.nasa.jpf.classfile.ClassFile;
import gov.nasa.jpf.classfile.ClassFileException;
import gov.nasa.jpf.jvm.bytecode.InstructionFactory;

/**
 * just a helper construct to create ClassInfos that can be used in unit tests
 * (without superclasses, clinit calls and the other bells and whistles)
 */
class NonResolvedClassInfo extends ClassInfo {
  NonResolvedClassInfo (ClassFile cf) throws ClassFileException {
    super(cf);
  }

  protected ClassInfo loadSuperClass (String superName) {
    return null;
  }

  protected CodeBuilder createCodeBuilder(){
    InstructionFactory insnFactory = new InstructionFactory();
    return new CodeBuilder(insnFactory, null, null);
  }

  protected void checkAnnotationDefaultValues(AnnotationInfo ai){
    // nothing - we don't want annotation types to be resolved
  }
}