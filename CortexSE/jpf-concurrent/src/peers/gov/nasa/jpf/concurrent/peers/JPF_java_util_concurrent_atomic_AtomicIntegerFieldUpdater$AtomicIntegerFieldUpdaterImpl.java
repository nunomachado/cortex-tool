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

import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.MJIEnv;

/**
 * Peer for java.util.concurrent.atomic.AtomicIntegerFieldUpdater.AtomicIntegerFieldUpdaterImpl
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_atomic_AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl {

  public static void $init__Ljava_lang_Class_2Ljava_lang_String_2__V(MJIEnv env,int objRef, int tClass,int fieldNameRef) {
    ClassInfo ci = env.getReferredClassInfo(tClass);
    String fieldName = env.getStringObject(fieldNameRef);
    FieldInfo field = ci.getDeclaredInstanceField(fieldName);
    if(field == null) {
      env.throwException("java.lang.RuntimeException");
      return;
    }
    if(!field.getTypeClassInfo().equals(ClassInfo.getResolvedClassInfo("int"))) {
      env.throwException("java.lang.IllegalArgumentException", "Must be integer type");
      return;
    }
    if(!field.isVolatile()) {
      env.throwException("java.lang.IllegalArgumentException", "Must be volatile type");
      return;
    }
    env.setLongField(objRef, "offset", fieldNameRef);
  }

  public static int get__Ljava_lang_Object_2__I(MJIEnv env,int objRef,int obj) {
    String fieldName = env.getStringObject((int)env.getLongField(objRef, "offset"));
    return env.getIntField(obj, fieldName);
  }

  public static void set__Ljava_lang_Object_2I__V(MJIEnv env,int objRef,int obj,int newValue) {
    String fieldName = env.getStringObject((int)env.getLongField(objRef, "offset"));
    env.setIntField(obj, fieldName,newValue);
  }

  public static void lazySet__Ljava_lang_Object_2I__V(MJIEnv env,int objRef,int obj,int newValue) {
    set__Ljava_lang_Object_2I__V(env, objRef, obj, newValue);
  }

  public static boolean compareAndSet__Ljava_lang_Object_2II__Z(MJIEnv env,int objRef,int obj,int expect, int update) {
    int value = get__Ljava_lang_Object_2__I(env, objRef, obj);
    if (value == expect){
      set__Ljava_lang_Object_2I__V(env, objRef, obj, update);
      return true;
    } else {
      return false;
    }
  }

  public static boolean weakCompareAndSet__Ljava_lang_Object_2II__Z(MJIEnv env,int objRef,int obj,int expect, int update) {
    return compareAndSet__Ljava_lang_Object_2II__Z(env, objRef, obj, expect, update);
  }

}
