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
 * Peer for java.util.concurrent.atomic.AtomicLong
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicLong extends ConcurrentPeer{

  public static void $init__J__V(MJIEnv env,int objRef, long initialValue) {
    set__J__V(env, objRef, initialValue);
  }

  public static void $init____V(MJIEnv env,int objRef) {
    set__J__V(env, objRef, 0);
  }
  
  public static long get____J(MJIEnv env,int objRef) {
    return env.getLongField(objRef, "value");
  }

  public static void set__J__V(MJIEnv env,int objRef,long newValue) {
    env.setLongField(objRef, "value",newValue);
  }

  public static void lazySet__J__V(MJIEnv env,int objRef,long newValue) {
    set__J__V(env, objRef, newValue);
  }

  public static boolean compareAndSet__JJ__Z (MJIEnv env, int objRef, long expect, long update){
    long value = get____J(env, objRef);
    if (value == expect){
      set__J__V(env, objRef, update);
      return true;
    } else {
      return false;
    }
  }

  public static boolean weakCompareAndSet__JJ__Z (MJIEnv env, int objRef, long expect, long update){
    return compareAndSet__JJ__Z(env, objRef, expect, update);
  }

  public static long getAndSet__J__J(MJIEnv env,int objRef,long newValue) {
    long oldValue = get____J(env, objRef);
    set__J__V(env, objRef, newValue);
    return oldValue;
  }

  public static long getAndAdd__J__J(MJIEnv env,int objRef,long delta) {
    long oldValue = get____J(env, objRef);
    set__J__V(env, objRef, delta+oldValue);
    return oldValue;
  }

  public static long getAndDecrement____J(MJIEnv env,int objRef) {
    return getAndAdd__J__J(env, objRef, -1);
  }

  public static long getAndIncrement____J(MJIEnv env,int objRef) {
    return getAndAdd__J__J(env, objRef, 1);
  }

  public static long addAndGet__J__J(MJIEnv env,int objRef,long delta) {
    long oldValue = get____J(env, objRef);
    set__J__V(env, objRef, oldValue + delta);
    return oldValue + delta;
  }

  public static long decrementAndGet____J(MJIEnv env,int objRef) {
    return addAndGet__J__J(env, objRef, -1);
  }

  public static long incrementAndGet____J(MJIEnv env,int objRef) {
    return addAndGet__J__J(env, objRef, 1);
  }

  public static int toString____Ljava_lang_String_2(MJIEnv env,int objRef) {
    return newString(env, Long.toString(get____J(env, objRef)));
  }

  public static int intValue____I(MJIEnv env,int objRef) {
    return (int)get____J(env, objRef);
  }

  public static long longValue____J(MJIEnv env,int objRef) {
    return get____J(env, objRef);
  }

  public static float floatValue____F(MJIEnv env,int objRef) {
    return (float)get____J(env, objRef);
  }

  public static double doubleValue____D(MJIEnv env,int objRef) {
    return (double)get____J(env, objRef);
  }
  
}
