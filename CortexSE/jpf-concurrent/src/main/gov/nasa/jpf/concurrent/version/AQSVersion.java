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
 * This class handles version for AQS objects.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class AQSVersion extends ThreadVersion {

  private boolean everBlocked = false;

  private Map<ThreadInfo,Boolean> sharedStatus = new HashMap<ThreadInfo,Boolean>();

  public AQSVersion () {
    super();
  }

  public AQSVersion (Version version) {
    internalCopy(version);
  }

  public boolean getEverBlocked() {
    return everBlocked;
  }

  public void setEverBlocked(boolean b) {
    everBlocked = b;
  }

  public boolean isShared(ThreadInfo t) {
    return sharedStatus.get(t);
  }

  public void addThreadToQueue(ThreadInfo t,boolean isShared) {
    super.addThreadToQueue(t);
    sharedStatus.put(t,isShared);

  }

  public void removeThreadFromQueue(ThreadInfo t) {
    super.removeThreadFromQueue(t);
    sharedStatus.remove(t);
  }

  private Map<ThreadInfo,Boolean> getSharedStatus() {
    return new HashMap(sharedStatus);
  }

  public boolean equals (Object o) {
    if(!(o instanceof Version)) return false;
    Version version = (Version)o;
    if(!super.equals(version)) return false;
    AQSVersion version2 = (AQSVersion)version;
    if(everBlocked != version2.getEverBlocked()) return false;
    if(!sharedStatus.equals(version2.sharedStatus)) return false;
    return true;
  }

  public int hashCode() {
    return super.hashCode() + (everBlocked ? 1 : 0) + sharedStatus.hashCode();
  }

  protected void internalCopy (Version version) {
    super.internalCopy(version);
    AQSVersion version2 = (AQSVersion)version;
    everBlocked = version2.getEverBlocked();
    sharedStatus = new HashMap<ThreadInfo,Boolean>(version2.sharedStatus);
  }

}
