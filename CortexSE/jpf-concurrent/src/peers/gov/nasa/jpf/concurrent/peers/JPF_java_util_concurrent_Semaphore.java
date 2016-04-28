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

import gov.nasa.jpf.concurrent.Semaphore;
import gov.nasa.jpf.jvm.MJIEnv;

/**
 * Peer for java.util.concurrent.Semaphore
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_Semaphore extends ConcurrentPeer{

  public static void newSemaphore__IZ__V(MJIEnv env, int objRef,int permits,boolean fair) {
    Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).newSemaphore(permits,fair);
  }

  public static boolean nativeTryAcquire__IJLjava_util_concurrent_TimeUnit_2__Z(MJIEnv env, int objRef,int permits,long timeout,int units) {
    return Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).tryAcquire(permits,timeout);
  }

  public static void removeThreadFromQueue____V(MJIEnv env, int objRef) {
    Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).removeThreadFromQueue();
  }

  public static boolean tryAcquire__I__Z(MJIEnv env, int objRef,int permits) {
    return Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).tryAcquire(permits);
  }

  public static void release__I__V(MJIEnv env, int objRef,int permits) {
    Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).release(permits);
  }

  public static int availablePermits____I(MJIEnv env, int objRef) {
    return Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).availablePermits();
  }

  public static void reducePermits__I__V(MJIEnv env, int objRef,int reduction) {
    Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).reducePermits(reduction);
  }

  public static boolean isFair____Z(MJIEnv env, int objRef) {
    return Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).isFair();
  }

  public static int getQueueLength____I(MJIEnv env, int objRef) {
    return Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).getQueueLength();
  }

  public static int getQueuedThreads____Ljava_util_Collection_2(MJIEnv env, int objRef) {
    return Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).getQueuedThreads();
  }

  public static boolean nativeAcquireUninterruptibly__I__Z(MJIEnv env, int objRef,int permits) {
    return Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).acquireUninterruptibly(permits);
  }

  public static boolean nativeAcquire__I__Z(MJIEnv env, int objRef,int permits) {
    return Semaphore.getSemaphore(env, objRef, getVersion(env, objRef)).acquire(permits);
  }

}