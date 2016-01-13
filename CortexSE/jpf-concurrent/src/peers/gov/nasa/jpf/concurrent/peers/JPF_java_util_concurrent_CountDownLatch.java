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
import gov.nasa.jpf.concurrent.CountDownLatch;

/**
 * Peer for java.util.concurrent.CountDownLatch
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_CountDownLatch extends ConcurrentPeer{

  public static void newCountDownLatch__I__V(MJIEnv env, int objRef, int count) {
    CountDownLatch.getCountDownLatch(env, objRef, getVersion(env, objRef)).newCountDownLatch(count);
  }

  public static void await____V(MJIEnv env, int objRef) {
    CountDownLatch.getCountDownLatch(env, objRef, getVersion(env, objRef)).await(0);
  }

  public static boolean await__JLjava_util_concurrent_TimeUnit_2__Z(MJIEnv env, int objRef,long timeout, int unit) {
    return CountDownLatch.getCountDownLatch(env, objRef, getVersion(env, objRef)).await(timeout);
  }

  public static void countDown____V(MJIEnv env, int objRef) {
    CountDownLatch.getCountDownLatch(env, objRef, getVersion(env, objRef)).countDown();
  }

  public static long getCount____J(MJIEnv env, int objRef) {
    return CountDownLatch.getCountDownLatch(env, objRef, getVersion(env, objRef)).getCount();
  }

}