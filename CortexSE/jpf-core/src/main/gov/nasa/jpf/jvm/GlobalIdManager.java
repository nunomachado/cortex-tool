//
// Copyright (C) 2012 United States Government as represented by the
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

import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.HashData;
import gov.nasa.jpf.util.IntTable;
import gov.nasa.jpf.util.MutableInteger;
import gov.nasa.jpf.util.MutableIntegerRestorer;
import gov.nasa.jpf.util.ObjVector;

/**
 * helper object to compute search global id's, e.g. to create canonical
 * orders. Usually, there is one instance per type (e.g. ThreadInfo or
 * ClassLoaderInfo), and it is kept in a static field of the respective class.
 * 
 * Returned id values are packed, i.e. can be used to index into arrays
 * 
 * The global ids are guaranteed to be unique along a given path since
 * there can only be one Nth object of class C created in Thread T at any time, and
 * we use a <GlobalId,int> hashmap with a GlobalId.equals() that compares both values 
 */
public class GlobalIdManager {

  // this is only used internally. Externally, global ids are integers for us
  static class GlobalId {
    
    protected int tgid;     // global id for creating thread (0 if this is for main thread itself)
    protected int count;    // running number created by thread
    protected int mgid;     // global id for creating method
    protected int insnIdx;  // instruction index for creating instruction

    protected int hc;       // hashCode cache   
    
    public GlobalId (int tgid, int count, int mgid, int insnIdx){
      this.tgid = tgid;
      this.count = count;
      this.mgid = mgid;
      this.insnIdx = insnIdx;
      
      HashData hd = new HashData();
      hd.add(tgid);
      hd.add(count);
      hd.add(mgid);
      hd.add(insnIdx);
      hc = hd.getValue();
    }
    
    @Override
    public boolean equals (Object o){
      if (o instanceof GlobalId){
        GlobalId other = (GlobalId)o;
        
        if ((hc == other.hc) &&  // shortcut
            (tgid == other.tgid) &&
            (count == other.count) &&
            (mgid == other.mgid) &&
            (insnIdx == other.insnIdx)){
          return true;
        }
      }
        
      return false;
    }
      
    @Override
    public int hashCode(){
      return hc;
    }
  }
  
  // this is where we keep track of instance counts without the need of adding
  // dedicated fields to the model classes
  ObjVector<MutableInteger> perThreadPathInstances = new ObjVector<MutableInteger>();
  
  // this is where we keep track of assigned ids, so that ids are packed and
  // we can use them in arrays/vectors (such as 'perThreadPathInstances')
  IntTable<GlobalId> idMap = new IntTable<GlobalId>();
  
  
  protected int getPathCount (SystemState ss, int tgid){
    MutableInteger mInt = perThreadPathInstances.get(tgid);
    if (mInt == null){
      mInt = new MutableInteger(0);
      perThreadPathInstances.set(tgid, mInt);
    }
    
    // make sure we properly restore the original value upon backtrack
    if (!ss.hasRestorer(mInt)){
      ss.putRestorer(mInt, new MutableIntegerRestorer(mInt));
    }
    
    mInt.inc(); // we got here because there is a new object we need an id for

    return mInt.intValue();
  }
  
  public int getNewId (SystemState ss, ThreadInfo executingThread, Instruction insn){
    int tgid = (executingThread != null) ? executingThread.getGlobalId() : 0;
    int count = getPathCount(ss,tgid);
    
    int mgid;
    int insnIdx;
    
    if (insn != null){
      mgid = insn.getMethodInfo().getGlobalId();
      insnIdx = insn.getInstructionIndex();      
    } else {
      mgid = -1;
      insnIdx = -1;
    }
    
    GlobalId key = new GlobalId(tgid,count,mgid,insnIdx);
    int gid = idMap.poolIndex(key);
    
    return gid;
  }
}
