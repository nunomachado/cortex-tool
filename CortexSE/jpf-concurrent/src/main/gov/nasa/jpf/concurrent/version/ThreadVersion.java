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
import java.util.Collections;
import java.util.List;

/*
 * Base for model classes that deal with threads on non-MJI level
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class ThreadVersion extends Version{

  protected ThreadInfo lastRemoved = null;

  protected List<ThreadInfo> queuedThreads = new ArrayList<ThreadInfo>();

  public ThreadVersion () {
    super();
  }

  public ThreadVersion (Version version) {
    internalCopy(version);
  }

  public List<ThreadInfo> getQueuedThreads() {
    return Collections.unmodifiableList(new ArrayList(queuedThreads));
  }

  public void addThreadToQueue(ThreadInfo t) {
    queuedThreads.remove(t);
    queuedThreads.add(t);
  }

  public void removeThreadFromQueue(ThreadInfo t) {
    queuedThreads.remove(t);
  }

  public ThreadInfo getLastRemoved () {
    return lastRemoved;
  }

  public void setLastRemoved (ThreadInfo lastRemoved) {
    this.lastRemoved = lastRemoved;
  }

  public boolean equals (Object o) {
    if(!(o instanceof Version)) return false;
    Version version = (Version)o;
    if(!super.equals(version)) return false;
    ThreadVersion version2 = (ThreadVersion)version;
    List<ThreadInfo> queuedThreads2 = version2.queuedThreads;
    if (queuedThreads.size() != queuedThreads2.size()) return false;
    for (int i = 0; i < queuedThreads.size(); i++) {
      if (!queuedThreads.get(i).equals(queuedThreads2.get(i))) return false;
    }
    if(lastRemoved != version2.getLastRemoved()) return false;
    return true;
  }

  public int hashCode() {
    return super.hashCode() + queuedThreads.hashCode() + lastRemoved.hashCode();
  }

  protected void internalCopy (Version version) {
    super.internalCopy(version);
    ThreadVersion version2 = (ThreadVersion)version;
    queuedThreads = new ArrayList<ThreadInfo>(version2.queuedThreads);
    lastRemoved = version2.getLastRemoved();
  }
}
