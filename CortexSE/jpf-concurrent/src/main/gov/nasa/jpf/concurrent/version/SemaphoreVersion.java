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
import java.util.HashMap;
import java.util.Map;

/*
 * This class handles version for Semaphore objects.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class SemaphoreVersion extends FairnessVersion {

  private Map<ThreadInfo,Integer> thread2Permit = new HashMap<ThreadInfo,Integer>();

  public SemaphoreVersion () {
    super();
  }

  public SemaphoreVersion (Version version) {
    internalCopy(version);
  }

  public void removeThreadFromQueue(ThreadInfo t) {
    super.removeThreadFromQueue(t);
    thread2Permit.remove(t);
  }

  public void addThreadToQueue(ThreadInfo thread,int permit) {
    super.addThreadToQueue(thread);
    thread2Permit.put(thread, permit);
  }

  public Integer getPermitByThread(ThreadInfo thread) {
    return thread2Permit.get(thread);
  }

  private Map<ThreadInfo,Integer> getThread2Permit() {
    return thread2Permit;
  }

  public boolean equals (Object o) {
    if(!(o instanceof Version)) return false;
    Version version = (Version)o;
    if(!super.equals(version)) return false;
    SemaphoreVersion version2 = (SemaphoreVersion)version;
    if(!thread2Permit.equals(version2.getThread2Permit())) return false;
    return true;
  }

  public int hashCode() {
    return super.hashCode() + thread2Permit.hashCode();
  }

  protected void internalCopy (Version version) {
    super.internalCopy(version);
    SemaphoreVersion version2 = (SemaphoreVersion)version;
    thread2Permit = new HashMap(version2.getThread2Permit());
  }
}
