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
 * Peer for java.util.concurrent.atomic.AtomicBoolean
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicBoolean extends ConcurrentPeer{

  public static void $init__Z__V(MJIEnv env,int objRef, boolean initialValue) {
    set__Z__V(env, objRef, initialValue);
  }

  public static void $init____V(MJIEnv env,int objRef) {
    set__Z__V(env, objRef, false);
  }
  
  public static boolean get____Z(MJIEnv env,int objRef) {
    return env.getBooleanField(objRef, "value");
  }

  public static void set__Z__V(MJIEnv env,int objRef,boolean newValue) {
    env.setBooleanField(objRef, "value",newValue);
  }

  public static void lazySet__Z__V(MJIEnv env,int objRef,boolean newValue) {
    set__Z__V(env, objRef, newValue);
  }

  public static boolean compareAndSet__ZZ__Z (MJIEnv env, int objRef, boolean expect, boolean update){
    boolean value = get____Z(env, objRef);
    if (value == expect){
      set__Z__V(env, objRef, update);
      return true;
    } else {
      return false;
    }
  }

  public static boolean weakCompareAndSet__ZZ__Z (MJIEnv env, int objRef, boolean expect, boolean update){
    return compareAndSet__ZZ__Z(env, objRef, expect, update);
  }

  public static boolean getAndSet__Z__Z(MJIEnv env,int objRef,boolean newValue) {
    boolean oldValue = get____Z(env, objRef);
    set__Z__V(env, objRef, newValue);
    return oldValue;
  }

  public static int toString____Ljava_lang_String_2(MJIEnv env,int objRef) {
    return newString(env, Boolean.toString(get____Z(env, objRef)));
  }
}
