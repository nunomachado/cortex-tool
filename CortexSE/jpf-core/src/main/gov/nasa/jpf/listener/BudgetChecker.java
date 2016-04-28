//
// Copyright (C) 2008 United States Government as represented by the
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
package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.annotation.JPFOption;
import gov.nasa.jpf.annotation.JPFOptions;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.search.Search;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Listener that implements various budget constraints
 */
@JPFOptions({
  @JPFOption(type = "Long", key = "budget.max_time", defaultValue= "-1", comment = "stop search after specified duration [msec]"),
  @JPFOption(type = "Long", key = "budget.max_heap", defaultValue = "-1", comment="stop search when VM heapsize reaches specified limit"),
  @JPFOption(type = "Int", key = "budget.max_depth", defaultValue = "-1", comment = "stop search at specified search depth"),
  @JPFOption(type = "Long", key = "budget.max_insn", defaultValue = "-1", comment = "stop search after specified number of intstructions"),
  @JPFOption(type = "Int", key = "budget.max_state", defaultValue = "-1", comment = "stop search when reaching specified number of new states"),
  @JPFOption(type = "Int", key = "budget.max_new_states", defaultValue = "-1", comment="stop search ater specified number of non-replayed new states")
})
public class BudgetChecker extends ListenerAdapter {

  static final int CHECK_INTERVAL = 10000;
  static final int CHECK_INTERVAL1 = CHECK_INTERVAL-1;
    
  long tStart;
  MemoryUsage muStart;
  long mStart;
  MemoryMXBean mxb;
  
  JVM vm;
  Search search;
  long insnCount;

  //--- the budget thresholds
  long maxTime;
  long maxState;
  long maxDepth;
  long maxInsn;
  long maxHeap;
  
  int maxNewStates;
  
  int newStates;
  
  // the message explaining the exceeded budget
  String message;
  
  public BudgetChecker (Config conf, JPF jpf) {
    maxTime = conf.getDuration("budget.max_time", -1);
    maxHeap = conf.getMemorySize("budget.max_heap", -1);
    maxDepth = conf.getLong("budget.max_depth", -1);
    maxInsn = conf.getLong("budget.max_insn", -1);
    maxState = conf.getLong("budget.max_state", -1);
    
    maxNewStates = conf.getInt("budget.max_new_states", -1);
    
    tStart = System.currentTimeMillis();
    
    if (maxHeap > 0) {
      mxb = ManagementFactory.getMemoryMXBean();
      muStart = mxb.getHeapMemoryUsage();
      mStart = muStart.getUsed();
    }

    search = jpf.getSearch();
    vm = jpf.getVM();
  }
      
  public boolean timeExceeded() {
    if (maxTime > 0) {
      long dur = System.currentTimeMillis() - tStart;
      if (dur > maxTime) {
        message = "max time exceeded: " + Publisher.formatHMS(dur)
               + " >= " + Publisher.formatHMS(maxTime);
        return true;
      }
    }
    
    return false;
  }
  
  public boolean heapExceeded() {
    if (maxHeap > 0) {
      MemoryUsage mu = mxb.getHeapMemoryUsage();
      long used = mu.getUsed() - mStart;
      if (used > maxHeap) {
        message = "max heap exceeded: " + (used / (1024*1024)) + "MB" 
                      + " >= " + (maxHeap / (1024*1024)) + "MB" ;
        return true;
      }
    }
    
    return false;
  }
  
  public boolean depthExceeded () {
    if (maxDepth > 0) {
      int d = search.getDepth();
      if (d > maxDepth) {
        message = "max search depth exceeded: " + maxDepth;
        return true;
      }
    }
    
    return false;
  }
  
  public boolean statesExceeded () {
    if (maxState > 0) {
      int stateId = vm.getStateId();
      if (stateId > maxState) {
        message = "max states exceeded: " + maxState;;
        return true;
      }
    }
    
    return false;
  }
    
  public boolean insnExceeded () {
    if (maxInsn > 0) {
      if (insnCount > maxInsn) {
        message = "max instruction count exceeded: " + maxInsn;
        return true;
      }
    }
    return false;
  }
  
  public boolean newStatesExceeded(){
    if (newStates > maxNewStates) {
      message = "max new state count exceeded: "  + maxNewStates;
      return true;
    }
    return false;
  }
  
  public void stateAdvanced (Search search) {    
    if (timeExceeded() || heapExceeded()) {
      search.notifySearchConstraintHit(message);
      search.terminate();
    }
    
    if (search.isNewState()){
      if (!vm.isTraceReplay()){
        newStates++;
      }
      if (statesExceeded() || depthExceeded() || newStatesExceeded()){
        search.notifySearchConstraintHit(message);
        search.terminate();        
      }
    }
  }
      
  public void instructionExecuted (JVM vm) {
    if ((insnCount++ % CHECK_INTERVAL) == CHECK_INTERVAL1) {

      if (timeExceeded() || heapExceeded() || insnExceeded()) {
        search.notifySearchConstraintHit(message);

        vm.getCurrentThread().breakTransition();
        search.terminate();
      }    
    }
  }

}
