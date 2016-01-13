package pt.tecnico.jpf.symbiosis;

import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.AALOAD;
import gov.nasa.jpf.jvm.bytecode.AASTORE;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.IALOAD;
import gov.nasa.jpf.jvm.bytecode.IASTORE;
import gov.nasa.jpf.jvm.bytecode.Instruction;

import java.util.HashMap;

import com.sun.org.apache.bcel.internal.generic.FASTORE;

public class StateInfo {
	public String tid;  		//thread id
	public int jpfid;			//JPF state id
	public String pathid; 		//path id
	public String symbTrace;	//symbolic trace
	public int bbsReached;		//number of basic blocks reached
	public int brchsReached;	//number branches reached
	public HashMap<String,Integer> mapSymVarIds = new HashMap<String, Integer>(); //map: symbvar -> number of times it was accessed by this thread for this path
	public boolean mayRunFree;	//flag indicating whether this state is allowed to go on free mode at some point
	public boolean hitAssert; //flag indicating if this state already seen the assertion condition (if it has not and belongs to the assertion thread, it is not allowed to store logs)
	
	public StateInfo(){
		tid = "";
		jpfid = 0;
		pathid = "-1";
		symbTrace = "";
		bbsReached = 0;
		brchsReached = 0;
		mapSymVarIds = new HashMap<String, Integer>();
		mayRunFree = false;
		hitAssert = false;
	}
	
	public StateInfo(String threadid, int stateid, String pathid, String symbtrace, int bbs, int brchs, HashMap<String,Integer> mapSymVarIds, boolean freeMode)
	{
		this.tid = threadid;
		this.jpfid = stateid;
		this.pathid = pathid;
		this.symbTrace = symbtrace;
		this.bbsReached = bbs;
		this.brchsReached = brchs;
		this.mapSymVarIds = mapSymVarIds;
		this.mayRunFree = freeMode;
	}
	
	public StateInfo(int stateid, StateInfo s){
		this.jpfid = stateid;
		this.tid = s.tid;
		this.pathid = s.pathid;
		this.symbTrace = s.symbTrace;
		this.bbsReached = s.bbsReached;
		this.brchsReached = s.brchsReached;
		this.mapSymVarIds = new HashMap<String, Integer>();
		this.mapSymVarIds.putAll(s.mapSymVarIds);
		this.mayRunFree = s.mayRunFree;
		this.hitAssert = s.hitAssert;
	}
	
	public String getStateId(){
		return (tid+"_"+jpfid+pathid);
	}
	
	public String toString(){
		String ret = "pathid: "+pathid+", BBs: "+bbsReached+", branches: "+brchsReached+", mayRunFree: "+mayRunFree+", hitAssert = "+hitAssert;
		//ret += "\nsymbtrace: "+symbTrace;
		return ret;
	}
	
	/**
	 * Returns a string representing the symbolic name of a RW operation.
	 * R/W-varname_address-threadid-varid
	 * @param field
	 * @param isWrite
	 * @return
	 */
	public String getRWSymbName(FieldInstruction field, boolean isWrite)
	{
		String tag = "W-";
		if(!isWrite)
			tag = "R-";
	   String symbname = tag+field.getFieldInfo().getName()+"_"+field.getLastElementInfo().getObjectRef()+"-"+tid;
	   
	   if(mapSymVarIds.containsKey(symbname))
		{
			int id = mapSymVarIds.get(symbname); 
			//System.out.println("--> OK: mapSymVarIds["+symbname+"] = "+mapSymVarIds.get(symbname));
			symbname = symbname +"-"+ id;
		}
		else
		{
			//System.out.println("--> NEW: mapSymVarIds["+symbname+"] = 1");
			mapSymVarIds.put(symbname, 1); 
			symbname = symbname +"-1";
		}
		return symbname;
	}
	
	public String getRWArraySymbName(Instruction inst, FieldInstruction field, ThreadInfo ti)
	{
		String tag = "";
		int arrayRef = -1;
		int index = -1;
		if(inst instanceof IASTORE){
			tag = "W-";
			arrayRef = ((IASTORE)inst).getArrayRef(ti);
			index = ((IASTORE)inst).getIndex(ti);
		}
		else if(inst instanceof AASTORE){
			tag = "W-";
			arrayRef = ((AASTORE)inst).getArrayRef(ti);
			index = ((AASTORE)inst).getIndex(ti);
		}
		else if(inst instanceof IALOAD){
			tag = "R-";		
			arrayRef = ((IALOAD)inst).getArrayRef(ti);
			index = ((IALOAD)inst).getIndex(ti);
		}
		else if(inst instanceof AALOAD){
			tag = "R-";		
			arrayRef = ((AALOAD)inst).getArrayRef(ti);
			index = ((AALOAD)inst).getIndex(ti);
		}
		
		String symbname = tag+field.getFieldInfo().getName()+"_"+(arrayRef+index)+"-"+tid;
	   
	   if(mapSymVarIds.containsKey(symbname))
		{
			int id = mapSymVarIds.get(symbname); 
			//System.out.println("--> OK: mapSymVarIds["+symbname+"] = "+mapSymVarIds.get(symbname));
			symbname = symbname +"-"+ id;
		}
		else
		{
			//System.out.println("--> NEW: mapSymVarIds["+symbname+"] = 1");
			mapSymVarIds.put(symbname, 1); 
			symbname = symbname +"-1";
		}
		return symbname;
	}
	
	/**
	 * Increase symbolic variable id. Used to update the id of fresh symbolic vars.
	 */
	public void incrementSymbVarId(String symbvar)
	{
		symbvar = symbvar.substring(0,symbvar.lastIndexOf('-'));
		int id = mapSymVarIds.get(symbvar);
		id++;
		mapSymVarIds.put(symbvar, id);
		//System.out.println("--> INCREMENT: mapSymVarIds["+symbvar+"] = "+mapSymVarIds.get(symbvar));
	}
	
}
