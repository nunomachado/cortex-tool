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
package sun.misc;

import gov.nasa.jpf.jvm.MJIEnv;

/**
 * this is just a stub for sun.misc.VM, which we don't really support
 */
public class JPF_sun_misc_VM {
  
  public static void initialize____V(MJIEnv env, int clsObjRef){
    
    // this should happen from java.lang.System initialization, which we model differently
    // since sun.misc.VM isn't fully supported, we defer the action as a last
    // effort to avoid UnsatisfiedLinkErrors, but if the client is really using
    // sun.misc.VM other than calling isBooted() this is not going to help much
    
    // we could override isBooted(), but the 'booted' field value is also used in other
    // methods of sun.misc.VM
    env.setStaticBooleanField(clsObjRef, "booted", true);
  }
}
