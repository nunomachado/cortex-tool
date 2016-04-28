//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.Debug;
import gov.nasa.jpf.util.IntTable;


/**
 * memory area for static fields
 */
public class StaticArea extends Area<StaticElementInfo> implements Restorable<StaticArea> {

  /**
   * analogy of DynamicMap to achieve symmetry for static fields (so that order
   * of class init does not matter)
   */
  private IntTable<String> staticMap = new IntTable<String>();


  static class SAMemento extends AreaMemento<StaticArea> implements Memento<StaticArea> {
    SAMemento (StaticArea area){
      super(area);
    }

    public StaticArea restore (StaticArea area){
      super.restore(area);
      return area;
    }
  }

  /**
   * Creates a new empty static area.
   */
  public StaticArea (Config config, KernelState ks) {
    super(ks);
  }

  public Memento<StaticArea> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<StaticArea> getMemento(){
    return new SAMemento(this);
  }


  public boolean containsClass (String cname) {
    return indexOf(cname) != -1;
  }

  public StaticElementInfo get (String cname) {
    int index = indexOf(cname);

    if (index == -1) {
      return null;
    } else {
      return get(index);
    }
  }


  public void resetVolatiles() {
    for (int i=0; i<elements.size(); i++) {
      ElementInfo ei = elements.get(i);
      if (ei != null) {
        ClassInfo ci = ei.getClassInfo();
        ci.setStaticElementInfo(null);
      }
    }
  }

  public void restoreVolatiles() {
    for (int i=0; i<elements.size(); i++) {
      StaticElementInfo sei = elements.get(i);
      if (sei != null) {
        ClassInfo ci = sei.getClassInfo();
        ci.setStaticElementInfo(sei);
      }
    }
  }


  /**
   * Returns the index of a given class.
   */
  public int indexOf (String cname) {
    IntTable.Entry<String> e = staticMap.get(cname);
    if (e != null && elements.get(e.val) != null) {
      return e.val;
    } else {
      return -1;
    }
  }

  public void log () {
    Debug.println(Debug.MESSAGE, "SA");

    for (int i = 0; i < elements.size(); i++) {
      ElementInfo ei = elements.get(i);
      if (ei != null) {
        ei.log();
      }
    }
  }

  public void markRoots (Heap heap) {
    for (StaticElementInfo ei : this.elements()){
      ei.markStaticRoot(heap);
    }
  }

  //--- StaticElementInfo factory methods
  protected StaticElementInfo createElementInfo () {
    return new StaticElementInfo();
  }

  protected StaticElementInfo createElementInfo (ClassInfo ci,Fields f, Monitor m, int tid, int clsObjRef){
    return new StaticElementInfo(ci,f,m,tid, clsObjRef);
  }


  protected StaticElementInfo createElementInfo (ClassInfo ci, int tid, int clsObjRef) {
    Fields   f = ci.createStaticFields();
    Monitor  m = new Monitor();

    StaticElementInfo ei = createElementInfo(ci,f, m, tid, clsObjRef);
    ci.setStaticElementInfo(ei);

    ci.initializeStaticData(ei);

    return ei;
  }

  /**
   * note this has to be followed by creating, initializing and linking
   * a class object - the StaticElementInfo needs its reference value
   */
  public StaticElementInfo addClass (ClassInfo ci, ThreadInfo ti) {
    int tid = ti == null ? 0 : ti.getId();
    StaticElementInfo ei = createElementInfo(ci, tid, -1);
    int index = indexFor(ci.getName());

    add(index, ei);
    ci.setStaticElementInfo(ei);

    return ei;
  }

  /**
   * Returns the index where class with specified name would be stored,
   * regardless of whether it's in the structure.
   */
  protected int indexFor (String cname) {
    return staticMap.poolIndex(cname);
  }
}
