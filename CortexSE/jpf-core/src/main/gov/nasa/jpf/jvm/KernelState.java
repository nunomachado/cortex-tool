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

import java.util.Stack;


/**
 * This class represents the SUT program state (statics, heap and threads)
 */
public class KernelState implements Restorable<KernelState> {

  /** The area containing static fields and  classes */
  public StaticArea statics;

  /** The area containing the heap */
  public Heap heap;

  /** The list of the threads */
  public ThreadList threads;

  /**
   * current listeners waiting for notification of next change.
   */
  private Stack<ChangeListener> listeners = new Stack<ChangeListener>();


  static class KsMemento implements Memento<KernelState> {
    // note - order does matter: threads need to be restored before the heap
    Memento<ThreadList> threadsMemento;
    Memento<StaticArea> staticsMemento;
    Memento<Heap> heapMemento;

    KsMemento (KernelState ks){
      threadsMemento = ks.threads.getMemento();
      staticsMemento = ks.statics.getMemento();
      heapMemento = ks.heap.getMemento();
    }

    public KernelState restore (KernelState ks) {
      // those are all in-situ objects, no need to set them in ks
      threadsMemento.restore(ks.threads);
      staticsMemento.restore(ks.statics);
      heapMemento.restore(ks.heap);

      return ks;
    }
  }

  /**
   * Creates a new kernel state object.
   */
  public KernelState (Config config) {
    Class<?>[] argTypes = { Config.class, KernelState.class };
    Object[] args = { config, this };

    statics = config.getEssentialInstance("vm.static.class", StaticArea.class, argTypes, args);
    heap = config.getEssentialInstance("vm.heap.class", Heap.class, argTypes, args);
    threads = config.getEssentialInstance("vm.threadlist.class", ThreadList.class, argTypes, args);
  }

  public Memento<KernelState> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  public Memento<KernelState> getMemento(){
    return new KsMemento(this);
  }

  public StaticArea getStaticArea() {
    return statics;
  }

  public Heap getHeap() {
    return heap;
  }

  public ThreadList getThreadList() {
    return threads;
  }

  /**
   * interface for getting notified of changes to KernelState and everything
   * "below" it.
   */
  public interface ChangeListener {
    void kernelStateChanged(KernelState ks);
  }

  /**
   * called by internals to indicate a change in KernelState.  list of listeners
   * is emptied.
   */
  public void changed() {
    while (!listeners.empty()) {
      listeners.pop().kernelStateChanged(this);
    }
  }

  /**
   * push a listener for notification of the next change.  further notification
   * requires re-pushing.
   */
  public void pushChangeListener(ChangeListener cl) {
    if (cl instanceof IncrementalChangeTracker && listeners.size() > 0) {
      for (ChangeListener l : listeners) {
        if (l instanceof IncrementalChangeTracker) {
          throw new IllegalStateException("Only one IncrementalChangeTracker allowed!");
        }
      }
    }
    listeners.push(cl);
  }

  boolean isDeadlocked () {
    return threads.isDeadlocked();
  }

  /**
   * The program is terminated if there are no alive threads, and there is no nonDaemon left.
   * 
   * NOTE - this is only approximated in real life. Daemon threads can still run for a few cycles
   * after the last non-daemon died, which opens an interesting source of errors we
   * actually might want to check for
   */
  public boolean isTerminated () {
    //return !threads.anyAliveThread();
    return !threads.hasMoreThreadsToRun();
  }

  public int getThreadCount () {
    return threads.length();
  }

  public void gc () {
        
    heap.gc();

    // we might have stored stale references in live objects
    // (outside of reference fields)
    // <2do> get rid of this by storing objects instead of ref/id values that are reused
    heap.cleanUpDanglingReferences();
    statics.cleanUpDanglingReferences(heap);
  }

  public void hash (HashData hd) {
    heap.hash(hd);
    statics.hash(hd);
    threads.hash(hd);
  }
}
