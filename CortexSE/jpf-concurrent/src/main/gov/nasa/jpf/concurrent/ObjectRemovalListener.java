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

import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.search.Search;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/*
 * This listener is a cure for memory leaks in my java.util.concurrent library abstraction.
 * As we don't remove objects from our main storage even if they are garbage
 * collected on non-MJI level we suffer from memory leaks.
 * Consider code:
 * 
 *    for(int i=0;i<10000;i++) {
 *        Semaphore s = new Semaphore();
 *        s = null;
 *    }
 *
 * This code without this listener will generate 10000 (because of reference numbering
 * and bounded heap size only 500) objects on MJI level that will never be freed.
 *
 * Listener behaves in very simple manner. Listens on stateAdvance/stateBacktracked
 * and accordingly saves/restored map with all tracked objects.
 * When objectReleased is called, we will remove gc'ed object from our saved map
 * for given state.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class ObjectRemovalListener extends PropertyListenerAdapter {

  private Map<Integer, Map<Integer, Model>> state2Model = new HashMap<Integer, Map<Integer, Model>>();

  public void stateAdvanced(Search search) {
    //Added to avoid overwriting
    if(state2Model.get(search.getStateId()) != null) return;
    Map<Integer,Model> ref2Model = findSame(Model.getReferenceToModel());
    if(ref2Model == null) ref2Model = Model.doCloneAll(Model.getReferenceToModel());
    state2Model.put(search.getStateId(), ref2Model);
  }

  public void stateBacktracked(Search search) {
    Map<Integer, Model> ref2Model = state2Model.get(search.getStateId());
    Model.setReferenceToModel(Model.doCloneAll(ref2Model));
  }

  /*
   * Because objectReleased is called before stateAdvanced and with old state number
   * we cannot modify state2Model as we did before. Now we modify Model map and
   * stateAdvanced will take care about modifying state2Model.
  */
  
  public void objectReleased(JVM vm) {
    ElementInfo ei = vm.getLastElementInfo();
    if (!isModeled(ei.getClassInfo())) return;
    MJIEnv env = null;
    int objRef = ei.getObjectRef();
    Model.removeModel(env, objRef);
  }

  //For some reason JPF will preserve Model map between tests, needs more investigation.
  public void searchStarted(Search search) {
    Model.getReferenceToModel().clear();
  }

 /*
   * This method is recursive, class is considered to be modeled
   * when meets given requirements or one of it's superclass
   * meets the specified requirements
   */
  private boolean isModeled(ClassInfo c) {
    if(c == null) return false;
    if(isModeled(c.getSuperClass())) return true;
    if (c.getName().indexOf("java.util.concurrent") == -1) return false;
    if (c.getDeclaredInstanceField("version") == null) return false;
    if (!c.getDeclaredInstanceField("version").getTypeClassInfo().equals(ClassInfo.getResolvedClassInfo("int"))) return false;
    return true;
  }

  private Map<Integer,Model> findSame(Map<Integer,Model> m) {
    Iterator<Map<Integer,Model>> i = state2Model.values().iterator();
    while(i.hasNext()) {
      Map<Integer,Model> ii = i.next();
      if(m.equals(ii)) return ii;
    }
    return null;
  }

}

