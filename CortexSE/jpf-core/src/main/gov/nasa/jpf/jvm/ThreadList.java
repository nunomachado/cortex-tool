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
import gov.nasa.jpf.util.HashData;

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Contains the list of all ThreadInfos for live java.lang.Thread objects
 * 
 * We add a thread upon creation (within the ThreadInfo ctor), and remove it
 * when the corresponding java.lang.Thread object gets recycled by JPF. This means
 * that:
 *   * the thread list can contain terminated threads
 *   * terminated and recycled threads are (eventually) removed from the list
 *   * the list can shrink along a given path
 *   * thread ids don't have to correspond with storing order !!
 *   * thread ids can be re-used
 *
 * Per default, thread ids are re-used in order to be packed (which is required to efficiently
 * keep track of referencing threads in ElementInfo reftids). If there is a need
 * to avoid recycled thread ids, set 'vm.reuse_tid=false'.
 * 
 * NOTE - this ThreadList implementation doubles up as a thread object -> ThreadInfo
 * map, which is for instance heavily used by the JPF_java_lang_Thread peer.
 * 
 * This implies that ThreadList is still not fully re-organized in case something
 * keeps terminated thread objects alive. We could avoid this by having a separate
 * map for live threads<->ThreadInfos, but this would also have to be a backrackable
 * container that is highly redundant to ThreadList (the only difference being
 * that terminated threads could be removed from ThreadList).
 * 
 */
public class ThreadList implements Cloneable, Iterable<ThreadInfo>, Restorable<ThreadList> {

  public static class Count {
    public final int alive;
    public final int runnableNonDaemons;
    public final int runnableDaemons;
    public final int blocked;
    
    Count (int alive, int runnableNonDaemons, int runnableDaemons, int blocked){
      this.alive = alive;
      this.runnableNonDaemons = runnableNonDaemons;
      this.runnableDaemons = runnableDaemons;
      this.blocked = blocked;
    }
  }
  
  protected boolean reuseTid;
  
  // ThreadInfos for all created but not terminated threads
  protected ThreadInfo[] threads;
  
  // the highest ID created so far along this path
  protected int maxTid;


  static class TListMemento implements Memento<ThreadList> {
    // note that we don't clone/deepcopy ThreadInfos
    Memento<ThreadInfo>[] tiMementos;
    int maxTid;

    TListMemento(ThreadList tl) {
      ThreadInfo[] threads = tl.threads;
      int len = threads.length;

      maxTid = tl.maxTid;
      tiMementos = new Memento[len];
      for (int i=0; i<len; i++){
        ThreadInfo ti = threads[i];
        Memento<ThreadInfo> m = null;

        if (!ti.hasChanged()){
          m = ti.cachedMemento;
        }
        if (m == null){
          m = ti.getMemento();
          ti.cachedMemento = m;
        }
        tiMementos[i] = m;
      }
    }

    public ThreadList restore(ThreadList tl){
      int len = tiMementos.length;
      ThreadInfo[] threads = new ThreadInfo[len];
      for (int i=0; i<len; i++){
        Memento<ThreadInfo> m = tiMementos[i];
        ThreadInfo ti = m.restore(null);
        ti.cachedMemento = m;
        threads[i] = ti;
      }
      tl.threads = threads;
      tl.maxTid = maxTid;

      return tl;
    }
  }


  protected ThreadList() {
    // nothing here
  }

  /**
   * Creates a new empty thread list.
   */
  public ThreadList (Config config, KernelState ks) {
    threads = new ThreadInfo[0];
    
    reuseTid = config.getBoolean("vm.reuse_tid", false);
  }

  public Memento<ThreadList> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }
  public Memento<ThreadList> getMemento(){
    return new TListMemento(this);
  }

  public Object clone() {
    ThreadList other = new ThreadList();
    other.threads = new ThreadInfo[threads.length];

    for (int i=0; i<threads.length; i++) {
      other.threads[i] = (ThreadInfo) threads[i].clone();
    }

    return other;
  }

  /**
   * add a new ThreadInfo if it isn't already in the list.
   * Note: the returned id is NOT our internal storage index
   * 
   * @return (path specific) Thread id
   */
  public int add (ThreadInfo ti) {
    int n = threads.length;

    BitSet ids = new BitSet();   
    for (int i=0; i<n; i++) {
      ThreadInfo t = threads[i];
      if (t == ti) {
        return t.getId();
      }
      
      ids.set( t.getId());
    }

    // append it
    ThreadInfo[] newThreads = new ThreadInfo[n+1];
    System.arraycopy(threads, 0, newThreads, 0, n);
    newThreads[n] = ti;
    threads = newThreads;
    
    if (reuseTid){
      return ids.nextClearBit(0);
    } else {
      return maxTid++;
    }
  }
  
  public boolean remove (ThreadInfo ti){
    int n = threads.length;
    
    for (int i=0; i<n; i++) {
      if (ti == threads[i]){
        int n1 = n-1;
        ThreadInfo[] newThreads = new ThreadInfo[n1];
        if (i>0){
          System.arraycopy(threads, 0, newThreads, 0, i);
        }
        if (i<n1){
          System.arraycopy(threads, i+1, newThreads, i, (n1-i));
        }
        
        threads = newThreads;        
        return true;
      }
    }
    
    return false;
  }
  
  public boolean hasAnyAliveThread () {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (threads[i].isAlive()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns the array of threads.
   */
  public ThreadInfo[] getThreads() {
    return threads.clone();
  }

  public void hash (HashData hd) {
    for (int i=0; i<threads.length; i++){
      threads[i].hash(hd);
    }
  }
  
  public ThreadInfo getThreadInfoForId (int tid){
    for (int i=0; i<threads.length; i++){
      ThreadInfo ti = threads[i];
      if (ti.getId() == tid){
        return ti;
      }
    }
    
    return null;
  }

  public ThreadInfo getThreadInfoForObjRef (int objRef){
    for (int i=0; i<threads.length; i++){
      ThreadInfo ti = threads[i];
      if (ti.getThreadObjectRef() == objRef){
        return ti;
      }
    }
    
    return null;
  }

  
  /**
   * Returns the length of the list.
   */
  public int length () {
    return threads.length;
  }

  /**
   * Replaces the array of ThreadInfos.
   */
  public void setAll(ThreadInfo[] threads) {
    this.threads = threads;
  }

  public ThreadInfo locate (int objref) {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (threads[i].getThreadObjectRef() == objref) {
        return threads[i];
      }
    }

    return null;
  }

  public void markRoots (Heap heap) {
    for (int i = 0, l = threads.length; i < l; i++) {
      if (threads[i].isAlive()) {
        threads[i].markRoots(heap);
      }
    }
  }

  public Count getCountWithout (ThreadInfo tiExclude){
    int alive=0, runnableNonDaemons=0, runnableDaemons=0, blocked=0;
    
    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
  
      if (ti != tiExclude){
        if (ti.isAlive()) {
          alive++;

          if (ti.isRunnable()) {
            if (ti.isDaemon()) {
              runnableDaemons++;
            } else {
              runnableNonDaemons++;
            }
          } else {
            blocked++;
          }
        }
      }
    }
    
    return new Count(alive, runnableNonDaemons, runnableDaemons, blocked);
  }

  public Count getCount(){
    return getCountWithout(null);
  }
  
  /**
   * return if there are still runnables, and there is at least one
   * non-daemon thread left 
   */
  public boolean hasMoreThreadsToRun() {
    int nonDaemons = 0;
    int runnables = 0;

    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      if (!ti.isDaemon() && !ti.isTerminated()) {
        nonDaemons++;
      }
      if (ti.isTimeoutRunnable()) {
        runnables++;
      }
    }
    
    return (nonDaemons > 0) && (runnables > 0);
  }

  public int getNonDaemonThreadCount () {
    int nd = 0;

    for (int i = 0; i < threads.length; i++) {
      if (!threads[i].isDaemon()) {
        nd++;
      }
    }

    return nd;
  }

  public int getRunnableThreadCount () {
    int n = 0;

    for (int i = 0; i < threads.length; i++) {
      if (threads[i].isTimeoutRunnable()) {
        n++;
      }
    }

    return n;
  }

  public ThreadInfo[] getRunnableThreads() {
    int nRunnable = getRunnableThreadCount();
    ThreadInfo[] list = new ThreadInfo[nRunnable];

    for (int i = 0, j=0; i < threads.length; i++) {
      ThreadInfo t = threads[i];
      if (t.isTimeoutRunnable()) {
        list[j++] = t;
        if (j == nRunnable) {
          break;
        }
      }
    }

    return list;
  }

  public ThreadInfo[] getRunnableThreadsWith (ThreadInfo ti) {
    int nRunnable = getRunnableThreadCount();
    ThreadInfo[] list =  new ThreadInfo[ti.isRunnable() ? nRunnable : nRunnable+1];

    for (int i = 0, j=0; i < threads.length; i++) {
      ThreadInfo t = threads[i];
      if (t.isTimeoutRunnable() || (t == ti)) {
        list[j++] = t;
        if (j == list.length) {
          break;
        }
      }
    }

    return list;
  }

  public ThreadInfo[] getRunnableThreadsWithout( ThreadInfo ti) {
    int nRunnable = getRunnableThreadCount();

    if (ti.isRunnable()) {
      nRunnable--;
    }
    ThreadInfo[] list = new ThreadInfo[nRunnable];

    for (int i = 0, j=0; i < threads.length; i++) {
      ThreadInfo t = threads[i];
      if (t.isTimeoutRunnable() && (ti != t)) {
        list[j++] = t;
        if (j == nRunnable) {
          break;
        }
      }
    }

    return list;
  }


  public int getLiveThreadCount () {
    int n = 0;

    for (int i = 0; i < threads.length; i++) {
      if (threads[i].isAlive()) {
        n++;
      }
    }

    return n;
  }

  boolean hasOtherRunnablesThan (ThreadInfo ti) {
    int n = threads.length;

    for (int i=0; i<n; i++) {
      ThreadInfo t = threads[i];
      if (t != ti) {
        if (t.isRunnable()) {
          return true;
        }
      }
    }

    return false;
  }

  boolean hasOtherNonDaemonRunnablesThan (ThreadInfo ti) {
    int n = threads.length;

    for (int i=0; i<n; i++) {
      ThreadInfo t = threads[i];
      if (t != ti) {
        if (t.isRunnable() && !t.isDaemon()) {
          return true;
        }
      }
    }

    return false;
  }

  boolean hasOnlyDaemonRunnablesOtherThan (ThreadInfo ti){
    int n = threads.length;

    for (int i=0; i<n; i++) {
      ThreadInfo t = threads[i];
      if (t != ti) {
        if (t.isRunnable() && t.isDaemon()) {
          return true;
        }
      }
    }

    return false;
  }
  
  boolean isDeadlocked () {
    boolean hasNonDaemons = false;
    boolean hasBlockedThreads = false;

    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      
      if (ti.isAlive()){
        hasNonDaemons |= !ti.isDaemon();

        // shortcut - if there is at least one runnable, we are not deadlocked
        if (ti.isTimeoutRunnable()) { // willBeRunnable() ?
          return false;
        }

        // means it is not NEW or TERMINATED, i.e. live & blocked
        hasBlockedThreads = true;
      }
    }

    return (hasNonDaemons && hasBlockedThreads);
  }

  public void dump () {
    int i=0;
    for (ThreadInfo t : threads) {
      System.err.println("[" + i++ + "] " + t);
    }
  }

  public Iterator<ThreadInfo> iterator() {
    return new Iterator<ThreadInfo>() {
      int i = 0;

      public boolean hasNext() {
        return threads != null && threads.length>0 && i<threads.length;
      }

      public ThreadInfo next() {
        if (threads != null && threads.length>0 && i<threads.length){
          return threads[i++];
        } else {
          throw new NoSuchElementException();
        }
      }

      public void remove() {
        throw new UnsupportedOperationException("Iterator<ThreadInfo>.remove()");
      }
    };
  }

  
  class CanonicalLiveIterator implements Iterator<ThreadInfo> {
    
    int nextGid = -1;
    int nextIdx = -1;
    
    CanonicalLiveIterator(){
      setNext();
    }
    
    // <2do> not overly efficient, but we assume small thread lists anyways
    void setNext (){
      int lastGid = nextGid;
      int nextGid = Integer.MAX_VALUE;
      int nextIdx = -1;
      
      for (int i=0; i<threads.length; i++){
        ThreadInfo ti = threads[i];
        if (ti.isAlive()){
          int gid = ti.getGlobalId();
          if ((gid > lastGid) && (gid < nextGid)){
            nextGid = gid;
            nextIdx = i;
          }
        }
      }
      
      CanonicalLiveIterator.this.nextGid = nextGid;
      CanonicalLiveIterator.this.nextIdx = nextIdx;
    }
    
    public boolean hasNext() {
      return (nextIdx >= 0);
    }

    public ThreadInfo next() {
      if (nextIdx >= 0){
        ThreadInfo tiNext = threads[nextIdx];
        setNext();
        return tiNext;
        
      } else {
        throw new NoSuchElementException();
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("Iterator<ThreadInfo>.remove()");
    }
  }
  
  /**
   * an iterator for a canonical order over all live threads
   */
  public Iterator<ThreadInfo> canonicalLiveIterator(){
    return new CanonicalLiveIterator();     
  }
  
  
  /**
   * only for debugging purposes, this is expensive
   */
  public void checkConsistency(boolean isStore) {
    for (int i = 0; i < threads.length; i++) {
      ThreadInfo ti = threads[i];
      
      ti.checkConsistency(isStore);
    }
  }
}
