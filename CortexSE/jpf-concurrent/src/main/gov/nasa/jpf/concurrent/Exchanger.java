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

import gov.nasa.jpf.concurrent.version.ExchangerVersion;
import gov.nasa.jpf.concurrent.version.Version;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.ThreadInfo;
import java.util.Iterator;
import java.util.Map.Entry;

/*
 * This class is the heart of the Exchanger model. 
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 * @author Nathan Reynolds <nathanila@gmail.com>
 *
 */
public class Exchanger extends ThreadModel {

  public static final int SLOT_EMPTY = -2;

  public int exchange(int x, long timeout) {
    int r = MJIEnv.NULL;
    if (!checkNotInterrupted()) {
      unpinObject(getCurrentVersion().removeThreadSlot(thread));
      unpinObject(getCurrentVersion().removeThreadExchange(thread));
      saveVersion();
      return r;
    }
    if(thread.isFirstStepInsn()) {
      if (thread.isTimedOut()) {
        env.throwException("java.util.concurrent.TimeoutException");
        unpinObject(getCurrentVersion().removeThreadSlot(thread));
        unpinObject(getCurrentVersion().removeThreadExchange(thread));
        park(0);
        saveVersion();
        return r;
      }
      checkIfUnblock();
    }
    r = getAndRemoveThreadExchange();
    if(r == SLOT_EMPTY) {
      Entry<ThreadInfo,Integer> e = getAndRemoveWaitingThread2Slot();
      if(e == null) {
        pinObject(x);
        getCurrentVersion().addSlot(thread, x);
        park(timeout);
      } else {
        pinObject(x);
        getCurrentVersion().addThreadExchange(e.getKey(), x);
        r = e.getValue();
        unpark(e.getKey().getThreadObjectRef());
      }
    }
    saveVersion();
    if(r != SLOT_EMPTY && r != MJIEnv.NULL) {
      unpinObject(r);
    }
    return r;
  }

  private Entry<ThreadInfo, Integer> getAndRemoveWaitingThread2Slot() {
    Iterator<Entry<ThreadInfo, Integer>> i = getCurrentVersion().getThread2Slot().entrySet().iterator();
    while (i.hasNext()) {
      Entry<ThreadInfo, Integer> e = i.next();
      if (e.getKey() != thread) {
        getCurrentVersion().removeThreadSlot(e.getKey());
        return e;
      }
    }
    return null;
  }

  private int getAndRemoveThreadExchange() {
    Integer slotValue = getCurrentVersion().getThread2Exchange().get(thread);
    if (slotValue == null) {
      return SLOT_EMPTY;
    }
    getCurrentVersion().removeThreadExchange(thread);
    return slotValue;
  }

  public Version newVersionInstance() {
    return new ExchangerVersion();
  }

  public Version newVersionInstance(Version v) {
    return new ExchangerVersion(v);
  }

  public static Exchanger getExchanger(MJIEnv env, int objRef, int version) {
    Exchanger s = (Exchanger) getModel(env, objRef);
    if (s == null) {
      s = new Exchanger();
      addModel(objRef, s);
    }
    s = (Exchanger) initModel(env, objRef, version, s);
    return s;
  }

  public Exchanger doClone() {
    return (Exchanger) doClone(new Exchanger());
  }

  protected ExchangerVersion getCurrentVersion() {
    return (ExchangerVersion) currentVersion;
  }

  protected void addAndPark(int permits, long timeout) {
    throw new UnsupportedOperationException();
  }

  protected void unpinObject(int obj) {
    if(obj != SLOT_EMPTY) {
      super.unpinObject(obj);
    }
  }
}
