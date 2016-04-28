package pt.tecnico.symbiosis.tloax;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.spark.pag.AllocNode;

public class XMHPAnalysis {
	private XPegCallGraph pcg;
	List<XAbstractRuntimeThread> MHPLists;
	XMHPAnalysis()
	{
		MHPLists = new ArrayList<XAbstractRuntimeThread>();
		pcg= XG.v().getPegCallGraph();
		
		buildMHPLists();
	}
	
	private void buildMHPLists() 
	{
		XAllocNodesFinder anf = XG.v().getMyAllocNodesFinder();
		Set<AllocNode> multiRunAllocNodes = anf.getMultiRunAllocNodes();
		
		XStartJoinFinder sjf = XG.v().getMyStartJoinFinder();
		Map<Stmt, List<AllocNode>> startToAllocNodes = sjf.getStartToAllocNodes();
		Map<Stmt, List<SootMethod>> startToRunMethods = sjf.getStartToRunMethods();
		
		Iterator threadIt = startToRunMethods.entrySet().iterator();
		int threadId = 1;
		while(threadIt.hasNext())
		{
			Map.Entry e = (Map.Entry) threadIt.next();
			List runMethods = (List) e.getValue();
			List threadAllocNodes = startToAllocNodes.get(e.getKey());

			// Get a list of all possible unique Runnable.run methods for this thread start statement
			XAbstractRuntimeThread thread = new XAbstractRuntimeThread(); // provides a list interface to the methods in a thread's sub-call-graph
			
			Iterator runMethodsIt = runMethods.iterator();
			while(runMethodsIt.hasNext())
			{
				SootMethod method = (SootMethod) runMethodsIt.next();
				//thread.addMethod(method);
				thread.addRunMethod(method);
			}
			
			thread.setThreadId(threadId);
			// Add this list of methods to MHPLists
			MHPLists.add(thread);
						
			// Find out if the "thread" in "thread.start()" could be more than one object
			boolean mayStartMultipleThreadObjects = (threadAllocNodes.size() > 1);
			if(!mayStartMultipleThreadObjects) // if there's only one alloc node
			{
				if(multiRunAllocNodes.contains(threadAllocNodes.iterator().next())) // but it gets run more than once
				{
					mayStartMultipleThreadObjects = true; // then "thread" in "thread.start()" could be more than one object
				}
			}
			
			if(mayStartMultipleThreadObjects)
			{
				//MHPLists.add(thread); // add another copy
				thread.setMultiInstance();
				threadId=threadId+2;
			}
			else
			{
				threadId++;
			}
		}
		
		// do same for main method
		XAbstractRuntimeThread mainThread = new XAbstractRuntimeThread();
		MHPLists.add(mainThread);
		mainThread.setThreadId(0);
		SootMethod mainMethod = Scene.v().getMainClass().getMethodByName("main");
		//mainThread.addMethod(mainMethod);
		mainThread.addRunMethod(mainMethod);
		
	}

	public List<XAbstractRuntimeThread> getThreads() {
		
		if(MHPLists == null)
			return null;

		List<XAbstractRuntimeThread> threads = new ArrayList<XAbstractRuntimeThread>();
		int size = MHPLists.size();
		for(int i = 0; i < size; i++)
		{
			if( !threads.contains(MHPLists.get(i)) )
			{
				threads.add(MHPLists.get(i));//I DON'T UNDERSTAND this!
			}//you compute it but not use it
		}
		return threads;
	}

}
