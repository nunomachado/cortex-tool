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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * DynamicArea is used to model the heap (dynamic memory), idx.e. the area were all
 * objects created by NEW insn live. Hence the garbage collection mechanism resides here
 */
public class DynamicArea extends Area<DynamicElementInfo> implements Heap, Restorable<Heap>, ElementInfoProcessor {

  protected class LiveIterator<E> implements Iterator<E>, Iterable<E> {
    int idx = elementsMap.nextSetBit(0);

    public void remove() {
      throw new UnsupportedOperationException ("illegal operation, only GC can remove objects");
    }

    public boolean hasNext() {
      return (idx >= 0);
    }

    public E next() {
      if (idx >= 0){
        int ref = idx;
        idx = elementsMap.nextSetBit(idx+1);
        return (E)elements.get(ref);

      } else {
        throw new NoSuchElementException();
      }
    }

    public Iterator<E> iterator() {
      return this;
    }
  }

  protected ReferenceQueue markQueue = new ReferenceQueue();

  protected boolean runFinalizer;
  protected boolean sweep;

  protected boolean outOfMemory; // can be used by listeners to simulate outOfMemory conditions

  /** used to keep track of marked WeakRefs that might have to be updated */
  protected ArrayList<ElementInfo> weakRefs;

  // which elements are in use
  BitSet elementsMap = new BitSet(elements.length());


  // this is toggled before each gc, and always restored to false if we backtrack. Used in conjunction
  // with isAlive(ElementInfo), which returns true if the object is either marked or has the right
  // liveBit value. We need this to avoid additional passes over all live elements at the end of
  // the gc in order to clean up
  boolean liveBitValue;


  static class DAMemento extends AreaMemento<DynamicArea> implements Memento<Heap>{
    DAMemento (DynamicArea area){
      super(area);
    }

    public Heap restore (Heap heap){
      // not very typesafe
      return super.restore((DynamicArea)heap);
    }
  }


  /**
   * Creates a new empty dynamic area.
   */
  public DynamicArea (Config config, KernelState ks) {
    super(ks);

    runFinalizer = config.getBoolean("vm.finalize", false);
    sweep = config.getBoolean("vm.sweep",true);
  }


  @Override
  public void restoreVolatiles () {
    // we always start with false after a restore
    liveBitValue = false;
  }

  public Iterable<ElementInfo> liveObjects() {
    return new LiveIterator<ElementInfo>();
  }

  @Override
  public Iterable<DynamicElementInfo> elements() {
    return new LiveIterator<DynamicElementInfo>();
  }

  public Iterable<ElementInfo> markedObjects() {
    return new MarkedElementInfoIterator();
  }


  public Memento<Heap> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<Heap> getMemento(){
    return new DAMemento(this);
  }

  /**
   * this is of course just a VM specific approximation
   */
  public int getHeapSize () {
    int n=0;
    for (DynamicElementInfo ei : this){
      n += ei.getHeapSize();
    }
    return n;
  }

  public boolean isOutOfMemory () {
    return outOfMemory;
  }

  public void setOutOfMemory (boolean isOutOfMemory) {
    outOfMemory = isOutOfMemory;
  }

  /**
   * Our deterministic  mark & sweep garbage collector.
   * It is called after each transition (forward) that has changed a reference,
   * to ensure heap symmetry (save states), but at the cost of huge
   * gc loads, where we cannot perform all the nasty performance tricks of normal GCs.
   * To avoid overpopulation of our heap, this can also be called every
   * 'vm.max_alloc_gc' allocations.
   * 
   * note that we no longer perform reachability analysis here, which has been
   * replaced by tracking referencing thread ids
   */

  public void gc () {
    // note - it actually seems more efficient to directly iterate over the
    // elements, and not use the elementsMap, which indicates that objects
    // are reasonably packed at this point

    JVM vm = JVM.getVM();
    int length = elements.size();
    weakRefs = null;

    vm.notifyGCBegin();

    markQueue.clear();
    liveBitValue = !liveBitValue; // toggle it

    //--- phase 1 - add our root sets.
    markPinnedDown();
    ks.threads.markRoots(this); // mark thread stacks
    ks.statics.markRoots(this); // mark objects referenced from StaticArea ElementInfos

    //--- phase 2 - traverse all queued elements
    markQueue.process(this);

    //--- phase 3 - run finalization (slightly approximated, since it should be
    // done in a dedicated thread)
    // we need to do this in two passes, or otherwise we might end up
    // removing objects that are still referenced from within finalizers
    if (sweep && runFinalizer) {
      //for (int i = elementsMap.nextSetBit(0); i >= 0; i = elementsMap.nextSetBit(i + 1)) {
      for (int i=0; i<length; i++){
        ElementInfo ei = elements.get(i);
        if (ei == null) continue;
        if (!ei.isMarked()) {
          // <2do> here we have to add the object to the finalizer add
          // and activate the FinalizerThread (which is kind of a root object too)
          // not sure yet how to handle this best to avoid more state space explosion
          // THIS IS NOT YET IMPLEMENTED
        }
      }
    }

    //--- phase 4 - all finalizations are done, reclaim all unmarked objects, idx.e.
    // all objects with 'lastGc' != 'curGc'
    int count = 0;

    ThreadInfo ti = vm.getCurrentThread();
    int tid = ti.getId();
    boolean isThreadTermination = ti.isTerminated();

    for (int i=0; i<length; i++){
      ElementInfo ei = elements.get(i);
      if (ei == null) continue;
      if (!ei.isMarked() && sweep) {
        // this object is garbage, toast it

        ei.processReleaseActions();
        
        count++;
        vm.notifyObjectReleased(ei);
        remove(i, false);
        
      } else {
        // for subsequent gc and serialization
        ei.setUnmarked();
        ei.setAlive(liveBitValue);
        ei.cleanUp(this, isThreadTermination, tid);
      }
    }

    if (sweep) {
      checkWeakRefs(); // for potential nullification
    }

    vm.processPostGcActions();
    vm.notifyGCEnd();
  }


  public void cleanUpDanglingReferences () {
    // nothing - we already cleaned up our live objects
  }


  //--- these are the mark phase methods

  // called from ElementInfo markRecursive. We don't want to expose the
  // markQueue since a copying gc might not have it
  public void queueMark (int objref){
    if (objref == -1) {
      return;
    }

    ElementInfo ei = elements.get(objref);
    if (!ei.isMarked()){ // only add objects once
      ei.setMarked();
      markQueue.add(ei);
    }
  }

  public void queueMark (ElementInfo ei){
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


  public void markPinnedDown () {
    int length = elements.size();
    for (int i = 0; i < length; i++) {
      ElementInfo ei = elements.get(i);
      if (ei != null) {
        if (ei.isPinnedDown()) {
          queueMark(ei);
        }
      }
    }
  }

  /**
   * called during non-recursive phase1 marking of all objects reachable
   * from static fields
   * @aspects: gc
   */
  public void markStaticRoot (int objref) {
    if (objref == -1) {
      return;
    }
    queueMark(objref);
  }

  /**
   * called during non-recursive phase1 marking of all objects reachable
   * from Thread roots
   * @aspects: gc
   */
  public void markThreadRoot (int objref, int tid) {
    if (objref == -1) {
      return;
    }
    queueMark(objref);
  }

  //--- object creation

  /**
   * Creates a new array of the given type
   *
   * NOTE: The elementType has to be either a valid builtin typecode ("B', "C", ..)
   * or an "L-slash" name
   */
  public int newArray (String elementType, int nElements, ThreadInfo ti) {

    //if (!Types.isTypeCode(elementType)) {
    //  elementType = Types.getTypeSignature(elementType, true);
    //}

    String type = "[" + elementType;
    ClassInfo ci = ClassInfo.getResolvedClassInfo(type);

    if (!ci.isInitialized()){
      // we do this explicitly here since there are no clinits for array classes
      ci.registerClass(ti);
      ci.setInitialized();
    }

    int idx = indexFor(ti);
    Fields  f = ci.createArrayFields(type, nElements,
                                     Types.getTypeSize(elementType),
                                     Types.isReference(elementType));
    Monitor  m = new Monitor();

    DynamicElementInfo e = createElementInfo(ci,f, m, ti);
    add(idx, e);

    //if (ti != null) { // maybe we should report them all, and put the burden on the listener
      JVM.getVM().notifyObjectCreated(ti, elements.get(idx));
    //}

    // see newObject for 'outOfMemory' handling

    return idx;
  }


  /**
   * Creates a new object of the given class.
   * NOTE - this does not ensure if the class is already loaded and/or
   * initialized, so that has to be checked in the caller
   *
   * <2do> this should return a DynamicElementInfo (most callers need it anyways,
   * and getting the ref out of the ElementInfo is more efficient than a get(ref)
   */
  public int newObject (ClassInfo ci, ThreadInfo ti) {
    int index;

    // create the thing itself
    Fields             f = ci.createInstanceFields();
    Monitor            m = new Monitor();

    DynamicElementInfo dei = createElementInfo(ci,f, m, ti);

    // get the index where to store this sucker, but be aware of that the
    // returned index might be outside the current elements array (super.add
    // takes care of this <Hrmm>)
    index = indexFor(ti);

    // store it on the heap
    add(index, dei);

    // and do the default (const) field initialization
    ci.initializeInstanceData(dei);

    //if (ti != null) { // maybe we should report them all, and put the burden on the listener
      JVM.getVM().notifyObjectCreated(ti, dei);
    //}

    // note that we don't return -1 if 'outOfMemory' (which is handled in
    // the NEWxx bytecode) because our allocs are used from within the
    // exception handling of the resulting OutOfMemoryError (and we would
    // have to override it, since the VM should guarantee proper exceptions)

    return index;
  }

  /**
   * Creates a new string.
   */
  public int newString (String str, ThreadInfo th) {
    if (str != null) {
      int length = str.length();
      int index = newObject(ClassInfo.stringClassInfo, th);
      int value = newArray("C", length, th);

      ElementInfo e = get(index);
      // <2do> pcm - this is BAD, we shouldn't depend on private impl of
      // external classes - replace with our own java.lang.String !
      e.setReferenceField("value", value);

      ElementInfo eVal = get(value);
      CharArrayFields cf = (CharArrayFields)eVal.getFields();
      cf.setCharValues(str.toCharArray());

      return index;
    } else {
      return -1;
    }
  }


  // we are trying to save a backtracked HashMap here. The idea is that the string
  // value chars are constant and not attributed, so the Fields object of the
  // 'char[] value' should never change. The string Fields itself
  // can (e.g. if we set symbolic attributes on the value field), so we have
  // to go one level deeper. We can't just store the String ElementInfo in the
  // map because the new state branch might just have reused it for another string
  // (unlikely but possible)
  static class InternStringEntry {
    String str;
    int ref;
    Fields fValue;

    InternStringEntry (String str, int ref, Fields fValue){
      this.str = str;
      this.ref = ref;
      this.fValue = fValue;
    }
  }

  protected boolean checkInternStringEntry (InternStringEntry e) {
    ElementInfo ei = get(e.ref);
    if (ei != null && ei.getClassInfo() == ClassInfo.stringClassInfo) {
      // check if it was the interned string
      int vref = ei.getReferenceField("value");
      ei = get(vref);
      if (ei != null && ei.getFields() == e.fValue) {
        return true;
      }
    }

    return false;
  }

  protected HashMap<String,InternStringEntry> internStrings = new HashMap<String,InternStringEntry>();

  public int newInternString (String str, ThreadInfo ti) {
    int ref = -1;

    InternStringEntry e = internStrings.get(str);
    if (e == null || !checkInternStringEntry(e)) { // not seen or new state branch
      ref = newString(str,ti);
      ElementInfo ei = get(ref);
      ei.incPinDown();

      int vref = ei.getReferenceField("value");
      ElementInfo eiValues = get(vref);
      internStrings.put(str, new InternStringEntry(str,ref,eiValues.getFields()));
      return ref;

    } else {
      return e.ref;
    }
  }

  //--- the primitive add and remove operations (update the elementsMap)

  @Override
  protected void add (int index, DynamicElementInfo dei){
    elementsMap.set(index);
    super.add(index, dei);
  }

  @Override
  protected void set (int index, DynamicElementInfo dei){
    elementsMap.set(index);
    super.set(index, dei);
  }

  @Override
  protected void remove (int index, boolean nullOk){
    elementsMap.clear(index);
    super.remove(index, nullOk);
  }

  @Override
  public void removeAllFrom (int index) {
    elementsMap.clear(index, elementsMap.length());
    super.removeAllFrom(index);
  }

  @Override
  public void removeAll() {
    elementsMap.clear();
    super.removeAll();
  }

  @Override
  public void removeRange( int fromIdx, int toIdx){
    elementsMap.clear(fromIdx, toIdx);
    super.removeRange(fromIdx,toIdx);
  }

  public void registerPinDown(int objref){
    ElementInfo ei = elements.get(objref);
    ei.incPinDown();
  }

  public void releasePinDown(int objref){
    ElementInfo ei = elements.get(objref);
    ei.decPinDown();
  }

  public void registerWeakReference (ElementInfo ei) {
    if (weakRefs == null) {
      weakRefs = new ArrayList<ElementInfo>();
    }

    weakRefs.add(ei);
  }

  public boolean isAlive (ElementInfo ei){
    return (ei == null || ei.isMarkedOrAlive(liveBitValue));
  }

  /**
   * reset all weak references that now point to collected objects to 'null'
   * NOTE: this implementation requires our own Reference/WeakReference implementation, to
   * make sure the 'ref' field is the first one
   */
  protected void checkWeakRefs () {
    if (weakRefs != null) {
      for (ElementInfo ei : weakRefs) {
        Fields f = ei.getFields();
        int    ref = f.getIntValue(0); // watch out, the 0 only works with our own WeakReference impl
        if (ref != MJIEnv.NULL) {
          ElementInfo refEi = get(ref);
          if ((refEi == null) || (refEi.isNull())) {
            // we need to make sure the Fields are properly state managed
            ei.setReferenceField(ei.getFieldInfo(0), MJIEnv.NULL);
          }
        }
      }

      weakRefs = null;
    }
  }

  //--- factory methods for creating associated ElementInfos
  protected DynamicElementInfo createElementInfo () {
    return new DynamicElementInfo();
  }

  protected DynamicElementInfo createElementInfo (ClassInfo ci, Fields f, Monitor m, ThreadInfo ti){
    int tid = ti == null ? 0 : ti.getId();
    return new DynamicElementInfo(ci,f,m,tid);
  }


  protected int indexFor (ThreadInfo ti){
    //return elements.nextNull(0);
    return elementsMap.nextClearBit(0);
  }

  
  // for debugging only
  public void checkConsistency(boolean isStore) {
    int nTotal = 0;
    for (int i = 0; i<elements.size(); i++) {
      DynamicElementInfo ei = elements.get(i);
            
      if (ei != null) {
        assert ei.getObjectRef() == i : "inconsistent reference value of " + ei + " : " + i;
        if (ei.hasChanged()){
          assert hasChanged.get(i) : "inconsistent change status of " + ei;
        }
        nTotal++;
        
        ei.checkConsistency();
      }      
    }
    
    assert (nTotal == nElements) : "inconsistent number of elements: " + nTotal;
  }
}


