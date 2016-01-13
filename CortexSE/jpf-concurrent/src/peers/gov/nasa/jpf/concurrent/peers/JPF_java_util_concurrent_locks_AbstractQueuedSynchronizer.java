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

import gov.nasa.jpf.concurrent.AQS;
import gov.nasa.jpf.jvm.MJIEnv;


/*
 * AbstractQueuedSynchronizer native peer.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class JPF_java_util_concurrent_locks_AbstractQueuedSynchronizer extends ConcurrentPeer{

  public static int getState____I(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).getState();
  }

  public static void setState__I__V(MJIEnv env, int objRef, int newState) {
    AQS.getAQS(env, objRef, getVersion(env, objRef)).setState(newState);
  }

  public static void queueThread__Z__V(MJIEnv env, int objRef,boolean isShared) {
    AQS.getAQS(env, objRef, getVersion(env, objRef)).queueThread(0,isShared);
  }

  public static boolean queueThreadInterruptibly__JZ__Z(MJIEnv env, int objRef,long timeout,boolean isShared) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).queueThreadInteruptibly(timeout,isShared);
  }

  public static boolean compareAndSetState__II__Z(MJIEnv env, int objRef, int expect, int update) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).compareAndSetState(expect,update);
  }

  public static void dequeueFirstThread____V(MJIEnv env, int objRef) {
    AQS.getAQS(env, objRef, getVersion(env, objRef)).dequeueFirstThread();
  }

  public static boolean hasQueuedThreads____Z(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).hasQueuedThreads();
  }

  public static boolean hasContended____Z(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).hasContended();
  }

  public static int getFirstQueuedThread____Ljava_lang_Thread_2(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).getFirstQueuedThread();
  }

  public static boolean isQueued__Ljava_lang_Thread_2__Z(MJIEnv env, int objRef, int thread) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).isQueued(thread);
  }

  public static int getQueueLength____I(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).getQueueLength();
  }

  public static int getQueuedThreads____Ljava_util_Collection_2(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).getQueuedThreads();
  }

  public static int getExclusiveQueuedThreads____Ljava_util_Collection_2(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).getExclusiveQueuedThreads();
  }

  public static int getSharedQueuedThreads____Ljava_util_Collection_2(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).getSharedQueuedThreads();
  }
  
  public static boolean apparentlyFirstQueuedIsExclusive____Z(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).apparentlyFirstQueuedIsExclusive();
  }

  public static boolean hasQueuedPredecessors____Z(MJIEnv env, int objRef) {
    return AQS.getAQS(env, objRef, getVersion(env, objRef)).hasQueuedPredecessors();
  }

}
