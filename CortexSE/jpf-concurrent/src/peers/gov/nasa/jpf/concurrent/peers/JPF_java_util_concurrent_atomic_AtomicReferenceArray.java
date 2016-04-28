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
 * Peer for java.util.concurrent.atomic.AtomicReferenceArray
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicReferenceArray {

  public static void $init__I__V(MJIEnv env, int objRef, int length) {
    int arrRef = env.newObjectArray("java.lang.Object", length);
    env.setReferenceField(objRef,"array", arrRef);
  }

  public static void $init___3Ljava_lang_Object_2__V(MJIEnv env, int objRef,int arrRef) {
    if(checkNPE(env,arrRef)) return;
    $init__I__V(env, objRef, env.getArrayLength(arrRef));
    int newArr = env.getReferenceField(objRef, "array");
    for(int i=0;i<env.getArrayLength(arrRef);i++) {
      env.setReferenceArrayElement(newArr, i, env.getReferenceArrayElement(arrRef, i));
    }
  }

  public static int get__I__Ljava_lang_Object_2 (MJIEnv env, int objRef,int i) {
    if(!checkBounds(env, objRef, i)) return -1;
    int arrRef = env.getReferenceField(objRef, "array");
    return env.getReferenceArrayElement(arrRef, i);
  }

  public static void set__ILjava_lang_Object_2__V(MJIEnv env,int objRef,int i,int newValue) {
    if(!checkBounds(env, objRef, i)) return;
    int arrRef = env.getReferenceField(objRef, "array");
    env.setReferenceArrayElement(arrRef, i, newValue);
  }

  public static void lazySet__ILjava_lang_Object_2__V(MJIEnv env,int objRef,int i,int newValue) {
    if(checkNPE(env,objRef)) return;
    set__ILjava_lang_Object_2__V(env, objRef, i, newValue);
  }

  public static boolean compareAndSet__ILjava_lang_Object_2Ljava_lang_Object_2__Z (MJIEnv env, int objRef, int i,int expect, int update){
     if(checkNPE(env,objRef)) return false;
    int value = get__I__Ljava_lang_Object_2(env, objRef, i);
    if (value == expect){
      set__ILjava_lang_Object_2__V(env, objRef, i, update);
      return true;
    } else {
      return false;
    }
  }

  public static boolean weakCompareAndSet__ILjava_lang_Object_2Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int i,int expect, int update){
    if(checkNPE(env,objRef)) return false;
    return compareAndSet__ILjava_lang_Object_2Ljava_lang_Object_2__Z(env, objRef, i, expect, update);
  }

  public static int getAndSet__ILjava_lang_Object_2__Ljava_lang_Object_2(MJIEnv env,int objRef,int i,int newValue) {
    if(checkNPE(env,objRef)) return -1;
    int oldValue = get__I__Ljava_lang_Object_2(env, objRef, i);
    set__ILjava_lang_Object_2__V(env, objRef, i, newValue);
    return oldValue;
  }

  public static int length____I(MJIEnv env, int objRef) {
    return env.getArrayLength(env.getReferenceField(objRef, "array"));
  }

  private static boolean checkNPE(MJIEnv env,int arrRef) {
    if(arrRef == MJIEnv.NULL) {
      env.throwException("java.lang.NullPointerException");
      return true;
    }
    return false;
  }

  private static boolean checkBounds(MJIEnv env,int objRef, int i) {
    int arrRef = env.getReferenceField(objRef, "array");
    if(i < 0 || i >= env.getArrayLength(arrRef)) {
      env.throwException("java.lang.IndexOutOfBoundsException");
      return false;
    }
    return true;
  }

}
