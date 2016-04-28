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
package gov.nasa.jpf.jvm.serialize;


import gov.nasa.jpf.jvm.AbstractSerializer;
import gov.nasa.jpf.jvm.ArrayFields;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.ElementInfoProcessor;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.Fields;
import gov.nasa.jpf.jvm.Heap;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.ReferenceProcessor;
import gov.nasa.jpf.jvm.ReferenceQueue;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.StaticArea;
import gov.nasa.jpf.jvm.StaticElementInfo;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.ThreadList;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.BitArray;
import gov.nasa.jpf.util.FinalBitSet;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.ObjVector;

import java.util.List;


/**
 * serializer that can ignore marked fields and stackframes for state matching
 *
 * <2do> rework filter policies
 */
public class FilteringSerializer extends AbstractSerializer implements ElementInfoProcessor, ReferenceProcessor {

  // indexed by method globalId
  final ObjVector<FramePolicy> methodCache    = new ObjVector<FramePolicy>();

  // the Fields slots reference masks, indexed by class uniqueId
  final ObjVector<FinalBitSet> instanceRefMasks = new ObjVector<FinalBitSet>();
  final ObjVector<FinalBitSet> staticRefMasks   = new ObjVector<FinalBitSet>();

  // the Fields slots filter masks, indexed by class uniqueid
  final ObjVector<FinalBitSet> instanceFilterMasks = new ObjVector<FinalBitSet>();
  final ObjVector<FinalBitSet> staticFilterMasks   = new ObjVector<FinalBitSet>();


  protected FilterConfiguration filter;

  protected transient IntVector buf = new IntVector(4096);

  Heap heap;


  @Override
  public void attach(JVM jvm) {
    super.attach(jvm);
    
    filter = jvm.getConfig().getInstance("filter.class", FilterConfiguration.class);
    if (filter == null) {
      filter = new DefaultFilterConfiguration();
    }
    filter.init(jvm.getConfig());
  }

  protected FramePolicy getFramePolicy(MethodInfo mi) {
    FramePolicy p = null;

    int mid = mi.getGlobalId();
    if (mid >= 0){
      p = methodCache.get(mid);
    if (p == null) {
      p = filter.getFramePolicy(mi);
      methodCache.set(mid, p);
    }
    } else {
      p = filter.getFramePolicy(mi);
    }

    return p;
  }

  protected FinalBitSet getInstanceRefMask(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = instanceRefMasks.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getInstanceDataSize());
      for (FieldInfo fi : filter.getMatchedInstanceFields(ci)) {
        if (fi.isReference()) {
          b.set(fi.getStorageOffset());
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      instanceRefMasks.set(cid, v);
    }
    return v;
  }

  protected FinalBitSet getStaticRefMask(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = staticRefMasks.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getStaticDataSize());
      for (FieldInfo fi : filter.getMatchedStaticFields(ci)) {
        if (fi.isReference()) {
          b.set(fi.getStorageOffset());
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      staticRefMasks.set(cid, v);
    }
    return v;
  }

  protected FinalBitSet getInstanceFilterMask(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = instanceFilterMasks.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getInstanceDataSize());
      b.setAll();
      for (FieldInfo fi : filter.getMatchedInstanceFields(ci)) {
        int start = fi.getStorageOffset();
        int end = start + fi.getStorageSize();
        for (int i = start; i < end; i++) {
          b.clear(i);
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      instanceFilterMasks.set(cid, v);
    }
    return v;
  }

  protected FinalBitSet getStaticFilterMask(ClassInfo ci) {
    int cid = ci.getUniqueId();
    FinalBitSet v = staticFilterMasks.get(cid);
    if (v == null) {
      BitArray b = new BitArray(ci.getStaticDataSize());
      b.setAll();
      for (FieldInfo fi : filter.getMatchedStaticFields(ci)) {
        int start = fi.getStorageOffset();
        int end = start + fi.getStorageSize();
        for (int i = start; i < end; i++) {
          b.clear(i);
        }
      }
      v = FinalBitSet.create(b);
      if (v == null) throw new IllegalStateException("Null BitArray returned.");
      staticFilterMasks.set(cid, v);
    }
    return v;
  }


  //--- the methods that implement the heap traversal

  protected ReferenceQueue refQueue;

  protected void initReferenceQueue() {
    // note - this assumes all heap objects are in an unmarked state, but this
    // is true if we execute outside the gc

    if (refQueue == null){
      refQueue = new ReferenceQueue();
    } else {
      refQueue.clear();
    }
  }


  //--- those are the methods that can be overridden by subclasses to implement abstractions

  // needs to be public because of ReferenceProcessor interface
  public void processReference(int objref) {
    if (objref >= 0) {
      ElementInfo ei = heap.get(objref);
      if (!ei.isMarked()) { // only add objects once
        ei.setMarked();
        refQueue.add(ei);
      }
    }

    buf.add(objref);
  }

  
  protected void processArrayFields (ArrayFields afields){
    buf.add(afields.arrayLength());

    if (afields.isReferenceArray()) {
      int[] values = afields.asReferenceArray();
      for (int i = 0; i < values.length; i++) {
        processReference(values[i]);
      }
    } else {
      afields.appendTo(buf);
    }
  }
    
  protected void processNamedFields (ClassInfo ci, Fields fields){
    FinalBitSet filtered = getInstanceFilterMask(ci);
    FinalBitSet refs = getInstanceRefMask(ci);

    // using a block operation probably doesn't buy us much here since
    // we would have to blank the filtered slots and then visit the
    // non-filtered reference slots, i.e. do two iterations over
    // the mask bit sets
    int[] values = fields.asFieldSlots();
    for (int i = 0; i < values.length; i++) {
      if (!filtered.get(i)) {
        int v = values[i];
        if (refs.get(i)) {
          processReference(v);
        } else {
          buf.add(v);
        }
      }
    }
  }

  // needs to be public because of ElementInfoProcessor interface
  // NOTE: that we don't serialize the monitor state here since this is
  // redundant to the thread locking state (which we will do after the heap).
  // <2do> we don't strictly need the lockCount since this has to show in the
  // stack frames. However, we should probably add monitor serialization to
  // better support specialized subclasses
  public void processElementInfo(ElementInfo ei) {
    Fields fields = ei.getFields();
    ClassInfo ci = ei.getClassInfo();
    buf.add(ci.getUniqueId());

    if (fields instanceof ArrayFields) { // not filtered
      processArrayFields((ArrayFields)fields);

    } else { // named fields, filtered
      processNamedFields(ci, fields);
    }
  }
  

  protected void processReferenceQueue () {
    refQueue.process(this);
    
    // this sucks, but we can'ti do the 'isMarkedOrLive' trick used in gc here
    // because gc depends on live bit integrity, and we only mark non-filtered live
    // objects here, i.e. we can'ti just set the Heap liveBitValue subsequently.
    heap.unmarkAll();
  }

  protected void serializeStackFrames() {
    ThreadList tl = ks.getThreadList();

    for (ThreadInfo ti : tl) {
      if (ti.isAlive()) {
        serializeStackFrames(ti);
      }
    }
  }

  protected void serializeStackFrames(ThreadInfo ti){
    // we need to add the thread object itself as a root
    processReference( ti.getThreadObjectRef());
    
    for (StackFrame frame = ti.getTopFrame(); frame != null; frame = frame.getPrevious()){
      serializeFrame(frame);
    }
  }

  /** more generic, but less efficient because it can't use block operations
  protected void _serializeFrame(StackFrame frame){
    buf.add(frame.getMethodInfo().getGlobalId());
    buf.add(frame.getPC().getInstructionIndex());

    int len = frame.getTopPos()+1;
    buf.add(len);

    // this looks like something we can push into the frame
    int[] slots = frame.getSlots();
    for (int i = 0; i < len; i++) {
      if (frame.isReferenceSlot(i)) {
        processReference(slots[i]);
      } else {
        buf.add(slots[i]);
      }
    }
  }
  **/

  protected void serializeFrame(StackFrame frame){
    buf.add(frame.getMethodInfo().getGlobalId());

    // there can be (rare) cases where a listener sets a null nextPc in
    // a frame that is still on the stack
    Instruction pc = frame.getPC();
    if (pc != null){
      buf.add(pc.getInstructionIndex());
    } else {
      buf.add(-1);
    }

    int len = frame.getTopPos()+1;
    buf.add(len);

    int[] slots = frame.getSlots();
    buf.append(slots,0,len);

    frame.visitReferenceSlots(this);
  }

  // this is called after the heap got serialized, i.e. we should not use
  // processReference() anymore. 
  protected void serializeThreadState (ThreadInfo ti){
    
    buf.add( ti.getId());
    buf.add( ti.getState().ordinal());
    buf.add( ti.getStackDepth());
    
    //--- the lock state
    // NOTE: both lockRef and lockedObjects can only refer to live objects
    // which are already heap-processed at this point (i.e. have a valid 'sid'
    // in case we don't want to directly serialize the reference values)
    
    // the object we are waiting for 
    ElementInfo eiLock = ti.getLockObject();
    if (eiLock != null){
      buf.add(getSerializedReferenceValue( eiLock));
    }
    
    // the objects we hold locks for
    // NOTE: this should be independent of lockedObjects order, hence we
    // have to factor this out
    serializeLockedObjects( ti.getLockedObjects());
  }

  // NOTE: this should not be called before all live references have been processed
  protected int getSerializedReferenceValue (ElementInfo ei){
    return ei.getObjectRef();
  }
  
  protected void serializeLockedObjects(List<ElementInfo> lockedObjects){
    // lockedObjects are already a set since we don't have multiple entries
    // (that would just increase the lock count), but our serialization should
    // NOT produce different values depending on order of entry. We could achieve this by using
    // a canonical order (based on reference or sid values), but this would require
    // System.arraycopys and object allocation, which is too much overhead
    // given that the number of lockedObjects is small for all but the most
    // pathological systems under test. 
    // We could spend all day to compute the perfect order-independent hash function,
    // but since our StateSet isn't guaranteed to be collision free anyway, we
    // rather shoot for something that can be nicely JITed

    int n = lockedObjects.size();
    buf.add(n);
    
    if (n > 0){
      if (n == 1){ // no order involved
        buf.add( getSerializedReferenceValue( lockedObjects.get(0)));
        
      } else {
        // don't burn an iterator on this, 'n' is supposed to be small
        int h = (n << 16) + (n % 3);
        for (int i=0; i<n; i++){
          int rot = (getSerializedReferenceValue( lockedObjects.get(i))) % 31;
          h ^= (h << rot) | (h >>> (32 - rot)); // rotate left
        }        
        buf.add( h);
      }
    }
  }
  
  protected void serializeThreadStates (){
    ThreadList tl = ks.getThreadList();

    for (ThreadInfo ti : tl) {
      if (ti.isAlive()) {
        serializeThreadState(ti);
      }
    }    
  }
  
  protected void serializeStatics(){
    StaticArea statics = ks.getStaticArea();
    buf.add(statics.getLength());

    for (StaticElementInfo sei : statics) {
      serializeClass(sei);
    }
  }

  protected void serializeClass (StaticElementInfo sei){
    buf.add(sei.getStatus());

    Fields fields = sei.getFields();
    ClassInfo ci = sei.getClassInfo();
    FinalBitSet filtered = getStaticFilterMask(ci);
    FinalBitSet refs = getStaticRefMask(ci);
    int max = ci.getStaticDataSize();
    for (int i = 0; i < max; i++) {
      if (!filtered.get(i)) {
        int v = fields.getIntValue(i);
        if (refs.get(i)) {
          processReference(v);
        } else {
          buf.add(v);
        }
      }
    }
  }
  
  //--- our main purpose in life

  @Override
  protected int[] computeStoringData() {

    buf.clear();
    heap = ks.getHeap();
    initReferenceQueue();

    //--- serialize all live objects and loaded classes
    serializeStackFrames();
    serializeStatics();
    processReferenceQueue();
    
    //--- now serialize the thread states (which might refer to live objects)
    // we do this last because threads contain some internal references
    // (locked objects etc) that should NOT set the canonical reference serialization
    // values (if they are encountered before their first explicit heap reference)
    serializeThreadStates();

    return buf.toArray();
  }

}