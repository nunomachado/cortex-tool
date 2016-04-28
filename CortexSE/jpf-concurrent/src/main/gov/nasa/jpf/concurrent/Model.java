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

import gov.nasa.jpf.concurrent.version.VersionManager;
import gov.nasa.jpf.concurrent.version.Version;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.ThreadInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/*
 * Base for all classes that models java.util.concurrent.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public abstract class Model {

  /*
   * All methods in Model and it's subclasses follows given pattern.
   *
   * Method can only be public if it's used by all it's subclasses
   * If a method is public it must properly handle versions (needs to call saveVersion())
   * Public method cannot be called from other public methods within the same classes
   * (we will execute saveVersion() many times and create bogus states).
   * All helpers methods must be protected or private and they can't
   * call saveVersion()
   *
   */

  /*
   * Thread that executes code in non-MJI level, the same as
   * Thread.currentThread();
   */

  protected ThreadInfo thread;

  protected MJIEnv env;

  protected int objRef;

  protected VersionManager versionManager = new VersionManager();

  protected Version currentVersion = (Version) versionManager.getVersion(0, this);

  public void setThread (ThreadInfo threadInfo) {
    thread = threadInfo;
  }

  public void setEnv (MJIEnv env) {
    this.env = env;
  }

  public void setRef (int objRef) {
    this.objRef = objRef;
  }

  public abstract Version newVersionInstance ();

  public abstract Version newVersionInstance (Version v);

  /*
   * This method sets Sync object into given version. Version is represented by
   * three variables queuedThreads,owner,acquired so for each variable we need
   * to retrieve value from appropriate map
   */

  protected void restoreVersion (int version) {
    currentVersion = versionManager.getVersion(version, this);
  }

  /*
   * Saving version method need to handle two situation: given state has been
   * seen before and we don't save any variables but just set version to
   * appropriate number state hasn't been seen before and we save all three
   * variables to appropriate maps
   */

  protected void saveVersion () {
    setVersion(versionManager.saveVersion(currentVersion).getId());
  }

  /*
   * Creates queue in current thread heap.
   */
  
  private static Map<Integer, Model> referenceToModel = new HashMap<Integer, Model>();

  public static Model getModel (MJIEnv env, int objRef) {
    return referenceToModel.get(objRef);
  }

  public static Model removeModel (MJIEnv env, int objRef) {
    return referenceToModel.remove(objRef);
  }

  public static void addModel(int objRef,Model m) {
    referenceToModel.put(objRef, m);
  }

  public static Model initModel(MJIEnv env,int objRef,int version,Model m) {
    m.setThread((ThreadInfo) env.getThreadInfo());
    m.setEnv(env);
    m.setRef(objRef);
    m.restoreVersion(version);
    return m;
  }

  public int getState() {
    return getCurrentVersion().getState();
  }

  public void setState(int newState) {
    getCurrentVersion().setState(newState);
    saveVersion();
  }

  public Model doClone() {
    throw new UnsupportedOperationException();
  }

  protected Model doClone(Model m) {
    m.versionManager = this.versionManager.doClone(m);
    m.currentVersion = m.versionManager.getVersion(getCurrentVersion().getId(), m);
    m.env = env;
    m.objRef = objRef;
    m.thread = thread;
    return m;
  }

  public boolean equals(Object o) {
    if(o == null) return false;
    if(!(o instanceof Model)) return false;
    Model m = (Model)o;
    return getCurrentVersion().equals(m.getCurrentVersion()) && m.versionManager.equals(versionManager);
  }

  public int hashCode() {
    return getCurrentVersion().hashCode() + versionManager.hashCode();
  }

  public static Map<Integer,Model> getReferenceToModel() {
    return referenceToModel;
  }

  public static void setReferenceToModel(Map<Integer,Model> ref) {
    referenceToModel = ref;
  }

  public static Map<Integer,Model> doCloneAll(Map<Integer,Model> refToModel) {
    Set<Entry<Integer,Model>> s = refToModel.entrySet();
    Iterator<Entry<Integer,Model>> i = s.iterator();
    Map<Integer,Model> r = new HashMap<Integer,Model>();
    while(i.hasNext()) {
      Entry<Integer,Model> e = i.next();
      r.put(e.getKey(),e.getValue().doClone());
    }
    return r;
  }

  protected abstract Version getCurrentVersion();

  protected void setVersion (int version) {
    env.setIntField(objRef, "version", version);
  }

  protected boolean checkIsNull(int value) {
    if(value == MJIEnv.NULL) {
      env.throwException("java.lang.NullPointerException");
      return true;
    }
    return false;
  }

  protected boolean checkIsNegative(int value) {
    if(value < 0) {
      env.throwException("java.lang.IllegalArgumentException");
      return true;
    }
    return false;
  }

  protected void pinObject(int obj) {
    if(obj != MJIEnv.NULL) {
      env.getHeap().registerPinDown(obj);
    }
  }

  protected void unpinObject(int obj) {
    if(obj != MJIEnv.NULL) {
      env.getHeap().releasePinDown(obj);
    }
  }
}
