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
 * Peer for java.util.concurrent.atomic.AtomicReference
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicReference {

  public static void $init__Ljava_lang_Object_2__V(MJIEnv env,int objRef, int initialValue) {
    set__Ljava_lang_Object_2__V(env, objRef, initialValue);
  }

  public static void $init____V(MJIEnv env,int objRef) {
    set__Ljava_lang_Object_2__V(env, objRef, MJIEnv.NULL);
  }
  
  public static int get____Ljava_lang_Object_2 (MJIEnv env, int objRef) {
    return env.getReferenceField(objRef, "value");
  }

  public static void set__Ljava_lang_Object_2__V(MJIEnv env,int objRef,int newValue) {
    env.setReferenceField(objRef, "value",newValue);
  }

  public static void lazySet__Ljava_lang_Object_2__V(MJIEnv env,int objRef,int newValue) {
    set__Ljava_lang_Object_2__V(env, objRef, newValue);
  }

  public static boolean compareAndSet__Ljava_lang_Object_2Ljava_lang_Object_2__Z (MJIEnv env, int objRef, int expect, int update){
    int value = get____Ljava_lang_Object_2(env, objRef);
    if (value == expect){
      set__Ljava_lang_Object_2__V(env, objRef, update);
      return true;
    } else {
      return false;
    }
  }

  public static boolean weakCompareAndSet__Ljava_lang_Object_2Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int expect, int update){
    return compareAndSet__Ljava_lang_Object_2Ljava_lang_Object_2__Z(env, objRef, expect, update);
  }

  public static int getAndSet__Ljava_lang_Object_2__Ljava_lang_Object_2(MJIEnv env,int objRef,int newValue) {
    int oldValue = get____Ljava_lang_Object_2(env, objRef);
    set__Ljava_lang_Object_2__V(env, objRef, newValue);
    return oldValue;
  }
}
