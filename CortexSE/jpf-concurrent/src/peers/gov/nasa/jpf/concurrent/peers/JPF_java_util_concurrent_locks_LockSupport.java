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

import gov.nasa.jpf.jvm.JPF_sun_misc_Unsafe;
import gov.nasa.jpf.jvm.MJIEnv;

/*
 * LockSupport native peer.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class JPF_java_util_concurrent_locks_LockSupport {

  public static void park__Ljava_lang_Object_2__V(MJIEnv env, int objRef, int blocker) {
    parkNanos__Ljava_lang_Object_2J__V(env, objRef, blocker,0);
  }

  public static void park____V(MJIEnv env, int objRef) {
    parkNanos__J__V(env, objRef, 0);
  }

  public static void unpark__Ljava_lang_Thread_2__V(MJIEnv env, int objRef, int thread) {
    if (thread != MJIEnv.NULL) {
      JPF_sun_misc_Unsafe.unpark__Ljava_lang_Object_2__V(env, -1, thread);
      setBlocker(env, thread, MJIEnv.NULL);
    }
  }

  public static void parkNanos__Ljava_lang_Object_2J__V(MJIEnv env, int objRef, int blocker, long timeout) {
    if(env.getThreadInfo().isTimedOut() || env.getThreadInfo().isInterrupted(false)) {
      setBlocker(env, env.getThreadInfo().getThreadObjectRef(), MJIEnv.NULL);
    } else {
      setBlocker(env, env.getThreadInfo().getThreadObjectRef(), blocker);
    }
    parkNanos__J__V(env, objRef, timeout);
  }

  public static void parkNanos__J__V(MJIEnv env, int objRef, long timeout) {
    JPF_sun_misc_Unsafe.park__ZJ__V(env, objRef, true, timeout);
  }

  public static void parkUntil__Ljava_lang_Object_2J__V(MJIEnv env, int objRef, int blocker, long timeout) {
    parkNanos__Ljava_lang_Object_2J__V(env, objRef, blocker,timeout);
  }

  public static void parkUntil__J__V(MJIEnv env, int objRef, long timeout) {
    parkNanos__J__V(env, objRef, timeout);
  }

  private static void setBlocker(MJIEnv env,int thread, int blocker) {
      env.setReferenceField(thread, "parkBlocker", blocker);
  }

  public static int getBlocker__Ljava_lang_Thread_2__Ljava_lang_Object_2(MJIEnv env, int objRef, int thread) {
    return env.getReferenceField(thread, "parkBlocker");
  }
}
