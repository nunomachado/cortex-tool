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

import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.ThreadInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/*
 * This class handles version for Exchanger objects.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class ExchangerVersion extends ThreadVersion {

  private Map<ThreadInfo,Integer> thread2Slot = new HashMap<ThreadInfo,Integer>();
  private Map<ThreadInfo,Integer> thread2Exchange = new HashMap<ThreadInfo,Integer>();

  public ExchangerVersion () {
    super();
  }

  public void addSlot(ThreadInfo thread,int slotValue) {
    thread2Slot.put(thread,slotValue);
  }

  public void addThreadExchange(ThreadInfo thread,int slotValue) {
    thread2Exchange.put(thread,slotValue);
  }

  public Map<ThreadInfo,Integer> getThread2Slot() {
    return Collections.unmodifiableMap(thread2Slot);
  }

  public Map<ThreadInfo,Integer> getThread2Exchange() {
    return Collections.unmodifiableMap(thread2Exchange);
  }

  public ThreadInfo getWaitingThreadSlot() {
    Iterator<Entry<ThreadInfo,Integer>> i = thread2Slot.entrySet().iterator();
    while(i.hasNext()) {
      return i.next().getKey();
    }
    return null;
  }

  public int removeThreadExchange(ThreadInfo thread) {
    Integer r = thread2Exchange.remove(thread);
    if(r == null) {
      return MJIEnv.NULL;
    } else {
      return r;
    }
  }

  public int removeThreadSlot(ThreadInfo thread) {
    Integer r = thread2Slot.remove(thread);
    if(r == null) {
      return MJIEnv.NULL;
    } else {
      return r;
    }
  }

  public ExchangerVersion (Version version) {
    internalCopy(version);
  }

  public boolean equals (Object o) {
    if(!(o instanceof Version)) return false;
    Version version = (Version)o;
    if(!super.equals(version)) return false;
    ExchangerVersion version2 = (ExchangerVersion)version;
    if(!thread2Slot.equals(version2.thread2Slot)) return false;
    if(!thread2Exchange.equals(version2.thread2Exchange)) return false;
    return true;
  }

  public int hashCode() {
    return super.hashCode() + thread2Slot.hashCode() + thread2Exchange.hashCode();
  }

  protected void internalCopy (Version version) {
    super.internalCopy(version);
    ExchangerVersion version2 = (ExchangerVersion)version;
    thread2Slot = new HashMap(version2.thread2Slot);
    thread2Exchange = new HashMap(version2.thread2Exchange);
  }
}
