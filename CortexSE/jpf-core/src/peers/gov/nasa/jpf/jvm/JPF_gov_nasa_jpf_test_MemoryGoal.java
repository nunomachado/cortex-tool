//
// Copyright (C) 2007 United States Government as represented by the
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

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;

/**
 * native peer for MemoryGoal tests
 */
public class JPF_gov_nasa_jpf_test_MemoryGoal {

  static Listener listener;
  
  // <2do> that's too simple, because we should only measure what is
  // allocated from the invoked method, not the MethodTester. Needs a listener
  
  static class Listener extends ListenerAdapter {
    
    MethodInfo mi;
    boolean active;
    
    long nAllocBytes;
    long nFreeBytes;
    long nAlloc;
    long nFree;
    
    Listener (MethodInfo mi){
      this.mi = mi;
    }
    
    public void objectCreated (JVM vm){
      if (active){
        ElementInfo ei = vm.getLastElementInfo();
        
        nAlloc++;
        nAllocBytes += ei.getHeapSize(); // just an approximation
      }
    }
    
    public void objectReleased (JVM vm){
      if (active){
        ElementInfo ei = vm.getLastElementInfo();
        
        nFree++;
        nFreeBytes += ei.getHeapSize(); // just an approximation
      }      
    }
    
    public void instructionExecuted (JVM vm){
      Instruction insn = vm.getLastInstruction();
      if (!active) {
        if (insn.getMethodInfo() == mi){
          active = true;
        }
      } else {
        if ((insn instanceof ReturnInstruction) && (insn.getMethodInfo() == mi)){
          active = false;
        }
      }
    }
    
    long totalAllocBytes() {
      return nAllocBytes - nFreeBytes;
    }
  }
  
  public static boolean preCheck__Lgov_nasa_jpf_test_TestContext_2Ljava_lang_reflect_Method_2__Z
                      (MJIEnv env, int objRef, int testContextRef, int methodRef){
    MethodInfo mi = JPF_java_lang_reflect_Method.getMethodInfo(env, methodRef);
    
    listener = new Listener(mi);
    env.addListener(listener);
    return true;
  }
  
  // what a terrible name!
  public static boolean postCheck__Lgov_nasa_jpf_test_TestContext_2Ljava_lang_reflect_Method_2Ljava_lang_Object_2Ljava_lang_Throwable_2__Z 
           (MJIEnv env, int objRef, int testContextRef, int methdRef, int resultRef, int exRef){

    long nMax = env.getLongField(objRef, "maxGrowth");

    Listener l = listener;
    env.removeListener(l);
    listener = null;
    
    return (l.totalAllocBytes() <= nMax);
  }
}
