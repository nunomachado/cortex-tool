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
package gov.nasa.jpf.concurrent;

import gov.nasa.jpf.concurrent.version.ThreadVersion;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.Heap;
import gov.nasa.jpf.jvm.JPF_sun_misc_Unsafe;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;
import java.util.ArrayList;
import java.util.List;


/*
 * This class is the heart of the AQS,ReentrantLock,Semaphore,... model.
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public abstract class ThreadModel extends Model {

  protected abstract void addAndPark(int permits,long timeout);

  public void removeThreadFromQueue() {
    getCurrentVersion().removeThreadFromQueue(thread);
    saveVersion();
  }

  public boolean isQueued(int threadObjRef) {
    if(checkIsNull(threadObjRef)) return false;
    return getCurrentVersion().getQueuedThreads().contains(env.getThreadInfoForObjRef(threadObjRef));
  }

  protected void dequeueLongestWaitingThread() {
    if(getCurrentVersion().getQueuedThreads().size() > 0) {
      ThreadInfo t = getCurrentVersion().getQueuedThreads().get(0);
      unpark(t.getThreadObjectRef());
      getCurrentVersion().removeThreadFromQueue(t);
      getCurrentVersion().setLastRemoved(t);
    } else {
      getCurrentVersion().setLastRemoved(null);
    }
  }

  public boolean hasQueuedThreads() {
    return getCurrentVersion().getQueuedThreads().size() > 0;
  }

  public int getQueueLength() {
    return getCurrentVersion().getQueuedThreads().size();
  }

  public int getQueuedThreads() {
    List<ThreadInfo> threadList = new ArrayList<ThreadInfo>();
    for(int i=0;i<getCurrentVersion().getQueuedThreads().size();i++) {
      threadList.add(getCurrentVersion().getQueuedThreads().get(i));
    }
    return createQueueWithThreads(env, thread, threadList);
  }


  protected int createQueueWithThreads (MJIEnv env, ThreadInfo thread, List<ThreadInfo> listToBeExported) {
    Heap heap = env.getHeap();
    Instruction insn = thread.getPC();
    ClassInfo listClass = ClassInfo.tryGetResolvedClassInfo("java.util.ArrayList");
    if (insn.requiresClinitExecution(thread, listClass)) {
      env.repeatInvocation();
      return MJIEnv.NULL;
    }
    int rList = heap.newObject(listClass, thread);
    int rNewElementData = heap.newArray("Ljava/lang/Thread", 10, thread);
    ElementInfo newElementData = heap.get(rNewElementData);
    for (int i = 0; i < listToBeExported.size(); i++) {
      newElementData.setReferenceElement(i, listToBeExported.get(i).getThreadObjectRef());
    }
    env.setReferenceField(rList, "elementData", rNewElementData);
    env.setIntField(rList, "size", listToBeExported.size());
    return rList;
  }

  protected abstract ThreadVersion getCurrentVersion();

  protected void park (long timeout) {
    JPF_sun_misc_Unsafe.park__ZJ__V(env, -1, false, timeout);
  }

  /*
   * The only tricky part of this method is creating new ChoiceGenerator. We
   * need to do that because unpark()(in Unsafe) will not create any new
   * ChoiceGenerator whereas it should(it could be a JPF bug)
   */
  protected void unpark (int objRef) {
    JPF_sun_misc_Unsafe.unpark__Ljava_lang_Object_2__V(env, -1, objRef);
    env.getSystemState().setNextChoiceGenerator(new ThreadChoiceFromSet("unpark",env.getVM().getThreadList().getRunnableThreads(), true));
  }

  /*
   * After a thread has been unparked we need to park it one more time to change
   * the state from UNBLOCKED to RUNNING.
   */

  protected boolean checkIfUnblock () {
    //if (thread.getState() == ThreadInfo.State.UNBLOCKED) {
    if (thread.isUnblocked()){
      park(0);
      return true;
    }
    return false;
  }

  protected boolean checkNotInterrupted () {
    if (env.getThreadInfo().isInterrupted(true)) {
      removeThreadFromQueue();
      env.throwException("java.lang.InterruptedException");
      return false;
    }
    return true;
  }
  
}
