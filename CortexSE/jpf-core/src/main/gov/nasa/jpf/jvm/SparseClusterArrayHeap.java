//
// Copyright (C) 2010 United States Government as represented by the
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
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.SparseClusterArray;
import gov.nasa.jpf.util.Transformer;

import java.util.ArrayList;

/**
 * a Heap implementation that is based on the SparseClusterArray
 */
public class SparseClusterArrayHeap extends SparseClusterArray<ElementInfo> implements Heap, Restorable<Heap>, ElementInfoProcessor {

  public static final int MAX_THREADS = MAX_CLUSTERS; // 256
  public static final int MAX_THREAD_ALLOC = MAX_CLUSTER_ENTRIES;  // 16,777,215

  protected JVM vm;

  // list of pinned down references (this is only efficient for a small number of objects)
  // this is copy-on-first-write
  protected IntVector pinDownList;

  // interned Strings
  // this is copy-on-first-write
  protected IntTable<String> internStrings;


  // the usual drill - the lower 2 bytes are sticky, the upper two ones 
  // hold change status and transient (transition local) flags
  int attributes;

  static final int ATTR_GC            = 0x0001;
  static final int ATTR_OUT_OF_MEMORY = 0x0002;
  static final int ATTR_RUN_FINALIZER = 0x0004;

  static final int ATTR_ELEMENTS_CHANGED  = 0x10000;
  static final int ATTR_PINDOWN_CHANGED   = 0x20000;
  static final int ATTR_INTERN_CHANGED    = 0x40000;
  static final int ATTR_ATTRIBUTE_CHANGED = 0x80000;

  // masks and sets
  static final int ATTR_STORE_MASK = 0x0000ffff;
  static final int ATTR_ANY_CHANGED = (ATTR_ELEMENTS_CHANGED | ATTR_PINDOWN_CHANGED | ATTR_INTERN_CHANGED | ATTR_ATTRIBUTE_CHANGED);

  //--- these objects are only used during gc

  // used to keep track of marked WeakRefs that might have to be updated (no need to restore, only transient use during gc)
  protected ArrayList<ElementInfo> weakRefs;

  protected ReferenceQueue markQueue = new ReferenceQueue();

  // this is set to false upon backtrack/restore
  protected boolean liveBitValue;


  public static class Snapshot<T> extends SparseClusterArray.Snapshot<ElementInfo,T> {
    int attributes;
    IntVector pinDownList;
    IntTable<String> internStrings;

    Snapshot (int size){
      super(size);
    }
  }

  static Transformer<ElementInfo,Memento<ElementInfo>> ei2mei = new Transformer<ElementInfo,Memento<ElementInfo>>(){
    public Memento<ElementInfo> transform (ElementInfo ei){
      Memento<ElementInfo> m = null;
      if (!ei.hasChanged()) {
        m = ei.cachedMemento;
      }
      if (m == null) {
        m = ei.getMemento();
        ei.cachedMemento = m;
      }
      return m;
    }
  };

  static Transformer<Memento<ElementInfo>,ElementInfo> mei2ei = new Transformer<Memento<ElementInfo>,ElementInfo>(){
    public ElementInfo transform(Memento<ElementInfo> m) {
      ElementInfo ei = m.restore(null);
      ei.cachedMemento = m;
      return ei;
    }
  };

  // our default memento implementation
  static class SCAMemento implements Memento<Heap> {
    SparseClusterArrayHeap.Snapshot<Memento<ElementInfo>> snap;

    SCAMemento(SparseClusterArrayHeap sca) {
      snap = sca.getSnapshot(ei2mei);
      sca.markUnchanged();
    }

    public Heap restore(Heap inSitu) {
      SparseClusterArrayHeap sca = (SparseClusterArrayHeap)inSitu;
      sca.restoreSnapshot(snap, mei2ei);
      return sca;
    }

  }


  public SparseClusterArrayHeap (Config config, KernelState ks){
    vm = JVM.getVM();

    pinDownList = new IntVector(256);
    attributes |= ATTR_PINDOWN_CHANGED; // no need to clone on next add

    internStrings = new IntTable(8);
    attributes |= ATTR_INTERN_CHANGED; // no need to clone on next add

    if (config.getBoolean("vm.finalize", true)){
      attributes |= ATTR_RUN_FINALIZER;
    }

    if (config.getBoolean("vm.sweep",true)){
      attributes |= ATTR_GC;
    }
  }

  // internal stuff

  protected DynamicElementInfo createElementInfo (ClassInfo ci, Fields f, Monitor m, ThreadInfo ti){
    int tid = ti == null ? 0 : ti.getId();
    return new DynamicElementInfo(ci,f,m,tid);
  }

  public <T> Snapshot<T> getSnapshot (Transformer<ElementInfo,T> transformer){
    Snapshot snap = new Snapshot(nSet);
    populateSnapshot(snap,transformer);

    // these are copy-on-first-write
    snap.pinDownList = pinDownList;
    snap.internStrings = internStrings;
    snap.attributes = attributes & ATTR_STORE_MASK;

    return snap;
  }

  public <T> void restoreSnapshot (Snapshot<T> snap, Transformer<T,ElementInfo> transformer){
    super.restoreSnapshot(snap, transformer);

    pinDownList = snap.pinDownList;
    internStrings = snap.internStrings;
    attributes = snap.attributes;

    liveBitValue = false; // always start with false after a restore
  }

  //--- Heap interface

  public boolean isGcEnabled (){
    return (attributes & ATTR_GC) != 0;
  }

  public void setGcEnabled(boolean doGC) {
    if (doGC != isGcEnabled()) {
      if (doGC) {
        attributes |= ATTR_GC;
      } else {
        attributes &= ~ATTR_GC;
      }
      attributes |= ATTR_ATTRIBUTE_CHANGED;
    }
  }

  public boolean isOutOfMemory() {
    return (attributes & ATTR_OUT_OF_MEMORY) != 0;
  }

  public void setOutOfMemory(boolean isOutOfMemory) {
    if (isOutOfMemory != isOutOfMemory()) {
      if (isOutOfMemory) {
        attributes |= ATTR_OUT_OF_MEMORY;
      } else {
        attributes &= ~ATTR_OUT_OF_MEMORY;
      }
      attributes |= ATTR_ATTRIBUTE_CHANGED;
    }
  }

  public int newArray(String elementType, int nElements, ThreadInfo ti) {
    String type = "[" + elementType;
    ClassInfo ci = ClassInfo.getResolvedClassInfo(type);

    if (!ci.isInitialized()){
      // we do this explicitly here since there are no clinits for array classes
      ci.registerClass(ti);
      ci.setInitialized();
    }

    Fields  f = ci.createArrayFields(type, nElements,
                                     Types.getTypeSize(elementType),
                                     Types.isReference(elementType));
    Monitor  m = new Monitor();
    DynamicElementInfo ei = createElementInfo(ci, f, m, ti);

    int tid = (ti != null) ? ti.getId() : 0;
    int index = firstNullIndex(tid << S1, MAX_CLUSTER_ENTRIES);
    if (index < 0){
      throw new JPFException("per-thread heap limit exceeded");
    }
    ei.setObjectRef(index);
    set(index, ei);
    
    attributes |= ATTR_ELEMENTS_CHANGED;

    vm.notifyObjectCreated(ti, ei);

    // see newObject for 'outOfMemory' handling

    return index;
  }

  public int newObject(ClassInfo ci, ThreadInfo ti) {
    // create the thing itself
    Fields f = ci.createInstanceFields();
    Monitor m = new Monitor();
    ElementInfo ei = createElementInfo(ci, f, m, ti);

    // get next free objRef into thread cluster
    int tid = (ti != null) ? ti.getId() : 0;
    int index = firstNullIndex(tid << S1, MAX_CLUSTER_ENTRIES);
    if (index < 0){
      throw new JPFException("per-thread heap limit exceeded");
    }
    ei.setObjectRef(index);
    set(index, ei);

    attributes |= ATTR_ELEMENTS_CHANGED;

    // and do the default (const) field initialization
    ci.initializeInstanceData(ei);

    vm.notifyObjectCreated(ti, ei);
    
    // note that we don't return -1 if 'outOfMemory' (which is handled in
    // the NEWxx bytecode) because our allocs are used from within the
    // exception handling of the resulting OutOfMemoryError (and we would
    // have to override it, since the VM should guarantee proper exceptions)

    return index;
  }

  private int newString(String str, ThreadInfo ti, boolean isIntern) {
    if (str != null) {      
      int length = str.length();
      int index = newObject(ClassInfo.stringClassInfo, ti);
      int vref = newArray("C", length, ti);
      
      ElementInfo e = get(index);
      // <2do> pcm - this is BAD, we shouldn't depend on private impl of
      // external classes - replace with our own java.lang.String !
      e.setReferenceField("value", vref);

      ElementInfo eVal = get(vref);
      CharArrayFields cf = (CharArrayFields)eVal.getFields();
      cf.setCharValues(str.toCharArray());

      if (isIntern){
        // we know it's not in the pinDown list yet, this is a new object
        e.incPinDown();
        addToPinDownList(index);
      }

      return index;

    } else {
      return -1;
    }
  }

  public int newString(String str, ThreadInfo ti){
    return newString(str,ti,false);
  }

  public int newInternString (String str, ThreadInfo ti) {
    IntTable.Entry<String> e = internStrings.get(str);
    if (e == null){
      int objref = newString(str,ti,true);

      if ((attributes & ATTR_INTERN_CHANGED) == 0){
        internStrings = internStrings.clone();
        attributes |= ATTR_INTERN_CHANGED;
      }
      internStrings.add(str, objref);

      return objref;

    } else {
      return e.val;
    }
  }

  public Iterable<ElementInfo> liveObjects() {
    return new ElementIterator<ElementInfo>();
  }

  public int size() {
    return nSet;
  }

  public boolean hasChanged() {
    return (attributes & ATTR_ANY_CHANGED) != 0;
  }

  public void unmarkAll(){
    for (ElementInfo ei : liveObjects()){
      ei.setUnmarked();
    }
  }

  public void markUnchanged() {
    attributes &= ~ATTR_ANY_CHANGED;
  }

  // clean up reference values outside of reference fields 
  public void cleanUpDanglingReferences() {
    // <2do> get rid of this by storing objects instead of ref/id values that can be reused
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    int tid = ti.getId();
    boolean isThreadTermination = ti.isTerminated();
    
    for (ElementInfo e : this) {
      if (e != null) {
        e.cleanUp(this, isThreadTermination, tid);
      }
    }
  }

  protected void addToPinDownList (int objref){
    if ((attributes & ATTR_PINDOWN_CHANGED) == 0) {
      pinDownList = pinDownList.clone();
      attributes |= ATTR_PINDOWN_CHANGED;
    }
    pinDownList.add(objref);
  }
  
  protected void removeFromPinDownList (int objref){
    if ((attributes & ATTR_PINDOWN_CHANGED) == 0) {
      pinDownList = pinDownList.clone();
      attributes |= ATTR_PINDOWN_CHANGED;
    }
    pinDownList.removeFirst(objref);    
  }

  public void registerPinDown(int objref){
    ElementInfo ei = get(objref);
    if (ei != null) {
      if (ei.incPinDown()){
        addToPinDownList(objref);
      }
    } else {
      throw new JPFException("pinDown reference not a live object: " + objref);
    }
  }

  public void releasePinDown(int objref){
    ElementInfo ei = get(objref);
    if (ei != null) {
      if (ei.decPinDown()){
        removeFromPinDownList(objref);
      }
    } else {
      throw new JPFException("pinDown reference not a live object: " + objref);
    }
  }

  public void registerWeakReference (ElementInfo ei) {
    if (weakRefs == null) {
      weakRefs = new ArrayList<ElementInfo>();
    }

    weakRefs.add(ei);
  }

  /**
   * reset all weak references that now point to collected objects to 'null'
   * NOTE: this implementation requires our own Reference/WeakReference implementation, to
   * make sure the 'ref' field is the first one
   */
  protected void cleanupWeakRefs () {
    if (weakRefs != null) {
      for (ElementInfo ei : weakRefs) {
        Fields f = ei.getFields();
        int    ref = f.getIntValue(0); // watch out, the 0 only works with our own WeakReference impl
        if (ref != -1) {
          ElementInfo refEi = get(ref);
          if ((refEi == null) || (refEi.isNull())) {
            // we need to make sure the Fields are properly state managed
            ei.setReferenceField(ei.getFieldInfo(0), -1);
          }
        }
      }

      weakRefs = null;
    }
  }
  
  public void gc() {
    vm.notifyGCBegin();

    markQueue.clear();
    weakRefs = null;
    liveBitValue = !liveBitValue;

    markPinDownList();
    vm.getThreadList().markRoots(this); // mark thread stacks
    vm.getStaticArea().markRoots(this); // mark objects referenced from StaticArea ElementInfos

    // add pinned down objects

    // at this point, all roots should be in the markQueue, but not traced yet

    markQueue.process(this); // trace all entries - this gets recursive
    
    ThreadInfo ti = vm.getCurrentThread();
    int tid = ti.getId();
    boolean isThreadTermination = ti.isTerminated();
    
    // now go over all objects, purge the ones that are not live and reset attrs for rest
    for (ElementInfo ei : this){
      
      if (ei.isMarked()){ // live object, prepare for next transition & gc cycle
        ei.setUnmarked();
        ei.setAlive(liveBitValue);
        
        ei.cleanUp(this, isThreadTermination, tid);
        
      } else { // object is no longer reachable  
        /**
        MethodInfo mi = ei.getClassInfo().getFinalizer();
        if (mi != null){
          // add to finalizer queue, but keep alive until processed
        } else {
        }
        **/
        ei.processReleaseActions();
        
        // <2do> still have to process finalizers here, which might make the object live again
        vm.notifyObjectReleased(ei);
        set(ei.getObjectRef(), null);   // <2do> - do we need a separate remove?
      }
    }

    cleanupWeakRefs(); // for potential nullification

    vm.processPostGcActions();
    vm.notifyGCEnd();
  }

  public boolean isAlive (ElementInfo ei){
    return (ei == null || ei.isMarkedOrAlive(liveBitValue));
  }

  //--- these are the mark phase methods

  // called from ElementInfo markRecursive. We don't want to expose the
  // markQueue since a copying gc might not have it
  public void queueMark (int objref){
    if (objref == -1) {
      return;
    }

    ElementInfo ei = get(objref);
    if (!ei.isMarked()){ // only add objects once
      ei.setMarked();
      markQueue.add(ei);
    }
  }

  // called from ReferenceQueue during processing of queued references
  // note that all queued references are alread marked as live
  public void processElementInfo (ElementInfo ei) {
    ei.markRecursive( this); // this might in turn call queueMark
  }

  void markPinDownList (){
    if (pinDownList != null){
      int len = pinDownList.size();
      for (int i=0; i<len; i++){
        int objref = pinDownList.get(i);
        queueMark(objref);
      }
    }
  }

  /**
   * called during non-recursive phase1 marking of all objects reachable
   * from static fields
   * @aspects: gc
   */
  public void markStaticRoot (int objref) {
    if (objref != -1) {
      queueMark(objref);
    }
  }

  /**
   * called during non-recursive phase1 marking of all objects reachable
   * from Thread roots
   * @aspects: gc
   */
  public void markThreadRoot (int objref, int tid) {
    if (objref != -1) {
      queueMark(objref);
    }
  }

  public void markChanged(int objref) {
    attributes |= ATTR_ELEMENTS_CHANGED;
  }

  public void hash(HashData hd) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void resetVolatiles() {
    // we don't have any
  }

  public void restoreVolatiles() {
    // we don't have any
  }

  public Memento<Heap> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<Heap> getMemento(){
    return new SCAMemento(this);
  }



  public void checkConsistency(boolean isStateStore) {
    for (ElementInfo ei : this){
      ei.checkConsistency();
    }
  }

  /**
   * this sucks! Since the "E get(int)" of SparseClusterArray gets type erased
   * to "get:(I)Ljava/lang/Object", the compiler automatically wraps it into a
   * "ElementInfo get(int)" in SparseClusterArrayHeap (at least it uses invokespecial)
   *
   * at least we have to slightly modify the super.get() anyways since we
   * treat negative indices as a 'null' reference instead of throwing an exception.
   * It also saves us one additional (ElementInfo) cast in the wrapper method,
   * but otherwise its just a bad case of copied code
   */
  public ElementInfo get(int i) {
    Node l1;
    ChunkNode l2;
    Chunk l3 = lastChunk;

    if (i < 0) {
      return null;
    }

    if (l3 != null && (l3.base == (i & CHUNK_BASEMASK))) {  // cache optimization for in-cluster access
      return (ElementInfo) l3.elements[i & ELEM_MASK];
    }

    int j = i >>> S1;
    if ((l1 = root.seg[j]) != null) {           // L1
      j = (i >>> S2) & SEG_MASK;
      if ((l2 = l1.seg[j]) != null) {           // L2
        j = (i >>> S3) & SEG_MASK;
        if ((l3 = l2.seg[j]) != null) {         // L3
          // too bad we can't get rid of this cast
          lastChunk = l3;
          return (ElementInfo) l3.elements[i & ELEM_MASK];
        }
      }
    }

    lastChunk = null;
    return null;
  }

}
