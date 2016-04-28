package pt.tecnico.symbiosis.tloax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.toolkits.graph.MutableDirectedGraph;
import soot.toolkits.scalar.Pair;


public class XClassLocalObjectsAnalysis {
	
	Map<SootField,Boolean> fieldInfoMap;
	Map<SootField,XFieldAccessInfo> fieldAccessInfoMap;
	public Boolean isFieldShared(SootField field)
	{
		if(fieldInfoMap.get(field) ==null)
		{
			fieldInfoMap.put(field,fieldAccessInfoMap.get(field).isFieldShared());
		}
		Boolean isShared = fieldInfoMap.get(field);
		return isShared;
	}
	public Iterator<SootField> iterator()
	{
		return fieldAccessInfoMap.keySet().iterator();
	}
	public void fieldRead(XAbstractRuntimeThread thread, SootField field)
	{
		Integer tid = thread.getThreadId();
		XFieldAccessInfo fieldAccessInfo = fieldAccessInfoMap.get(field);
		if(fieldAccessInfo==null)
		{
			fieldAccessInfo = new XFieldAccessInfo(field);
			fieldAccessInfoMap.put(field,fieldAccessInfo);
		}
		
		fieldAccessInfo.readAccess(tid);
		if(thread.isMultiInstance())
			fieldAccessInfo.readAccess(tid+1);
		
	}
	public void fieldWrite(XAbstractRuntimeThread thread, SootField field)
	{
		Integer tid = thread.getThreadId();
		XFieldAccessInfo fieldAccessInfo = fieldAccessInfoMap.get(field);
		if(fieldAccessInfo==null)
		{
			fieldAccessInfo = new XFieldAccessInfo(field);
			fieldAccessInfoMap.put(field,fieldAccessInfo);
		}
		
		fieldAccessInfo.writeAccess(tid);
		if(thread.isMultiInstance())
			fieldAccessInfo.writeAccess(tid+1);
		
	}
	SootClass sootClass;
	
//	List<SootMethod> allMethods;
//	List<SootMethod> externalMethods;
//	List<SootMethod> internalMethods;
//	
//	List<SootField> allFields;
//	List<SootField> externalFields;
//	List<SootField> internalFields;
//	
//	ArrayList<SootField> localFields;
//	ArrayList<SootField> sharedFields;
//	ArrayList<SootField> localInnerFields;
//	ArrayList<SootField> sharedInnerFields;
	
	XClassLocalObjectsAnalysis(SootClass sootClass)
	{
		this.sootClass = sootClass;
		this.fieldInfoMap = new HashMap<SootField,Boolean>();
		this.fieldAccessInfoMap =  new HashMap<SootField,XFieldAccessInfo>();
	}
	

	

}
