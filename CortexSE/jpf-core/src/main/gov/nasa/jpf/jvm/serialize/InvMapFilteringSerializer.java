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

package gov.nasa.jpf.jvm.serialize;

import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.util.IntVector;
import gov.nasa.jpf.util.SparseIntVector;

/**
 * alternative canonicalizing & filtering serializer that uses ElementInfo
 * external memory to canonicalize. This is less efficient than the
 * CanonicalizingFilteringSerializer, but does not depend on ElementInfo consistency
 */
public class InvMapFilteringSerializer extends FilteringSerializer {

  // this stores the number in which object references are traversed, not
  // the reference value itself, which provides some additional heap symmetry.
  // NOTE - the problem is that it assumes we can allocate an array/map that is
  // large enough to hold all possible reference values. This is true in the
  // case of DynamicArea (which also stores elements in a single vector), but
  // not for the SparseClusterArrayHeap. Using a SparseIntVector can help to
  // some extent, but doesn't scale well in terms of total number of serialized
  // objects since (it requires ~ 1.5 times more entries to avoid frequent collisions)

  protected SparseIntVector heapMap = new SparseIntVector(14,0);
  //protected transient IntVector heapMap = new IntVector(4096);

  // invHeapMap is a dense array of all encountered live and non-filtered objects
  protected transient IntVector invHeapMap = new IntVector(4096);

  @Override
  protected void initReferenceQueue() {
    heapMap.clear();
    invHeapMap.clear();

    // add something so that we can tell if get() => 0 means "not seen yet", or just object 0
    invHeapMap.add(-1);
  }

  @Override
  public void processReference(int objref) {
    if (objref < 0) {
      buf.add(-1);

    } else {
      int idx = heapMap.get(objref);
      if (idx == 0) {  // this got to be "not seen yet", since we start to store from invHeapMap size 1
        idx = invHeapMap.size();
        invHeapMap.add(objref);
        heapMap.set(objref, idx);
      }
      buf.add(idx);
    }
  }

  @Override
  protected void processReferenceQueue() {
    int len = invHeapMap.size();
    for (int i=1; i<invHeapMap.size(); i++){
      int objref = invHeapMap.get(i);
      ElementInfo ei = heap.get(objref);
      processElementInfo(ei);
    }
  }
}