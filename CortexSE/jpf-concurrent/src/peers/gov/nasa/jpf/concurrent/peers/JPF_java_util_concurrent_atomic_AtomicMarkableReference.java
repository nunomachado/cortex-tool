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
 * Peer for java.util.concurrent.atomic.AtomicMarkableReference
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicMarkableReference {

  public static void $init__Ljava_lang_Object_2Z__V(MJIEnv env, int objRef, int initialRef, boolean initialMark) {
    int newAtomicRef = env.newObject("java.util.concurrent.atomic.AtomicReference");
    env.setReferenceField(objRef, "atomicRef", newAtomicRef);
    set__Ljava_lang_Object_2Z__V(env, objRef, initialRef, initialMark);
  }

  public static int get___3Z__Ljava_lang_Object_2(MJIEnv env, int objRef, int markHolder) {
    env.setBooleanArrayElement(markHolder, 0, isMarked____Z(env, objRef));
    return getReference____Ljava_lang_Object_2(env, objRef);
  }

  public static void set__Ljava_lang_Object_2Z__V(MJIEnv env, int objRef, int newReference, boolean newMark) {
    int atomicRef = env.getReferenceField(objRef, "atomicRef");
    int oldReference = getReference____Ljava_lang_Object_2(env, objRef);
    boolean oldMark = isMarked____Z(env, objRef);
    if (oldReference != newReference || oldMark != newMark) {
      int newRefIntegerPair = env.newObject("java.util.concurrent.atomic.AtomicMarkableReference$ReferenceBooleanPair");
      env.setReferenceField(newRefIntegerPair, "reference", newReference);
      env.setBooleanField(newRefIntegerPair, "bit", newMark);
      env.setReferenceField(atomicRef, "value", newRefIntegerPair);
    }
  }
  
  public static boolean compareAndSet__Ljava_lang_Object_2Ljava_lang_Object_2ZZ__Z(MJIEnv env, int objRef, int expectedReference, int newReference, boolean expectedMark, boolean newMark) {
    int oldReference = getReference____Ljava_lang_Object_2(env, objRef);
    boolean oldMark = isMarked____Z(env, objRef);
    if (oldReference == expectedReference && oldMark == expectedMark) {
      set__Ljava_lang_Object_2Z__V(env, objRef, newReference, newMark);
      return true;
    }
    return false;
  }

  public static boolean weakCompareAndSet__Ljava_lang_Object_2Ljava_lang_Object_2ZZ__Z(MJIEnv env, int objRef, int expectedReference, int newReference, boolean expectedMark,boolean newMark) {
    return compareAndSet__Ljava_lang_Object_2Ljava_lang_Object_2ZZ__Z(env, objRef, expectedReference, newReference, expectedMark, newMark);
  }

  public static boolean attemptMark__Ljava_lang_Object_2Z__Z(MJIEnv env, int objRef, int expectedReference, boolean newMark) {
    int oldReference = getReference____Ljava_lang_Object_2(env, objRef);
    if (expectedReference == oldReference) {
      set__Ljava_lang_Object_2Z__V(env, objRef, expectedReference, newMark);
      return true;
    }
    return false;
  }

  public static int getReference____Ljava_lang_Object_2(MJIEnv env, int objRef) {
    int atomicRef = env.getReferenceField(objRef, "atomicRef");
    int refIntegerPair = env.getReferenceField(atomicRef, "value");
    if (refIntegerPair == MJIEnv.NULL) {
      return MJIEnv.NULL;
    }
    return env.getReferenceField(refIntegerPair, "reference");
  }

  public static boolean isMarked____Z(MJIEnv env, int objRef) {
    int atomicRef = env.getReferenceField(objRef, "atomicRef");
    int refIntegerPair = env.getReferenceField(atomicRef, "value");
    if (refIntegerPair == MJIEnv.NULL) {
      return false;
    }
    return env.getBooleanField(refIntegerPair, "bit");
  }
}
