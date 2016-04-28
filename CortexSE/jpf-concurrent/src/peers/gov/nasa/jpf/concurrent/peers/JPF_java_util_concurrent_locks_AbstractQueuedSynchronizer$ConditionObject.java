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

import gov.nasa.jpf.concurrent.Condition;
import gov.nasa.jpf.jvm.MJIEnv;

/*
 * AbstractQueuedSynchronizer$ConditionObject native peer.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class JPF_java_util_concurrent_locks_AbstractQueuedSynchronizer$ConditionObject extends ConcurrentPeer{

  public static void $init__Ljava_util_concurrent_locks_AbstractQueuedSynchronizer_2__V(MJIEnv env,int objRef,int outer) {
    env.setReferenceField(objRef,"this$0", outer);
  }

  public static boolean nativeAwait__J__Z (MJIEnv env, int objRef,long timeout) {
    return Condition.getCondition(env, objRef, getVersion(env, objRef)).await(timeout);
  }

  public static boolean nativeAwaitUninterruptibly____Z (MJIEnv env, int objRef) {
    return Condition.getCondition(env, objRef, getVersion(env, objRef)).awaitUninterruptibly();
  }

  public static void nativeSignal____V (MJIEnv env, int objRef) {
    Condition.getCondition(env, objRef, getVersion(env, objRef)).signal();
  }

  public static void nativeSignalAll____V (MJIEnv env, int objRef) {
    Condition.getCondition(env, objRef, getVersion(env, objRef)).signalAll();
  }

  public static int nativeGetWaitQueueLength____I (MJIEnv env, int objRef) {
    return Condition.getCondition(env, objRef, getVersion(env, objRef)).getWaitQueueLength();
  }

  public static int nativeGetWaitingThreads____Ljava_util_Collection_2 (MJIEnv env, int objRef) {
    return Condition.getCondition(env, objRef, getVersion(env, objRef)).getWaitingThreads();
  }

  public static boolean nativeHasWaiters____Z (MJIEnv env, int objRef) {
    return Condition.getCondition(env, objRef, getVersion(env, objRef)).hasWaiters();
  }

}
