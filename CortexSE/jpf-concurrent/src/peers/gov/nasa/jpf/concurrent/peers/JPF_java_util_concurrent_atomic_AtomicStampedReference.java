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
 * Peer for java.util.concurrent.atomic.AtomicStampedReference
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */

public class JPF_java_util_concurrent_atomic_AtomicStampedReference {

  public static void $init__Ljava_lang_Object_2I__V(MJIEnv env,int objRef, int initialRef,int initialStamp) {
    int newAtomicRef = env.newObject("java.util.concurrent.atomic.AtomicReference");
    env.setReferenceField(objRef, "atomicRef", newAtomicRef);
    set__Ljava_lang_Object_2I__V(env, objRef, initialRef, initialStamp);
  }

  public static int get___3I__Ljava_lang_Object_2 (MJIEnv env, int objRef,int stampHolder) {
    env.setIntArrayElement(stampHolder, 0, getStamp____I(env, objRef));
    return getReference____Ljava_lang_Object_2(env, objRef);
  }

  public static void set__Ljava_lang_Object_2I__V(MJIEnv env,int objRef,int newReference,int newStamp) {
    int atomicRef = env.getReferenceField(objRef, "atomicRef");
    int oldReference = getReference____Ljava_lang_Object_2(env, objRef);
    int oldStamp = getStamp____I(env, objRef);
    if(oldReference != newReference || oldStamp != newStamp) {
      int newRefIntegerPair = env.newObject("java.util.concurrent.atomic.AtomicStampedReference$ReferenceIntegerPair");
      env.setReferenceField(newRefIntegerPair, "reference", newReference);
      env.setIntField(newRefIntegerPair, "integer", newStamp);
      env.setReferenceField(atomicRef, "value", newRefIntegerPair);
    }
  }

  public static boolean compareAndSet__Ljava_lang_Object_2Ljava_lang_Object_2II__Z (MJIEnv env, int objRef, int expectedReference, int newReference,int expectedStamp,int newStamp){
    int oldReference = getReference____Ljava_lang_Object_2(env, objRef);
    int oldStamp = getStamp____I(env, objRef);
    if(oldReference == expectedReference && oldStamp == expectedStamp) {
      set__Ljava_lang_Object_2I__V(env, objRef, newReference, newStamp);
      return true;
    }
    return false;
  }

  public static boolean weakCompareAndSet__Ljava_lang_Object_2Ljava_lang_Object_2II__Z(MJIEnv env, int objRef, int expectedReference, int newReference,int expectedStamp,int newStamp){
    return compareAndSet__Ljava_lang_Object_2Ljava_lang_Object_2II__Z(env, objRef, expectedReference, newReference, expectedStamp, newStamp);
  }

  public static boolean attemptStamp__Ljava_lang_Object_2I__Z(MJIEnv env,int objRef,int expectedReference,int newStamp) {
    int oldReference = getReference____Ljava_lang_Object_2(env, objRef);
    if(expectedReference == oldReference) {
      set__Ljava_lang_Object_2I__V(env, objRef, expectedReference, newStamp);
      return true;
    }
    return false;
  }
  
  public static int getReference____Ljava_lang_Object_2(MJIEnv env,int objRef) {
    int atomicRef = env.getReferenceField(objRef, "atomicRef");
    int refIntegerPair = env.getReferenceField(atomicRef, "value");
    if(refIntegerPair == MJIEnv.NULL) return MJIEnv.NULL;
    return env.getReferenceField(refIntegerPair, "reference");
  }
  
  public static int getStamp____I(MJIEnv env,int objRef) {
    int atomicRef = env.getReferenceField(objRef, "atomicRef");
    int refIntegerPair = env.getReferenceField(atomicRef, "value");
    if(refIntegerPair == MJIEnv.NULL) return -1;
    return env.getIntField(refIntegerPair, "integer");
  }
}
