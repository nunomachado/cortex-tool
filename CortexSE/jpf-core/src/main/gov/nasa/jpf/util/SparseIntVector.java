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
package gov.nasa.jpf.util;

import static java.lang.Integer.MIN_VALUE;

import java.util.Arrays;

/**
 * This has approximately the interface of IntVector but uses a hash table
 * instead of an array.  Also, does not require allocation with each add.
 * Configurable default value. 
 */
public class SparseIntVector {
  private static final boolean DEBUG = false;
  
  static final double MAX_LOAD_WIPE = 0.6;
  static final double MAX_LOAD_REHASH = 0.4;
  static final int DEFAULT_POW = 10;
  static final int DEFAULT_VAL = 0;

  int[] idxTable;  // MIN_VALUE => unoccupied
  int[] valTable;  // can be bound to null
  
  int count;
  int pow;
  int mask;
  int nextWipe;
  int nextRehash;
  
  int defaultValue;
  
  /**
   * Creates a SimplePool that holds about 716 elements before first
   * rehash.
   */
  public SparseIntVector() {
    this(DEFAULT_POW,DEFAULT_VAL);
  }
  
  /**
   * Creates a SimplePool that holds about 0.7 * 2**pow elements before
   * first rehash.
   */
  public SparseIntVector(int pow, int defValue) {
    this.pow = pow;
    newTable();
    count = 0;
    mask = valTable.length - 1;
    nextWipe = (int)(MAX_LOAD_WIPE * mask);
    nextRehash = (int)(MAX_LOAD_REHASH * mask);
    defaultValue = defValue;
  }  
  
  // INTERNAL //
  
  @SuppressWarnings("unchecked")
  protected void newTable() {
    valTable = new int[1 << pow];
    idxTable = new int[1 << pow];
    if (defaultValue != 0) {
      Arrays.fill(valTable, defaultValue);
    }
    Arrays.fill(idxTable, MIN_VALUE);
  }
  
  protected int mix(int x) {
    int y = 0x9e3779b9;
    x ^= 0x510fb60d;
    y += (x >> 8) + (x << 3);
    x ^= (y >> 5) + (y << 2);
    return y - x;
  }
  
  
  // ********************* Public API ******************** //

  public void clear() {
    Arrays.fill(valTable, defaultValue);
    Arrays.fill(idxTable, MIN_VALUE);
    count = 0;
  }
  
  @SuppressWarnings("unchecked")
  public int get(int idx) {
    int code = mix(idx);
    int pos = code & mask;
    int delta = (code >> (pow - 1)) | 1; // must be odd!
    int oidx = pos;

    for(;;) {
      int tidx = idxTable[pos];
      if (tidx == MIN_VALUE) {
        return defaultValue;
      }
      if (tidx == idx) {
        return valTable[pos];
      }
      pos = (pos + delta) & mask;
      assert (pos != oidx); // should never wrap around
    }
  }

  public void set(int idx, int val) {
    int code = mix(idx);
    int pos = code & mask;
    int delta = (code >> (pow - 1)) | 1; // must be odd!
    int oidx = pos;

    for(;;) {
      int tidx = idxTable[pos];
      if (tidx == MIN_VALUE) {
        break;
      }
      if (tidx == idx) {
        valTable[pos] = val; // update
        return;            // and we're done
      }
      pos = (pos + delta) & mask;
      assert (pos != oidx); // should never wrap around
    }
    // idx not in table; add it
    
    count++;
    if (count >= nextWipe) { // too full
      // determine if size needs to be increased or just wipe null blocks
      int oldCount = count;
      count = 0;
      for (int i = 0; i < idxTable.length; i++) {
        if (idxTable[i] != MIN_VALUE && valTable[i] != defaultValue) {
          count++;
        }
      }
      if (count >= nextRehash) {
        pow++; // needs to be increased in size
        if (DEBUG) {
          System.out.println("Rehash to capacity: 2**" + pow);
        }
      } else {
        if (DEBUG) {
          System.out.println("Rehash reclaiming this many nulls: " + (oldCount - count));
        }
      }
      int[] oldValTable = valTable;
      int[] oldIdxTable = idxTable;
      newTable();
      mask = idxTable.length - 1;
      nextWipe = (int)(MAX_LOAD_WIPE * mask);
      nextRehash = (int)(MAX_LOAD_REHASH * mask);

      int oldLen = oldIdxTable.length;
      for (int i = 0; i < oldLen; i++) {
        int tidx = oldIdxTable[i];
        if (tidx == MIN_VALUE) continue;
        int o = oldValTable[i];
        if (o == defaultValue) continue;
        // otherwise:
        code = mix(tidx);
        pos = code & mask;
        delta = (code >> (pow - 1)) | 1; // must be odd!
        while (idxTable[pos] != MIN_VALUE) { // we know enough slots exist
          pos = (pos + delta) & mask;
        }
        idxTable[pos] = tidx;
        valTable[pos] = o;
      }
      // done with rehash; now get idx to empty slot
      code = mix(idx);
      pos = code & mask;
      delta = (code >> (pow - 1)) | 1; // must be odd!
      while (idxTable[pos] != MIN_VALUE) { // we know enough slots exist
        pos = (pos + delta) & mask;
      }
    } else {
      // pos already pointing to empty slot
    }

    idxTable[pos] = idx;
    valTable[pos] = val;
  }
  
  
  // ************************** Test main ************************ //
  
  public static void main(String[] args) {
    SparseIntVector vect = new SparseIntVector(3, MIN_VALUE);
    
    // add some
    for (int i = -4200; i < 4200; i += 10) {
      vect.set(i, i);
    }
    
    // check for added & non-added
    for (int i = -4200; i < 4200; i += 10) {
      int v = vect.get(i);
      if (v != i) {
        throw new IllegalStateException();
      }
    }
    for (int i = -4205; i < 4200; i += 10) {
      int v = vect.get(i);
      if (v != MIN_VALUE) {
        throw new IllegalStateException();
      }
    }
    
    // add some more
    for (int i = -4201; i < 4200; i += 10) {
      vect.set(i, i);
    }

    // check all added
    for (int i = -4200; i < 4200; i += 10) {
      int v = vect.get(i);
      if (v != i) {
        throw new IllegalStateException();
      }
    }
    for (int i = -4201; i < 4200; i += 10) {
      int v = vect.get(i);
      if (v != i) {
        throw new IllegalStateException();
      }
    }
    
    // "remove" some
    for (int i = -4200; i < 4200; i += 10) {
      vect.set(i,MIN_VALUE);
    }
    
    // check for added & non-added
    for (int i = -4201; i < 4200; i += 10) {
      int v = vect.get(i);
      if (v != i) {
        throw new IllegalStateException();
      }
    }
    for (int i = -4200; i < 4200; i += 10) {
      int v = vect.get(i);
      if (v != MIN_VALUE) {
        throw new IllegalStateException();
      }
    }

    // add even more
    for (int i = -4203; i < 4200; i += 10) {
      vect.set(i, i);
    }
    for (int i = -4204; i < 4200; i += 10) {
      vect.set(i, i);
    }
  }
}
