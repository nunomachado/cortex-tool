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
 * Peer for java.util.concurrent.atomic.AtomicInteger
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicInteger extends ConcurrentPeer{

  public static void $init__I__V(MJIEnv env,int objRef, int initialValue) {
    set__I__V(env, objRef, initialValue);
  }

  public static void $init____V(MJIEnv env,int objRef) {
    set__I__V(env, objRef, 0);
  }
  
  public static int get____I(MJIEnv env,int objRef) {
    return env.getIntField(objRef, "value");
  }

  public static void set__I__V(MJIEnv env,int objRef,int newValue) {
    env.setIntField(objRef, "value",newValue);
  }

  public static void lazySet__I__V(MJIEnv env,int objRef,int newValue) {
    set__I__V(env, objRef, newValue);
  }

  public static boolean compareAndSet__II__Z (MJIEnv env, int objRef, int expect, int update){
    int value = get____I(env, objRef);
    if (value == expect){
      set__I__V(env, objRef, update);
      return true;
    } else {
      return false;
    }
  }

  public static boolean weakCompareAndSet__II__Z (MJIEnv env, int objRef, int expect, int update){
    return compareAndSet__II__Z(env, objRef, expect, update);
  }

  public static int getAndSet__I__I(MJIEnv env,int objRef,int newValue) {
    int oldValue = get____I(env, objRef);
    set__I__V(env, objRef, newValue);
    return oldValue;
  }

  public static int getAndAdd__I__I(MJIEnv env,int objRef,int delta) {
    int oldValue = get____I(env, objRef);
    set__I__V(env, objRef, delta+oldValue);
    return oldValue;
  }

  public static int getAndDecrement____I(MJIEnv env,int objRef) {
    return getAndAdd__I__I(env, objRef, -1);
  }

  public static int getAndIncrement____I(MJIEnv env,int objRef) {
    return getAndAdd__I__I(env, objRef, 1);
  }

  public static int addAndGet__I__I(MJIEnv env,int objRef,int delta) {
    int oldValue = get____I(env, objRef);
    set__I__V(env, objRef, oldValue + delta);
    return oldValue + delta;
  }

  public static int decrementAndGet____I(MJIEnv env,int objRef) {
    return addAndGet__I__I(env, objRef, -1);
  }

  public static int incrementAndGet____I(MJIEnv env,int objRef) {
    return addAndGet__I__I(env, objRef, 1);
  }

  public static int toString____Ljava_lang_String_2(MJIEnv env,int objRef) {
    return newString(env, Integer.toString(get____I(env, objRef)));
  }

  public static int intValue____I(MJIEnv env,int objRef) {
    return get____I(env, objRef);
  }

  public static long longValue____J(MJIEnv env,int objRef) {
    return (long)get____I(env, objRef);
  }

  public static float floatValue____F(MJIEnv env,int objRef) {
    return (float)get____I(env, objRef);
  }

  public static double doubleValue____D(MJIEnv env,int objRef) {
    return (double)get____I(env, objRef);
  }

}
