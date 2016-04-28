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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.HashData;

/**
 * A specialized version of ElementInfo for use in the StaticArea.  The
 * StaticElementInfo is only used to store "static class fields" in the
 * StaticArea.  It specifically knows about the relationship amongst
 * classes, and will recursively lookup a data member if needed.
 * 
 * Note that refTid is not set in the constructor, but in the first
 * checkUpdatedSharedness() call, which always returns true (i.e. breaks)
 * on the first invocation. This is to handle the case of non-deterministic
 * class loading in symmetric threads - if each of them terminates before
 * the other hits the contended static field ref, none would ever consider this
 * to be a shared field and miss the race
 */
public final class StaticElementInfo extends ElementInfo implements Restorable<ElementInfo> {

  // this is kind of dangerous - make sure these flags are still unused in ElementInfo
  static final int ATTR_COR_CHANGED    = 0x100000;
  static final int ATTR_STATUS_CHANGED = 0x200000;

  static final int ATTR_ANY_CHANGED = ElementInfo.ATTR_ANY_CHANGED | ATTR_COR_CHANGED | ATTR_STATUS_CHANGED;


  int classObjectRef = -1;
  int status = ClassInfo.UNINITIALIZED;


  // our default memento implementation
  static class SEIMemento extends EIMemento<StaticElementInfo> {
    int classObjectRef;
    int status;

    SEIMemento (StaticElementInfo ei) {
      super(ei);

      this.classObjectRef = ei.classObjectRef;
      this.status = ei.status;
    }

    @Override
    public ElementInfo restore (ElementInfo ei){
      StaticElementInfo sei = (ei != null) ? (StaticElementInfo) ei : get();
      if (sei == null){
        sei = new StaticElementInfo();
      }

      super.restore(sei);

      sei.status = status;
      sei.classObjectRef = classObjectRef;

      return sei;
    }

    /** for debugging purposes
    public boolean equals(Object o){
      if (o instanceof SEIMemento){
        SEIMemento other = (SEIMemento)o;
        if (!super.equals(o)) return false;
        if (classObjectRef != other.classObjectRef) return false;
        if (status != other.status) return false;
        return true;
      }
      return false;
    }
    public String toString() {
      return "SEIMemento {{" +super.toString() + "},classObjRef="+classObjectRef+",status="+status+"}";
    }
    **/
  }

  public StaticElementInfo () {
  }

  public StaticElementInfo (ClassInfo ci, Fields f, Monitor m, int tid, int classObjRef) {
    super(ci, f, m, tid);

    // note we don't set refTid yet 
    //refTid = createRefTid(tid);
    
    classObjectRef = classObjRef;

    // initial attributes?
  }

  //@Override 
  public boolean checkUpdatedSharedness (ThreadInfo ti) {
    if (refTid == null){
      refTid = createRefTid( ti.getId());
      attributes |= ElementInfo.ATTR_REFTID_CHANGED;
      return true;
    } else {
      return super.checkUpdatedSharedness(ti);
    }
  }
  
  public boolean isObject(){
    return false;
  }
  
  public Memento<ElementInfo> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<ElementInfo> getMemento(){
    return new SEIMemento(this);
  }
  
  @Override
  protected int getNumberOfFieldsOrElements(){
    // static fields can't be arrays, those are always heap objects
    return ci.getNumberOfStaticFields();
  }

  @Override
  public boolean hasChanged() {
    return (attributes & ATTR_ANY_CHANGED) != 0;
  }

  @Override
  public void markUnchanged() {
    attributes &= ~ATTR_ANY_CHANGED;
  }
  
  @Override
  public void hash(HashData hd) {
    super.hash(hd);
    hd.add(classObjectRef);
    hd.add(status);
  }

  @Override
  public boolean equals(Object o) {
    if (super.equals(o) && o instanceof StaticElementInfo) {
      StaticElementInfo other = (StaticElementInfo) o;

      if (classObjectRef != other.classObjectRef) {
        return false;
      }
      if (status != other.status) {
        return false;
      }

      return true;

    } else {
      return false;
    }
  }

  protected void markAreaChanged(){
    JVM.getVM().getStaticArea().markChanged(objRef);
  }

  public boolean isShared() {
    // static fields are always thread global
    return true;
  }

  public int getStatus() {
    return status;
  }
  
  void setStatus (int newStatus) {
    if (status != newStatus) {
      status = newStatus;
      attributes |= ATTR_STATUS_CHANGED;
      markAreaChanged();
    }
  }

  
  protected FieldInfo getDeclaredFieldInfo (String clsBase, String fname) {
    ClassInfo ci = ClassInfo.getResolvedClassInfo(clsBase);
    FieldInfo fi = ci.getDeclaredStaticField(fname);
    
    if (fi == null) {
      throw new JPFException("class " + ci.getName() +
                                         " has no static field " + fname);
    }
    return fi;
  }

  public FieldInfo getFieldInfo (String fname) {
    ClassInfo ci = getClassInfo();
    return ci.getStaticField(fname);
  }
  
  protected void checkFieldInfo (FieldInfo fi) {
    if (getClassInfo() != fi.getClassInfo()) {
      throw new JPFException("wrong static FieldInfo : " + fi.getName()
          + " , no such field in class " + getClassInfo().getName());
    }
  }

  public int getNumberOfFields () {
    return getClassInfo().getNumberOfStaticFields();
  }
  
  public FieldInfo getFieldInfo (int fieldIndex) {
    return getClassInfo().getStaticField(fieldIndex);
  }
  
  protected ElementInfo getElementInfo (ClassInfo ci) {
    if (ci == getClassInfo()) {
      return this;
    } else {
      return JVM.getVM().getStaticArea().get( ci.getName());
    }
  }
  
  /**
   * mark all our fields as static (shared) reachable. No need to set our own
   * attributes, since we reside in the StaticArea
   * @aspects: gc
   */
  void markStaticRoot (Heap heap) {
    // WATCH IT! this overrides the heap object behavior in our super class.
    // See ElementInfo.markStaticRoot() for details
    
    ClassInfo ci = getClassInfo();
    int n = ci.getNumberOfStaticFields();
    
    for (int i=0; i<n; i++) {
      FieldInfo fi = ci.getStaticField(i);
      if (fi.isReference()) {
        heap.markStaticRoot(fields.getIntValue(fi.getStorageOffset()));
      }
    }
    
    // don't forget the class object itself (which is not a field)
    heap.markStaticRoot(classObjectRef);
  }
      
  public int getClassObjectRef () {
    return classObjectRef;
  }
  
  public void setClassObjectRef(int r) {
    classObjectRef = r;
    attributes |= ATTR_COR_CHANGED;
  }

  public String toString() {
    return getClassInfo().getName(); // don't append objRef (useless and misleading for statics)
  }

  protected ElementInfo getReferencedElementInfo (FieldInfo fi){
    assert fi.isReference();
    Heap heap = JVM.getVM().getHeap();
    return heap.get(getIntField(fi));
  }
}

