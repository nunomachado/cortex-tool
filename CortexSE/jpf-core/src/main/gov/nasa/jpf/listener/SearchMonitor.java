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
package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.search.heuristic.HeuristicSearch;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * SearchListener class to collect and report statistical
 * data during JPF execution.
 * This replaces the old JPF Statistics mechanism
 * 
 * updated 28 July 2006: now System.out is the default, rather than trying the
 * network (which loads extra classes and raises RuntimeExceptions in the
 * process, etc.).  Now to use network, at least the port must be specified.
 * -peterd
 */
public class SearchMonitor extends ListenerAdapter {

  static final String DEF_HOSTNAME = "localhost";
  static final int DEF_INTERVAL = 10000; // min duration in ms between reports
  
  String hostName;
  int port;
  
  Socket sock;
  PrintWriter out;
  
  int reportNumber;
  
  int  interval; 
  long time;
  long lastTime;
  long startTime;
  long startFreeMemory;
  
  int searchLevel=0;
  int maxSearchLevel=0;
  
  int newStates;
  int endStates;
  int backtracks;
  int revisitedStates;
  int processedStates;
  int restoredStates;
  
  int steps;

  long maxMemory;
  long totalMemory;
  long freeMemory;
    
  boolean isHeuristic = false;
  int queueSize = 0;
  
  int currentHeapCount = 0;
  int maxHeapCount = 0;

  int currentThreadCount = 0;
  int maxThreadCount = 0;
  int totalThreads = 0;

  
  String constraintHit;

  int dpCalls = 0;
  
  /*
   * SearchListener interface
   */  
  
  public void stateAdvanced(Search search) {
    
    steps += search.getTransition().getStepCount();
   
    
    
    if (isHeuristic)
    	queueSize = ((HeuristicSearch)(search)).getQueueSize();
    
    if (search.isNewState()) {
      searchLevel = search.getDepth();
      if (searchLevel > maxSearchLevel)
      	maxSearchLevel = searchLevel;
      
      newStates++; 
      
      currentHeapCount = search.getVM().getHeap().size();
      
      if (currentHeapCount > maxHeapCount)
      	maxHeapCount = currentHeapCount;
      
      currentThreadCount = search.getVM().getAliveThreadCount();
      totalThreads = search.getVM().getKernelState().getThreadCount();
      
      if (currentThreadCount > maxThreadCount)
      	maxThreadCount = currentThreadCount;
      
      
      if (search.isEndState()) {
        endStates++;
      }
    } else {
      revisitedStates++;
    }
    
    checkReport();
  }

  public void stateProcessed(Search search) {
    processedStates++;
    checkReport();
  }

  public void stateBacktracked(Search search) {
    searchLevel = search.getDepth();
    backtracks++;
    checkReport();
  }

  public void stateRestored(Search search) {
    searchLevel = search.getDepth();
    restoredStates++;
    checkReport();
  }

  public void propertyViolated(Search search) {
  }

  public void searchStarted(Search search) {
    connect();
    
    if (search instanceof HeuristicSearch) {
      isHeuristic = true;
    }
    
    startTime = lastTime = System.currentTimeMillis();
    
    Runtime rt = Runtime.getRuntime();
    startFreeMemory = rt.freeMemory();
    totalMemory = rt.totalMemory();
    maxMemory = rt.maxMemory();
    reportNumber = 1;
  }

  public void searchConstraintHit(Search search) {
	if (constraintHit == null) {  
	  constraintHit = search.getSearchConstraint();
	  System.out.println("Constraint Hit: " + constraintHit);
	}
  }

  public void searchFinished(Search search) {
    report("------------------------------------ statistics");
    if (constraintHit != null) {    	
      if (constraintHit.equals("SIZE"))
    	constraintHit = "Memory";
    	
      out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
      out.println("INCOMPLETE SEARCH (" + constraintHit + " Constraint Hit)");
      out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
    }  
    if (sock != null) {
      try {
        out.close();
        sock.close();
      } catch (IOException iox) {
      }
    }
  }

  void checkReport () {
    time = System.currentTimeMillis();
    
    Runtime rt = Runtime.getRuntime();
    long m = rt.totalMemory();
    if (m > totalMemory) {
      totalMemory = m;
    }
    
    if ((time - lastTime) >= interval) {
      freeMemory = rt.freeMemory();
      
      report("# " + reportNumber++);
      lastTime = time;
    }
  }
  
  void reportRuntime () {
    long td = time - startTime;
    
    int h = (int) (td / 3600000);
    int m = (int) (td / 60000) % 60;
    int s = (int) (td / 1000) % 60;
    
    out.print("  abs time:          ");
    if (h < 10) out.print('0');
    out.print( h);
    out.print(':');
    if (m < 10) out.print('0');
    out.print( m);
    out.print(':');
    if (s < 10) out.print('0');
    out.print( s);
    
    out.print( "  (");
    out.print(td);
    out.println(" ms)");
  }
  
  void report (String header) {

    out.println(header);

    reportRuntime();
    
    out.print("  rel. time [ms]:    ");
    out.println(time - lastTime);
    
    out.println();
    out.print("  search depth:      ");
    out.print(searchLevel);
    out.print(" (max: ");
    out.print(maxSearchLevel);
    out.println(")");
    
    out.print("  new states:        ");
    out.println(newStates);
    
    out.print("  revisited states:  ");
    out.println(revisitedStates);
        
    out.print("  end states:        ");
    out.println(endStates);

    out.print("  backtracks:        ");
    out.println(backtracks);

    out.print("  processed states:  ");
    out.print( processedStates);
    out.print(" (");
    // a little ad-hoc rounding
    double d = (double) backtracks / (double)processedStates;
    int n = (int) d;
    int m = (int) ((d - /*(double)*/ n) * 10.0);
    out.print( n);
    out.print('.');
    out.print(m);
    out.println( " bt/proc state)");
    
    out.print("  restored states:   ");
    out.println(restoredStates);

    if (isHeuristic) {
    	out.print("  queue size:        ");
    	out.println(queueSize);
    }
/*
    int dpcalls = gov.nasa.jpf.symbolic.integer.JPF_gov_nasa_jpf_symbolic_integer_SymbolicConstraints.dp_calls;
    if (dpcalls > 0) {
    	out.print("  DP Calls:          ");
    	out.println(dpcalls);
    	out.print("  DP % Time:         ");
    	out.println((gov.nasa.jpf.symbolic.integer.JPF_gov_nasa_jpf_symbolic_integer_SymbolicConstraints.dp_time/(time - startTime))*100);
    	out.print("  DP % Satisfied:    ");
    	out.println((((double)gov.nasa.jpf.symbolic.integer.JPF_gov_nasa_jpf_symbolic_integer_SymbolicConstraints.sat_count)/dpcalls)*100);    
    }
*/
    
    out.println();
    out.print("  total memory [kB]: ");
    out.print(totalMemory / 1024);
    out.print(" (max: ");
    out.print(maxMemory / 1024);
    out.println(")");
    
    out.print("  free memory [kB]:  ");
    out.println(freeMemory / 1024);
    
    out.print("  heap objects:      ");
    out.print(currentHeapCount);
    out.print(" (max: ");
    out.print(maxHeapCount);
    out.println(")");

    out.print("  alive threads:     ");
    out.print(currentThreadCount);
    out.print(" (max: ");
    out.print(maxThreadCount);
    out.println(") out of " + totalThreads + " current thread objects ");
    
    
    out.println();
  }
  
  int consumeIntArg (String[] args, int i, String varName, int def) {
    int ret = def;
    
    args[i] = null;
    if (i < args.length-1){
      i++;
      try {
        ret = Integer.parseInt(args[i]);
        args[i] = null;
      } catch (NumberFormatException nfx) {
        System.err.print("Warning: illegal " + varName + " specification: " + args[i]
                                  + " using default " + ret);
      }
    }
   
   return ret;
  }
  
  void filterArgs (String[] args) {
    for (int i=0; i<args.length; i++) {
      if (args[i] != null) {
        if (args[i].equals("-port")) {
          port = consumeIntArg(args, i++, "port", -1);
        } else if (args[i].equals("-interval")) {
          interval = consumeIntArg(args, i++, "interval", DEF_INTERVAL);
        } else if (args[i].equals("-hostname")) {
          args[i] = null;
          if (i < args.length-1){
            i++;
            hostName = args[i];
            args[i] = null;
          }
        }
      }
    }
  }
  
  static void printUsage () {
    System.out.println("SearchMonitor - a JPF listener tool to monitor JPF searches");
    System.out.println("usage: java gov.nasa.jpf.tools.SearchMonitor <jpf-options>  <monitor-options> <class>");
    System.out.println("<monitor-options>:");
    System.out.println("       -hostname <name> : connect to host <name>, default: " + DEF_HOSTNAME);
    System.out.println("       -port <num>      : connect to port <num>, default: (use stdout)");
    System.out.println("       -interval <num>  : report every <num> msec, default: " + DEF_INTERVAL);
  }
  
  
  void connect () {
    if (port > 0) {
      try {
        sock = new Socket(hostName, port);
        out = new PrintWriter(sock.getOutputStream(), true);
      } catch (UnknownHostException uhx) {
        System.err.println("Warning: unknown log host: " + hostName + ", using System.out");
      } catch (ConnectException cex) {
        System.err.println("Warning: no log host detected, using System.out");
      } catch (IOException iox) {
        System.err.println(iox);
      }
    }
    
    if (out == null) {
      out = new PrintWriter(System.out, true);
    }
  }
  
  public void run (Config conf) {
    JPF jpf = new JPF(conf);
    jpf.addSearchListener(this);
    
    jpf.run();
  }
  
  public SearchMonitor (Config config) {
    port = config.getInt("monitor.port", -1);  
    hostName = config.getString("monitor.hostname", DEF_HOSTNAME);
    interval = config.getInt("monitor.interval", DEF_INTERVAL);
  }  
}
