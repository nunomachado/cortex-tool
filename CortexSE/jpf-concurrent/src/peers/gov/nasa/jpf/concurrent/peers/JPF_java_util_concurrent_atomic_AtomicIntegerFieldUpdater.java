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
 * Peer for java.util.concurrent.atomic.AtomicIntegerFieldUpdater.AtomicIntegerFieldUpdaterImpl
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicIntegerFieldUpdater extends ConcurrentPeer {

  public static int getAndSet__Ljava_lang_Object_2I__I(MJIEnv env, int objRef, int obj, int newValue) {
    int current = JPF_java_util_concurrent_atomic_AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl.get__Ljava_lang_Object_2__I(env, objRef, obj);
    JPF_java_util_concurrent_atomic_AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl.compareAndSet__Ljava_lang_Object_2II__Z(env, objRef, obj, current, newValue);
    return current;
  }

  public static int getAndIncrement__Ljava_lang_Object_2__I(MJIEnv env, int objRef, int obj) {
    return getAndAdd__Ljava_lang_Object_2I__I(env, objRef, obj, 1);
  }

  public static int getAndDecrement__Ljava_lang_Object_2__I(MJIEnv env, int objRef, int obj) {
    return getAndAdd__Ljava_lang_Object_2I__I(env, objRef, obj, -1);
  }

  public static int getAndAdd__Ljava_lang_Object_2I__I(MJIEnv env, int objRef, int obj, int delta) {
    int current = JPF_java_util_concurrent_atomic_AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl.get__Ljava_lang_Object_2__I(env, objRef, obj);
    int next = current + delta;
    getAndSet__Ljava_lang_Object_2I__I(env, objRef, obj, next);
    return current;
  }

  public static int incrementAndGet__Ljava_lang_Object_2__I(MJIEnv env, int objRef, int obj) {
    return addAndGet__Ljava_lang_Object_2I__I(env, objRef, obj, 1);
  }

  public static int decrementAndGet__Ljava_lang_Object_2__I(MJIEnv env, int objRef, int obj) {
    return addAndGet__Ljava_lang_Object_2I__I(env, objRef, obj, -1);
  }

  public static int addAndGet__Ljava_lang_Object_2I__I(MJIEnv env, int objRef, int obj, int delta) {
    int current = JPF_java_util_concurrent_atomic_AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl.get__Ljava_lang_Object_2__I(env, objRef, obj);
    int next = current + delta;
    getAndSet__Ljava_lang_Object_2I__I(env, objRef, obj, next);
    return next;
  }
}
