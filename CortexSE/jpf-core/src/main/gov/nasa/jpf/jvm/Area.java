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

import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.ObjVector;

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Area defines a memory class. An area can be used for objects
 * in the DynamicArea (heap) or classes in the StaticArea (classinfo with
 * static fields).
 */
public abstract class Area<EI extends ElementInfo> implements Iterable<EI> {

  /**
   * Contains the information for each element.
   */
  protected ObjVector<EI> elements;

  /**
   * The number of elements. This is the number of non-null
   * refs in the array, which can differ from the size of
   * the array and can differ from lastElement+1.
   */
  protected int nElements;

  /**
   * Reference of the kernel state this dynamic area is in.
   */
  public final KernelState ks;

  /**
   * Set of bits used to see which elements have changed.
   */
  protected final BitSet hasChanged;

  // our default memento implementation
  static abstract class AreaMemento<A extends Area>  {
    Memento<ElementInfo>[] liveEI;

    public AreaMemento (A area){
      int len = area.size();
      Memento<ElementInfo>[] a = new Memento[len];

      int i=0;
      // it actually makes sense to use the elements iterator at this point
      // since this happens after gc, i.e. there is a good chance that the
      // area got fragmented again
      for (ElementInfo ei : (Iterable<ElementInfo>)area.elements()){
        Memento<ElementInfo> m = null;
        if (!ei.hasChanged()){
          m = ei.cachedMemento;
        }
        if (m == null){
          m = ei.getMemento();
          ei.cachedMemento = m;
        }
        a[i++] = m;
      }

      area.markUnchanged();
      liveEI = a;
    }

    public A restore(A area) {
      ObjVector<ElementInfo> e = area.elements;
      Memento<ElementInfo>[] a = liveEI;
      int len = a.length;

      area.resetVolatiles();

      int index = -1;
      int lastIndex = -1;
      for (int i=0; i<len; i++){
        Memento<ElementInfo> m = a[i];
        // ElementInfo mementos are Softreferences, so we don't need to get the
        // restore-objRef upfront in order to retrieve the right inSitu object
        ElementInfo ei = m.restore(null);

        index = ei.getObjectRef();

        area.removeRange(lastIndex+1, index);
        lastIndex = index;

        ei.cachedMemento = m;

        // can't call elements.set() directly because our concrete area
        // might have to do its own housekeeping (e.g. update bitsets)
        area.set(index,ei);
      }

      if (index >= 0){
        area.removeAllFrom(index+1);
      }

      area.nElements = len;
      area.restoreVolatiles();
      area.markUnchanged();

      return area;
    }
  }

  /**
   * support for iterators that return all allocated objects, so that
   * clients don't have to know our concrete implementation
   *
   * sometimes it sucks not having variant generics
   */

  protected class ElementInfoIterator implements Iterator<ElementInfo>, Iterable<ElementInfo> {
    int index, visited;

    public void remove() {
      throw new UnsupportedOperationException ("illegal operation, only GC can remove objects");
    }

    public boolean hasNext() {
      return (index < elements.size()) && (visited < nElements);
    }

    public ElementInfo next() {
      int len = elements.size();
      for (; index < len; index++) {
        EI ei = elements.get(index);
        if (ei != null) {
          index++; visited++;
          return ei;
        }
      }

      throw new NoSuchElementException();
    }

    public Iterator<ElementInfo> iterator() {
      return this;
    }
  }

  protected class MarkedElementInfoIterator implements Iterator<ElementInfo>, Iterable<ElementInfo> {
    int index;

    MarkedElementInfoIterator() {
      index = getNextMarked(0);
    }

    public boolean hasNext() {
      return (index >= 0);
    }

    public ElementInfo next() {
      if (index >= 0){
        int i = index;
        index = getNextMarked(index + 1);
        return elements.get(i);
      } else {
        throw new NoSuchElementException();
      }
    }

    public Iterator<ElementInfo> iterator() {
      return this;
    }

    public void remove() {
      throw new UnsupportedOperationException ("illegal operation, only GC can remove objects");
    }
  }


  public Iterable<EI> elements() {
    return elements.elements();
  }

  /**
   * extended iterator that just returns changed elements entries. Note that next()
   * can return 'null' elements, in which case we can to query the current ref value
   * with getLastRef()
   */
  public class ChangedIterator implements java.util.Iterator<EI> {
    int i = getNextChanged(0);
    int ref = -1;

    public void remove() {
      throw new UnsupportedOperationException ("illegal operation, only GC can remove objects");
    }

    public boolean hasNext() {
      return (i >= 0);
    }

    public EI next() {
      if (i >= 0){
        EI ei = elements.get(i);
        ref = i;
        i = getNextChanged(i+1);
        return ei;

      } else {
        throw new NoSuchElementException();
      }
    }

    /**
     * this returns the objRef of the last element returned by next()
     */
    public int getLastRef() {
      return ref;
    }
  }

  // this one answers the changed reference values (ints).
  // It's not a standard java.util.EIiterator because we don't want to box ints
  public class ChangedReferenceIterator implements gov.nasa.jpf.util.IntIterator {
    int i = getNextChanged(0);

    public void remove() {
      throw new UnsupportedOperationException ("illegal operation, only GC can remove objects");
    }

    public boolean hasNext() {
      return (i >= 0);
    }

    public int next() {
      if (i >= 0){
        int ref = i;
        i = getNextChanged(i+1);
        return ref;

      } else {
        throw new NoSuchElementException();
      }
    }
  }

  public Area (KernelState ks) {
    this.ks = ks;
    elements = new ObjVector<EI>(1024);
    nElements = 0;
    hasChanged = new BitSet();
  }


  public Iterator<EI> iterator() {
    return elements.nonNullIterator();
  }

  public ChangedIterator changedIterator() {
    return new ChangedIterator();
  }

  public ChangedReferenceIterator changedReferenceIterator() {
    return new ChangedReferenceIterator();
  }

  public int numberOfChanged() {
    return hasChanged.cardinality();
  }


  /**
   * reset any information that has to be re-computed in a backtrack
   * (objRef.e. hasn't been stored explicitly)
   */
  public void resetVolatiles () {
    // nothing yet
  }

  public void restoreVolatiles () {
    // nothing to do
  }

  public void cleanUpDanglingReferences (Heap heap) {
    ThreadInfo ti = ThreadInfo.getCurrentThread();
    int tid = ti.getId();
    boolean isThreadTermination = ti.isTerminated();
    
    for (ElementInfo e : this) {
      if (e != null) {
        e.cleanUp(heap, isThreadTermination, tid);
      }
    }
  }

  public int size () {
    return nElements;
  }

  int getNextMarked(int fromIndex) {
    int len = elements.size();
    for (int i = fromIndex; i < len; i++) {
      EI ei = elements.get(i);
      if (ei != null && ei.isMarked()) {
        return i;
      }
    }
    return -1;
  }

  public void unmarkAll() {
    int len = elements.size();
    for (int i = 0; i < len; i++) {
      EI ei = elements.get(i);
      if (ei != null && ei.isMarked()) {
        ei.setUnmarked();
      }
    }
  }
  
  public int getNextChanged (int startIdx) {
    return hasChanged.nextSetBit(startIdx);
  }

  public EI get (int index) {
    if (index < 0) {
      return null;
    } else {
      return elements.get(index);
    }
  }

  public EI ensureAndGet(int index) {
    EI ei = elements.get(index);
    if (ei == null) {      
      ei = createElementInfo();

      ei.resurrect(this, index);
      
      elements.set(index, ei);
      nElements++;
    }
    return ei;
  }

  public int getLength() {
    return elements.size();
  }

  public void hash (HashData hd) {
    int length = elements.size();

    for (int i = 0; i < length; i++) {
      EI ei = elements.get(i);
      if (ei != null) {
        ei.hash(hd);
      }
    }
  }

  public int hashCode () {
    HashData hd = new HashData();

    hash(hd);

    return hd.getValue();
  }

  public void removeAll() { 
    elements.clear();
    nElements = 0;
  }

  // this is called during restoration, we don't have to mark changes
  public void removeAllFrom (int idx) {
    int n = elements.removeFrom(idx);
    nElements -= n;
  }

  public void removeRange( int fromIdx, int toIdx){
    int n = elements.removeRange(fromIdx, toIdx);
    nElements -= n;
  }

  public String toString () {
    return getClass().getName() + "@" + super.hashCode();
  }

  // BUG! nElements is not consistent with the elements array length
  // somtimes it seems to be bigger
  // UPDATE: fixed? -pcd
  protected void add (int index, EI e) {
    e.setObjectRef(index);

    assert (elements.get(index) == null) :
      "trying to overwrite non-null object: " + elements.get(index) + " with: " + e;

    nElements++;
    elements.set(index,e);
    markChanged(index);
  }

  // for Restorer use only (EI is already properly initialized)
  protected void set (int index, EI e) {
    e.setObjectRef(index);
    elements.set(index,e);
  }


  public void markChanged (int index) {
    hasChanged.set(index);
    ks.changed();
  }

  public void markUnchanged() {
    hasChanged.clear();
  }

  public boolean anyChanged() {
    return !hasChanged.isEmpty();
  }

  public boolean hasChanged(int index){
    return hasChanged.get(index);
  }

  protected void remove (int index, boolean nullOk) {
    EI ei = elements.get(index);

    if (nullOk && ei == null) return;

    assert (ei != null) : "trying to remove null object at index: " + index;

    if (ei.recycle()) {
      elements.set(index, null);
      elements.squeeze();
      nElements--;
      markChanged(index);
    }
  }

  abstract EI createElementInfo ();
}
