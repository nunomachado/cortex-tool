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
import java.util.Arrays;

/**
 * Peer for java.util.concurrent.atomic.AtomicLongArray
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicLongArray extends ConcurrentPeer{

  public static void $init__I__V(MJIEnv env, int objRef, int length) {
    int arrRef = env.newLongArray(length);
    env.setReferenceField(objRef,"array", arrRef);
  }

  public static void $init___3J__V(MJIEnv env, int objRef,int arrRef) {
    if(checkNPE(env,arrRef)) return;
    $init__I__V(env, objRef, env.getArrayLength(arrRef));
    int newArr = env.getReferenceField(objRef, "array");
    for(int i=0;i<env.getArrayLength(arrRef);i++) {
      env.setLongArrayElement(newArr, i, env.getLongArrayElement(arrRef, i));
    }
  }

  public static long get__I__J(MJIEnv env, int objRef, int i) {
    if(!checkBounds(env, objRef, i)) return -1;
    int arrRef = env.getReferenceField(objRef, "array");
    return env.getLongArrayElement(arrRef, i);
  }

  public static void set__IJ__V(MJIEnv env, int objRef, int i, long newValue) {
    if(!checkBounds(env, objRef, i)) return;
    int arrRef = env.getReferenceField(objRef, "array");
    env.setLongArrayElement(arrRef, i, newValue);
  }

  public static void lazySet__IJ__V(MJIEnv env, int objRef, int i, long newValue) {
    if(!checkBounds(env, objRef, i)) return;
    set__IJ__V(env, objRef, i, newValue);
  }

  public static boolean compareAndSet__IJJ__Z(MJIEnv env, int objRef,int i, long expect, long update) {
    if(!checkBounds(env, objRef, i)) return false;
    long value = get__I__J(env, objRef,i);
    if (value == expect) {
      set__IJ__V(env, objRef,i, update);
      return true;
    } else {
      return false;
    }
  }

  public static boolean weakCompareAndSet__IJJ__Z(MJIEnv env, int objRef,int i, long expect, long update) {
    if(!checkBounds(env, objRef, i)) return false;
    return compareAndSet__IJJ__Z(env, objRef,i, expect, update);
  }

  public static long getAndSet__IJ__J(MJIEnv env, int objRef,int i, long newValue) {
    if(!checkBounds(env, objRef, i)) return -1;
    long oldValue = get__I__J(env, objRef,i);
    set__IJ__V(env, objRef,i, newValue);
    return oldValue;
  }

  public static long getAndAdd__IJ__J(MJIEnv env, int objRef,int i, long delta) {
    if(!checkBounds(env, objRef, i)) return -1;
    long oldValue = get__I__J(env, objRef,i);
    set__IJ__V(env, objRef,i, delta + oldValue);
    return oldValue;
  }

  public static long getAndDecrement__I__J(MJIEnv env, int objRef,int i) {
    if(!checkBounds(env, objRef, i)) return -1;
    return getAndAdd__IJ__J(env, objRef,i, -1);
  }

  public static long getAndIncrement__I__J(MJIEnv env, int objRef,int i) {
    if(!checkBounds(env, objRef, i)) return -1;
    return getAndAdd__IJ__J(env, objRef,i, 1);
  }

  public static long addAndGet__IJ__J(MJIEnv env, int objRef,int i, long delta) {
    if(!checkBounds(env, objRef, i)) return -1;
    long oldValue = get__I__J(env, objRef,i);
    set__IJ__V(env, objRef,i, oldValue + delta);
    return oldValue + delta;
  }

  public static long decrementAndGet__I__J(MJIEnv env, int objRef,int i) {
    if(!checkBounds(env, objRef, i)) return -1;
    return addAndGet__IJ__J(env, objRef, i, -1);
  }

  public static long incrementAndGet__I__J(MJIEnv env, int objRef,int i) {
    if(!checkBounds(env, objRef, i)) return -1;
    return addAndGet__IJ__J(env, objRef, i, 1);
  }

  public static int length____I(MJIEnv env, int objRef) {
    return env.getArrayLength(env.getReferenceField(objRef, "array"));
  }

  public static int toString____Ljava_lang_String_2(MJIEnv env,int objRef) {
    return newString(env, Arrays.toString(env.getLongArrayObject(env.getReferenceField(objRef, "array"))));
  }

  private static boolean checkBounds(MJIEnv env,int objRef, int i) {
    int arrRef = env.getReferenceField(objRef, "array");
    if(i < 0 || i >= env.getArrayLength(arrRef)) {
      env.throwException("java.lang.IndexOutOfBoundsException");
      return false;
    } 
    return true;
  }

  private static boolean checkNPE(MJIEnv env,int arrRef) {
    if(arrRef == MJIEnv.NULL) {
      env.throwException("java.lang.NullPointerException");
      return true;
    }
    return false;
  }
}
