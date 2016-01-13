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

import gov.nasa.jpf.JPFException;

import java.util.Iterator;

/**
 * a hash map that holds int values
 * this is a straight forward linked list hashmap
 *
 * note: this does deep copy clones, which can be quite expensive
 */
public final class IntTable<E> implements Iterable<IntTable.Entry<E>>, Cloneable{
  static final int INIT_TBL_POW = 7;
  static final double MAX_LOAD = 0.80;
  
  protected ObjArray<Entry<E>> table;
  protected int tblPow;       // = log_2(table.length)
  protected int mask;         // = table.length - 1
  protected int nextRehash;   // = ceil(MAX_LOAD * table.length);
  protected int size;         // number of Entry<E> objects reachable from table
  
  protected Entry<E> nullEntry = null;
  
  
  public IntTable() {
    this(INIT_TBL_POW);
  }
  
  public IntTable(int pow) {
    newTable(pow);
    size = 0;
  }

  // this is a deep copy (needs to be because entries are reused when growing the table)
  public IntTable<E> clone() {
    try {
      IntTable t = (IntTable)super.clone();
      ObjArray<Entry<E>> tbl = table.clone();
      t.table = tbl;

      // clone entries
      int len = tbl.length();
      for (int i=0; i<len; i++){
        Entry<E> eFirst = tbl.get(i);
        if (eFirst != null){
          eFirst = eFirst.clone();
          Entry<E> ePrev = eFirst;
          for (Entry<E> e = eFirst.next; e != null; e = e.next){
            e = e.clone();
            ePrev.next = e;
            ePrev = e;
          }
          tbl.set(i, eFirst);
        }
      }

      return t;

    } catch (CloneNotSupportedException cnsx){
      throw new JPFException("clone failed");
    }
  }

  protected void newTable(int pow) {
    tblPow = pow;
    table = new ObjArray<Entry<E>>(1 << tblPow);
    mask = table.length() - 1;
    nextRehash = (int) Math.ceil(MAX_LOAD * table.length());
  }
  
  protected int getTableIndex(E key) {
    int hc = key.hashCode();
    int ret = hc ^ 786668707;
    ret += (hc >>> tblPow);
    return (ret ^ 1558394450) & mask;
  }

  protected boolean maybeRehash() {
    if (size < nextRehash) return false;
    ObjArray<Entry<E>> old = table;
    newTable(tblPow + 1);
    int len = old.length();
    for (int i = 0; i < len; i++) {
      addList(old.get(i));
    }
    return true;
  }
  
  private void addList(Entry<E> e) {
    Entry<E> cur = e;
    while (cur != null) {
      Entry<E> tmp = cur;
      cur = cur.next;
      doAdd(tmp, getTableIndex(tmp.key));
    }
  }
  
  // helper for adding
  private void doAdd(Entry<E> e, int idx) {
    e.next = table.get(idx);
    table.set(idx, e);
  }
  
  // helper for searching
  protected Entry<E> getHelper(E key, int idx) {
    Entry<E> cur = table.get(idx);
    while (cur != null) {
      if (cur.key.equals(key)) {
        return cur;
      }
      cur = cur.next;
    }
    return null; // not found
  }

  
  /* ===============  PUBLIC METHODS =============== */
  
  /** returns number of bindings in the table. */
  public int size() { return size; }
  
  /** ONLY USE IF YOU ARE SURE NO PREVIOUS BINDING FOR key EXISTS. */
  public void add(E key, int val) {
    Entry<E> e = new Entry<E>(key,val);
    if (key == null) {
      nullEntry = e;
    } else {
      maybeRehash();
      doAdd(e, getTableIndex(key));
    }
    size++;
  }
  
  /** lookup, returning null if no binding. */
  public Entry<E> get(E key) {
    return getHelper(key, getTableIndex(key));
  }
  
  /** just like HashMap put. */
  public void put(E key, int val) {
    if (key == null) {
      if (nullEntry == null) {
        nullEntry = new Entry<E>(null,val);
        size++;
      } else {
        nullEntry.val = val;
      }
      return;
    }
    
    int idx = getTableIndex(key);
    Entry<E> e = getHelper(key, idx);
    if (e == null) {
      if (maybeRehash()){
        idx = getTableIndex(key);
      }
      doAdd(new Entry<E>(key,val), idx);
      size++;
    } else {
      e.val = val;
    }
  }

  /** removes a binding/entry from the table. */
  public Entry<E> remove(E key) {
    int idx = getTableIndex(key);
    Entry<E> prev = null;
    Entry<E> cur = table.get(idx);
    while (cur != null) {
      if (cur.key.equals(key)) {
        if (prev == null) {
          table.set(idx, cur.next);
        } else {
          prev.next = cur.next;
        }
        cur.next = null;
        size--;
        return cur;
      }
      prev = cur;
      cur = cur.next;
    }
    return null; // not found
  }
  
  
  /** empties the table, leaving it capacity the same. */
  public void clear() {
    table.nullify();
    nullEntry = null;
    size = 0;
  }
  
  /** returns the next val to be assigned by a call to pool() on a fresh key. */
  public int nextPoolVal() { return size; }
  
  /** gets the Entry associated with key, adding previous `size' if not yet bound. */
  public Entry<E> pool(E key) {
    if (key == null) {
      if (nullEntry == null) {
        nullEntry = new Entry<E>(null,size);
        size++;
      }
      return nullEntry;
    }
    
    int idx = getTableIndex(key);
    Entry<E> e = getHelper(key, idx);
    if (e == null) {
      if (maybeRehash()) {
        idx = getTableIndex(key);
      }
      e = new Entry<E>(key,size);
      doAdd(e, idx);
      size++;
    }
    return e;
  }
  
  /** shorthand for <code>pool(key).val</code>. */
  public int poolIndex(E key) {
    return pool(key).val;
  }
  
  /** shorthand for <code>pool(key).key</code>. */
  public E poolKey(E key) {
    return pool(key).key;
  }
  
  /** shorthand for <code>get(key) != null</code>. */
  public boolean hasEntry(E key) {
    return get(key) != null;
  }
  
  /**
   * encapsulates an Entry in the table.  changes to val will be reflected
   * in the table.
   */  
  public static class Entry<E> implements Cloneable {
    public    final E  key;
    public    int      val;
    protected Entry<E> next;
    
    protected Entry(E k, int v) { key = k; val = v; next = null; }
    protected Entry(E k, int v, Entry<E> n) { key = k; val = v; next = n; }

    public Entry<E> clone() {
      try {
        return (Entry<E>)super.clone();
      } catch (CloneNotSupportedException x){
        throw new JPFException("clone failed");
      }
    }

    public String toString() {
      return key.toString() + " => " + val;
    }
  }

  /**
   * returns an iterator over the entries.  unpredictable behavior could result if
   * using iterator after table is altered.
   */
  public Iterator<Entry<E>> iterator () {
    return new TblIterator();
  }

  protected class TblIterator implements Iterator<Entry<E>> {
    int idx;
    Entry<E> cur;

    public TblIterator() {
      idx = -1; cur = null;
      advance();
    }
    
    void advance() {
      if (cur != null) {
        cur = cur.next;
      }
      int len = table.length();
      while (idx < len && cur == null) {
        idx++;
        if (idx < len) {
          cur = table.get(idx);
        }
      }
    }
    
    public boolean hasNext () {
      return idx < table.length();
    }

    public Entry<E> next () {
      Entry<E> e = cur;
      advance();
      return e;
    }

    public void remove () { throw new UnsupportedOperationException(); }
    
  }
  
  public static void main(String[] args) {
	  IntTable<String> tbl = new IntTable<String>(4);
	  int max = Integer.parseInt(args[0]);
	  int i;

	  for (i = 0; i < max; i++) {
		  tbl.add("foo" + i, i + 3);
	  }
	  
	  for (i = 0; i < max; i++) {
		  if (tbl.get("foo" + i).val != i + 3) {
			  throw new RuntimeException();
		  }
	  }
  }
}
