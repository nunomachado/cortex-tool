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
package gov.nasa.jpf;

import gov.nasa.jpf.classfile.ClassFile;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.VMListener;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.search.SearchListener;

/**
 * abstract base class that dummy implements Property, Search- and VMListener methods
 * convenient for creating listeners that act as properties, just having to override
 * the methods they need
 *
 * the only local functionality is that instances register themselves automatically
 * as property when the search is started
 *
 * <2do> rewrite once GenericProperty is an interface
 */
public abstract class PropertyListenerAdapter extends GenericProperty implements
    SearchListener, VMListener, PublisherExtension {

  //--- Property interface
  public boolean check(Search search, JVM vm) {
    // return false if property is violated
    return true;
  }

  public void reset () {
    // override if the property has any local state
  }

  //--- the VMListener interface
  public void instructionExecuted(JVM vm) {}
  public void executeInstruction(JVM vm) {}
  public void threadStarted(JVM vm) {}
  public void threadWaiting (JVM vm) {}
  public void threadNotified (JVM vm) {}
  public void threadInterrupted (JVM vm) {}
  public void threadScheduled (JVM vm) {}
  public void threadBlocked (JVM vm) {}
  public void threadTerminated(JVM vm) {}
  public void loadClass (JVM vm, ClassFile cf) {}
  public void classLoaded(JVM vm) {}
  public void objectCreated(JVM vm) {}
  public void objectReleased(JVM vm) {}
  public void objectLocked (JVM vm) {}
  public void objectUnlocked (JVM vm) {}
  public void objectWait (JVM vm) {}
  public void objectNotify (JVM vm) {}
  public void objectNotifyAll (JVM vm) {}
  public void gcBegin(JVM vm) {}
  public void gcEnd(JVM vm) {}
  public void exceptionThrown(JVM vm) {}
  public void exceptionBailout(JVM vm) {}
  public void exceptionHandled(JVM vm) {}
  public void choiceGeneratorRegistered (JVM vm) {}
  public void choiceGeneratorSet (JVM vm) {}
  public void choiceGeneratorAdvanced (JVM vm) {}
  public void choiceGeneratorProcessed (JVM vm) {}
  public void methodEntered (JVM vm) {}
  public void methodExited (JVM vm) {}


  //--- the SearchListener interface
  public void stateAdvanced(Search search) {}
  public void stateProcessed(Search search) {}
  public void stateBacktracked(Search search) {}
  public void statePurged(Search search) {}
  public void stateStored(Search search) {}
  public void stateRestored(Search search) {}
  public void propertyViolated(Search search) {}
  public void searchStarted(Search search) {
    search.addProperty(this);
  }
  public void searchConstraintHit(Search search) {}
  public void searchFinished(Search search) {}


  //--- PublisherExtension interface
  public void publishStart (Publisher publisher) {}
  public void publishTransition (Publisher publisher) {}
  public void publishPropertyViolation (Publisher publisher) {}
  public void publishConstraintHit (Publisher publisher) {}
  public void publishFinished (Publisher publisher) {}
}
