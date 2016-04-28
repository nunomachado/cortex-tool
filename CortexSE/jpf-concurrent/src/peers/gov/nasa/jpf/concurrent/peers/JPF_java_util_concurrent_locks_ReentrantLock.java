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

import gov.nasa.jpf.concurrent.ReentrantLock;
import gov.nasa.jpf.jvm.MJIEnv;

/*
 * ReentrantLock native peer.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class JPF_java_util_concurrent_locks_ReentrantLock extends ConcurrentPeer{

  public static boolean nativeLock____Z (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).lock();
  }

  public static void unlock____V (MJIEnv env, int objRef) {
    ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).unlock();
  }

  public static boolean nativeLockInterruptibly____Z (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).acquireInterruptibly();
  }

  public static boolean isFair____Z (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).isFair();
  }

  public static void setFair__Z__V (MJIEnv env, int objRef,boolean f) {
    ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).setFair(f);
  }

  public static boolean isLocked____Z (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).isLocked();
  }

  public static boolean hasQueuedThreads____Z (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).hasQueuedThreads();
  }

  public static int getQueueLength____I (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).getQueueLength();
  }

  public static int getHoldCount____I (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).getHoldCount();
  }

  public static boolean isHeldByCurrentThread____Z (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).isHeldByCurrentThread();
  }

  public static boolean hasQueuedThread__Ljava_lang_Thread_2__Z (MJIEnv env, int objRef, int rThread) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).hasQueuedThread(rThread);
  }

  public static int getQueuedThreads____Ljava_util_Collection_2 (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).getQueuedThreads();
  }

  public static int getOwner____Ljava_lang_Thread_2 (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).getOwner();
  }

  public static void nativeTryLock__JLjava_util_concurrent_TimeUnit_2__V (MJIEnv env, int objRef, long timeout, int unit) {
    ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).tryLock(timeout, unit);
  }

  public static boolean tryLock____Z (MJIEnv env, int objRef) {
    return ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).tryLock();
  }

  public static void nativeRemoveFromQueue____V (MJIEnv env, int objRef) {
    ReentrantLock.getReentrantLock(env, objRef, getVersion(env, objRef)).removeThreadFromQueue();
  }
}
