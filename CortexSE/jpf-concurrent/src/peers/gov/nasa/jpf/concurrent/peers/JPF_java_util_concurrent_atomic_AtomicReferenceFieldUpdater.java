//
//Copyright (C) 2009 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.
//
//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.
//
//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.concurrent.peers;

import gov.nasa.jpf.jvm.MJIEnv;

/**
 * Peer for java.util.concurrent.atomic.AtomicReferenceFieldUpdater
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicReferenceFieldUpdater {

  public static int getAndSet__Ljava_lang_Object_2Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int objRef,int obj,int newValue) {
    int oldValue = JPF_java_util_concurrent_atomic_AtomicReferenceFieldUpdater$AtomicReferenceFieldUpdaterImpl.get__Ljava_lang_Object_2__Ljava_lang_Object_2(env, objRef, obj);
    JPF_java_util_concurrent_atomic_AtomicReferenceFieldUpdater$AtomicReferenceFieldUpdaterImpl.set__Ljava_lang_Object_2Ljava_lang_Object_2__V(env, objRef, obj, newValue);
    return oldValue;
  }


}
