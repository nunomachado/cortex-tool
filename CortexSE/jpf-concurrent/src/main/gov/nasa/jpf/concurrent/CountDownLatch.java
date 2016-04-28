//
// Copyright (C) 2008 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.concurrent;

import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.ThreadInfo;


/*
 * Implements logic for CountDownLatch.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class CountDownLatch extends Condition {

  public void newCountDownLatch(int count) {
    getCurrentVersion().setState(count);
    saveVersion();
  }
  
  public boolean await(long timeout) {
    if(!thread.isFirstStepInsn()) {
      //this is the case when latch value is zero 0 
      //because all countDowns has been executed before await
      if(getCurrentVersion().getState() == 0) return true;
    }
    boolean b = super.await(timeout);
    return b;
  }

  public void countDown() {
    int s = getCurrentVersion().getState();
    if(s == 0) return;
    getCurrentVersion().setState(s-1);
    if(getCurrentVersion().getState() == 0) {
      if(!super.signalAll()) saveVersion();
    } else {
      saveVersion();
    }
  }

  public long getCount() {
    return getState();
  }

  public CountDownLatch doClone() {
    return (CountDownLatch)doClone(new CountDownLatch());
  }

  public static CountDownLatch getCountDownLatch(MJIEnv env, int objRef, int version) {
    CountDownLatch c = (CountDownLatch)getModel(env,objRef);
    if (c == null) {
      c = new CountDownLatch();
      addModel(objRef, c);
    }
    c = (CountDownLatch)initModel(env, objRef, version, c);
    return c;
  }


}
