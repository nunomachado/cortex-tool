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

import gov.nasa.jpf.concurrent.version.Version;
import gov.nasa.jpf.concurrent.version.ConcurrentHashMapVersion;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MJIEnv;

import java.util.Iterator;
import java.util.Map;

/*
 * Implements logic for ConcurrentHashMap.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 * @author Nathan Reynolds <nathanila@gmail.com>
 *
 */
public class ConcurrentHashMap extends Model {

  public void newMap() {
    getCurrentVersion().setMap(new java.util.concurrent.ConcurrentHashMap<Integer, Integer>());
    saveVersion();
  }

  public void newMap(int initialCapacity) {
    getCurrentVersion().setMap(new java.util.concurrent.ConcurrentHashMap<Integer, Integer>(initialCapacity));
    saveVersion();
  }

  public void clear() {
    Map map = getCurrentVersion().getMap();
    Iterator<Map.Entry<Integer,Integer>> i = map.entrySet().iterator();
    while(i.hasNext()) {
      Map.Entry<Integer,Integer> entry = i.next();
      unpinObject(entry.getValue());
    }
    getCurrentVersion().getMap().clear();
    saveVersion();
  }

  public boolean contains(int value) {
    return containsValue(value);
  }

  public boolean containsKey(int key) {
    if (checkIsNull(key)) {
      return false;
    }
    return getCurrentVersion().getMap().containsKey(key);
  }

  public boolean containsValue(int value) {
    if (checkIsNull(value)) {
      return false;
    }
    return getCurrentVersion().getMap().containsValue(value);
  }

  public int get(int key) {
    if (checkIsNull(key)) {
      return MJIEnv.NULL;
    }
    Integer r = getCurrentVersion().getMap().get(key);
    if (r == null) {
      return MJIEnv.NULL;
    }
    return r;
  }

  public boolean isEmpty() {
    return getCurrentVersion().getMap().isEmpty();
  }

  public int put(int key, int value) {
    if (checkIsNull(key) || checkIsNull(value)) {
      return MJIEnv.NULL;
    }
    Integer r = getCurrentVersion().getMap().put(key, value);
    pinObject(value);
    saveVersion();
    if (r == null) {
      return MJIEnv.NULL;
    } else {
      unpinObject(r);
    }
    return r;
  }

  public int putIfAbsent(int key, int value) {
    if (checkIsNull(key) || checkIsNull(value)) {
      return MJIEnv.NULL;
    }
    Integer r = get(key);
    if (r == MJIEnv.NULL) {
      put(key,value);
    }
    return r;
  }

  public int remove(int key) {
    if (checkIsNull(key)) {
      return MJIEnv.NULL;
    }
    Integer r = getCurrentVersion().getMap().remove(key);
    saveVersion();
    if (r == null) {
      return MJIEnv.NULL;
    } else {
      unpinObject(r);
    }
    return r;
  }

  public boolean remove(int key, int value) {
    if (checkIsNull(key)) {
      return false;
    }
    boolean r = getCurrentVersion().getMap().remove(key, value);
    if(r) {
      unpinObject(value);
    }
    saveVersion();
    return r;
  }

  public int replace(int key, int value) {
    if (checkIsNull(key) || checkIsNull(value)) {
      return MJIEnv.NULL;
    }
    Integer r = getCurrentVersion().getMap().replace(key, value);
    pinObject(value);
    saveVersion();
    if (r == null) {
      return MJIEnv.NULL;
    } else {
      unpinObject(r);
    }
    return r;
  }

  public boolean replace(int key, int oldValue, int newValue) {
    if (checkIsNull(key) || checkIsNull(oldValue) || checkIsNull(newValue)) {
      return false;
    }
    boolean r = getCurrentVersion().getMap().replace(key, oldValue, newValue);
    if(r) {
      unpinObject(oldValue);
      pinObject(newValue);
    }
    saveVersion();
    return r;
  }

  public int size() {
    return getCurrentVersion().getMap().size();
  }

  public boolean hasNextEntry(int pos) {
    return getCurrentVersion().getMap().entrySet().toArray().length > pos;
  }

  public int nextEntry(int pos) {
    if(pos >= getCurrentVersion().getMap().size()) {
      env.throwException("java.util.NoSuchElementException");
      return MJIEnv.NULL;
    }
    Heap heap = env.getHeap();
    Instruction insn = thread.getPC();
    ClassInfo entryClass = ClassInfo.tryGetResolvedClassInfo("java.util.concurrent.ConcurrentHashMap$WriteThroughEntry");
    if (insn.requiresClinitExecution(thread, entryClass)) {
      env.repeatInvocation();
      return MJIEnv.NULL;
    }
    int rEntry = heap.newObject(entryClass, thread);
    Map.Entry<Integer, Integer> me = (Map.Entry<Integer, Integer>) getCurrentVersion().getMap().entrySet().toArray()[pos];
    env.setReferenceField(rEntry, "this$0", objRef);
    env.setReferenceField(rEntry, "key", me.getKey());
    env.setReferenceField(rEntry, "value", me.getValue());
    return rEntry;
  }

  public void removeEntry(int pos) {
    if(!checkValidState(pos)) return;
    Map.Entry<Integer,Integer> e = (Map.Entry<Integer, Integer>)getCurrentVersion().getMap().entrySet().toArray()[pos-1];
    getCurrentVersion().getMap().remove(e.getKey());
    if(e.getValue() != MJIEnv.NULL) {
      unpinObject(e.getValue());
    }
    saveVersion();
  }

  public ConcurrentHashMap doClone() {
    return (ConcurrentHashMap)doClone(new ConcurrentHashMap());
  }

  public Version newVersionInstance() {
    return new ConcurrentHashMapVersion();
  }

  public Version newVersionInstance(Version v) {
    return new ConcurrentHashMapVersion(v);
  }

  public static ConcurrentHashMap getConcurrentHashMap(MJIEnv env, int objRef, int version) {
    ConcurrentHashMap m = (ConcurrentHashMap) getModel(env, objRef);
    if (m == null) {
      m = new ConcurrentHashMap();
      addModel(objRef, m);
    }
    m = (ConcurrentHashMap) initModel(env, objRef, version, m);
    return m;
  }

  protected ConcurrentHashMapVersion getCurrentVersion() {
    return (ConcurrentHashMapVersion) currentVersion;
  }

  private boolean checkValidState(int pos) {
    if(pos > getCurrentVersion().getMap().size() || pos == 0) {
      env.throwException("java.lang.IllegalStateException");
      return false;
    }
    return true;
  }
}
