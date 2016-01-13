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
package gov.nasa.jpf.concurrent.version;

import gov.nasa.jpf.jvm.ThreadInfo;
import java.util.ArrayList;
import java.util.List;

/*
 * This class handles version for all Condition objects.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class ConditionVersion extends ThreadVersion {

  /* Consider code:
   *  t.interrupt();
   *  c.signal();
   *
   * t variable is a thread and c is a condition, let assume that before
   * t.interrupt() thread t invoked c.await() and is waiting for a signal.
   * It may happen that t will wake up after signal with interrupt flag
   * set on, if await() has been invoked then IE will be thrown, but when
   * awaitUninterruptibly() was invoked we will end up in deadlock because
   * interrupt has no effect on this method so the thread will be parked.
   * That's why we need information that thread is in UNBLOCKED state from signal
   * rather than interruption. We also need to preserve information about interruption
   * because after awaitUninterruptibly the interruption flag must be set on.
  */ 


  private List<ThreadInfo> recentlySignalled = new ArrayList<ThreadInfo>();

  public ConditionVersion () {
    super();
  }

  public ConditionVersion (Version version) {
    internalCopy(version);
  }

  public void addRecentlySignalled(ThreadInfo t) {
    recentlySignalled.add(t);
  }

  public void removeRecentlySignalled(ThreadInfo t) {
    recentlySignalled.remove(t);
  }

  public boolean isRecentlySignalled(ThreadInfo t) {
    return recentlySignalled.contains(t);
  }

  public boolean equals (Object o) {
    if(!(o instanceof Version)) return false;
    Version version = (Version)o;
    if(!super.equals(version)) return false;
    ConditionVersion version2 = (ConditionVersion)version;
    if(!recentlySignalled.equals(version2.recentlySignalled)) return false;
    return true;
  }

  public int hashCode() {
    return super.hashCode() + recentlySignalled.hashCode();
  }

  protected void internalCopy (Version version) {
    super.internalCopy(version);
    ConditionVersion version2 = (ConditionVersion)version;
    recentlySignalled = new ArrayList<ThreadInfo>(version2.recentlySignalled);
  }
}
